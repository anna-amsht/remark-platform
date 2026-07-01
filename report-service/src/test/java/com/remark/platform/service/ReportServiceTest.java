package com.remark.platform.service;

import com.remark.platform.client.NotificationClient;
import com.remark.platform.client.ReferenceServiceClient;
import com.remark.platform.client.RemarkServiceClient;
import com.remark.platform.client.UserServiceClient;
import com.remark.platform.dto.GenerateReportRequest;
import com.remark.platform.dto.InternalRemarkResponse;
import com.remark.platform.dto.ReportPdfContext;
import com.remark.platform.kafka.ReportSentKafkaPublisher;
import com.remark.platform.entity.Report;
import com.remark.platform.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private RemarkServiceClient remarkServiceClient;

    @Mock
    private RemarkReportPdfGenerator remarkReportPdfGenerator;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private ReferenceServiceClient referenceServiceClient;

    @Mock
    private ReportSentKafkaPublisher reportSentKafkaPublisher;

    @InjectMocks
    private ReportService reportService;

    private UUID reportId;
    private UUID remarkId;
    private UUID branchId;
    private UUID applicantId;
    private UUID masterId;
    private UUID assignerId;

    @BeforeEach
    void setUp() {
        reportId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        remarkId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        branchId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        applicantId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        masterId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        assignerId = UUID.fromString("66666666-6666-6666-6666-666666666666");
    }

    @Test
    void generateCreatesReportAndNotifiesApplicantAndAssigner() {
        InternalRemarkResponse remark = resolvedRemark();
        byte[] pdf = "pdf-content".getBytes();
        when(remarkServiceClient.getRemark(remarkId)).thenReturn(remark);
        when(userServiceClient.getActiveUser(applicantId)).thenReturn(user(applicantId, "Applicant", "Engineer", branchId));
        when(userServiceClient.getActiveUser(masterId)).thenReturn(user(masterId, "Master", "Mechanic", branchId));
        when(referenceServiceClient.getBranch(branchId)).thenReturn(new ReferenceServiceClient.BranchResponse(
                branchId,
                "BR-1",
                "Minsk branch",
                true
        ));
        when(remarkReportPdfGenerator.generate(any(ReportPdfContext.class))).thenReturn(pdf);
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            report.setId(reportId);
            report.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));
            return report;
        });

        var response = reportService.generate(new GenerateReportRequest(remarkId));

        assertThat(response.id()).isEqualTo(reportId);
        assertThat(response.fileName()).isEqualTo("REM-2026-00001-report.pdf");

        ArgumentCaptor<ReportPdfContext> contextCaptor = ArgumentCaptor.forClass(ReportPdfContext.class);
        verify(remarkReportPdfGenerator).generate(contextCaptor.capture());
        assertThat(contextCaptor.getValue().applicantFullName()).isEqualTo("Applicant");
        assertThat(contextCaptor.getValue().branchName()).isEqualTo("Minsk branch");
        assertThat(contextCaptor.getValue().responsibleOrganization()).isEqualTo("Master (Mechanic)");

        verify(notificationClient).sendReportNotification(eq(applicantId), eq(remarkId), eq(reportId), eq("REM-2026-00001"), contains("REM-2026-00001"));
        verify(notificationClient).sendReportNotification(eq(assignerId), eq(remarkId), eq(reportId), eq("REM-2026-00001"), contains("Applicant"));
        verify(reportSentKafkaPublisher).publish(remarkId, "REM-2026-00001");
    }

    @Test
    void generate_rejectsRemarkThatIsNotResolved() {
        when(remarkServiceClient.getRemark(remarkId)).thenReturn(
                new InternalRemarkResponse(
                        remarkId,
                        "RM-1",
                        UUID.randomUUID(),
                        "Building",
                        "Description",
                        LocalDate.now(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "IN_PROGRESS",
                        null,
                        null,
                        null
                )
        );

        assertThatThrownBy(() -> reportService.generate(new GenerateReportRequest(remarkId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RESOLVED");
    }

    @Test
    void findAllAllowsApplicantToSeeOwnReportsOnly() {
        Report ownReport = report(remarkId);
        Report otherReport = report(UUID.fromString("77777777-7777-7777-7777-777777777777"));
        when(reportRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(ownReport, otherReport));
        when(remarkServiceClient.getRemark(remarkId)).thenReturn(resolvedRemark());
        when(remarkServiceClient.getRemark(otherReport.getRemarkId())).thenReturn(new InternalRemarkResponse(
                otherReport.getRemarkId(),
                "REM-2026-00002",
                branchId,
                "Other building",
                "Other description",
                LocalDate.now(),
                UUID.randomUUID(),
                masterId,
                assignerId,
                "RESOLVED",
                "Done",
                Instant.now(),
                null
        ));

        var reports = reportService.findAll(applicantId, "APPLICANT");

        assertThat(reports).hasSize(1);
        assertThat(reports.getFirst().remarkId()).isEqualTo(remarkId);
    }

    @Test
    void downloadAllowsBranchHeadFromSameBranch() {
        Report report = report(remarkId);
        UUID branchHeadId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(remarkServiceClient.getRemark(remarkId)).thenReturn(resolvedRemark());
        when(userServiceClient.getActiveUser(branchHeadId)).thenReturn(user(branchHeadId, "Head", "Manager", branchId));

        var response = reportService.download(reportId, branchHeadId, "BRANCH_HEAD");

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("REM-2026-00001-report.pdf");
        assertThat(response.getBody()).isEqualTo("pdf-content".getBytes());
    }

    @Test
    void downloadRejectsApplicantForAnotherUserReport() {
        Report report = report(remarkId);
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(remarkServiceClient.getRemark(remarkId)).thenReturn(resolvedRemark());

        assertThatThrownBy(() -> reportService.download(reportId, UUID.randomUUID(), "APPLICANT"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot view");
    }

    @Test
    void findAllRejectsMissingRoles() {
        assertThatThrownBy(() -> reportService.findAll(applicantId, " "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("roles");
    }

    private InternalRemarkResponse resolvedRemark() {
        return new InternalRemarkResponse(
                remarkId,
                "REM-2026-00001",
                branchId,
                "Building",
                "Description",
                LocalDate.of(2026, 1, 1),
                applicantId,
                masterId,
                assignerId,
                "RESOLVED",
                "Fixed",
                Instant.parse("2026-01-02T10:00:00Z"),
                null
        );
    }

    private Report report(UUID remarkId) {
        Report report = new Report();
        report.setId(reportId);
        report.setRemarkId(remarkId);
        report.setRemarkNumber("REM-2026-00001");
        report.setFileName("REM-2026-00001-report.pdf");
        report.setContent("pdf-content".getBytes());
        report.setCreatedAt(Instant.parse("2026-01-03T10:00:00Z"));
        return report;
    }

    private static UserServiceClient.UserProfileResponse user(
            UUID id,
            String fullName,
            String position,
            UUID branchId
    ) {
        return new UserServiceClient.UserProfileResponse(
                id,
                id + "@example.com",
                fullName,
                position,
                branchId,
                "ACTIVE",
                Set.of("APPLICANT")
        );
    }
}
