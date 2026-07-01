package com.remark.platform.service;

import com.remark.platform.client.NotificationClient;
import com.remark.platform.kafka.ReportSentKafkaPublisher;
import com.remark.platform.client.ReferenceServiceClient;
import com.remark.platform.client.RemarkServiceClient;
import com.remark.platform.client.UserServiceClient;
import com.remark.platform.dto.ReportNotificationRecipient;
import com.remark.platform.dto.ReportNotificationRecipient.RecipientRole;
import com.remark.platform.dto.GenerateReportRequest;
import com.remark.platform.dto.InternalRemarkResponse;
import com.remark.platform.dto.ReportPdfContext;
import com.remark.platform.dto.ReportResponse;
import com.remark.platform.entity.Report;
import com.remark.platform.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final RemarkServiceClient remarkServiceClient;
    private final RemarkReportPdfGenerator remarkReportPdfGenerator;
    private final NotificationClient notificationClient;
    private final UserServiceClient userServiceClient;
    private final ReferenceServiceClient referenceServiceClient;
    private final ReportSentKafkaPublisher reportSentKafkaPublisher;

    @Transactional
    public ReportResponse generate(GenerateReportRequest request) {
        InternalRemarkResponse remark = remarkServiceClient.getRemark(request.remarkId());
        if (!"RESOLVED".equals(remark.status())) {
            throw new IllegalStateException("Report can be generated only for RESOLVED remarks");
        }

        ReportPdfContext pdfContext = buildReportContext(remark);
        byte[] pdfContent = remarkReportPdfGenerator.generate(pdfContext);
        String fileName = remark.remarkNumber() + "-report.pdf";

        Report report = new Report();
        report.setRemarkId(remark.id());
        report.setRemarkNumber(remark.remarkNumber());
        report.setFileName(fileName);
        report.setContent(pdfContent);
        Report saved = reportRepository.save(report);

        for (ReportNotificationRecipient recipient : collectReportRecipients(remark)) {
            notificationClient.sendReportNotification(
                    recipient.userId(),
                    remark.id(),
                    saved.getId(),
                    remark.remarkNumber(),
                    buildNotificationBody(recipient.role(), remark, pdfContext)
            );
        }

        reportSentKafkaPublisher.publish(remark.id(), remark.remarkNumber());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> findAll(UUID userId, String roles) {
        assertCanViewReports(roles);
        return reportRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(report -> canViewReport(report, userId, roles))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> download(UUID id, UUID userId, String roles) {
        assertCanViewReports(roles);
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + id));
        if (!canViewReport(report, userId, roles)) {
            throw new IllegalStateException("User cannot view report");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + report.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(report.getContent());
    }

    private ReportPdfContext buildReportContext(InternalRemarkResponse remark) {
        UserServiceClient.UserProfileResponse applicant =
                userServiceClient.getActiveUser(remark.createdByUserId());
        ReferenceServiceClient.BranchResponse branch =
                referenceServiceClient.getBranch(remark.branchId());

        String responsibleOrganization = "Не назначена";
        if (remark.assignedMasterId() != null) {
            UserServiceClient.UserProfileResponse master =
                    userServiceClient.getActiveUser(remark.assignedMasterId());
            responsibleOrganization = formatResponsibleOrganization(master);
        }

        return new ReportPdfContext(
                remark.remarkNumber(),
                applicant.fullName(),
                branch.name(),
                remark.objectBuilding(),
                remark.description(),
                remark.discoveredAt(),
                responsibleOrganization,
                remark.resolvedAt(),
                remark.resolutionMessage()
        );
    }

    private String formatResponsibleOrganization(UserServiceClient.UserProfileResponse master) {
        if (master.position() != null && !master.position().isBlank()) {
            return master.fullName() + " (" + master.position().trim() + ")";
        }
        return master.fullName();
    }

    private List<ReportNotificationRecipient> collectReportRecipients(InternalRemarkResponse remark) {
        List<ReportNotificationRecipient> recipients = new ArrayList<>();
        recipients.add(new ReportNotificationRecipient(remark.createdByUserId(), RecipientRole.APPLICANT));

        if (remark.assignedByUserId() != null
                && !remark.assignedByUserId().equals(remark.createdByUserId())) {
            recipients.add(new ReportNotificationRecipient(remark.assignedByUserId(), RecipientRole.ASSIGNER));
        }
        return recipients;
    }

    private String buildNotificationBody(
            RecipientRole role,
            InternalRemarkResponse remark,
            ReportPdfContext pdfContext
    ) {
        if (role == RecipientRole.ASSIGNER) {
            return """
                    Сформирован отчёт об устранении замечания № %s, которое вы направили мастеру.
                    Заявитель: %s
                    Объект: %s

                    PDF доступен в разделе «Уведомления» и в списке отчётов.
                    """.formatted(
                    remark.remarkNumber(),
                    pdfContext.applicantFullName(),
                    remark.objectBuilding()
            );
        }

        return """
                По вашему заявлению № %s сформирован отчёт об устранении производственного дефекта.
                Объект: %s

                PDF доступен в разделе «Уведомления» и в списке отчётов.
                """.formatted(
                remark.remarkNumber(),
                remark.objectBuilding()
        );
    }

    private boolean canViewReport(Report report, UUID userId, String roles) {
        InternalRemarkResponse remark = remarkServiceClient.getRemark(report.getRemarkId());
        if (hasRole(roles, "ADMIN") || hasRole(roles, "BRANCH_HEAD")) {
            UUID branchId = requireActorBranchId(userId);
            return branchId.equals(remark.branchId());
        }
        if (hasRole(roles, "APPLICANT")) {
            return remark.createdByUserId().equals(userId);
        }
        return false;
    }

    private UUID requireActorBranchId(UUID userId) {
        UserServiceClient.UserProfileResponse actor = userServiceClient.getActiveUser(userId);
        if (actor.branchId() == null) {
            throw new IllegalStateException("User branch is required");
        }
        return actor.branchId();
    }

    private ReportResponse toResponse(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getRemarkId(),
                report.getRemarkNumber(),
                report.getFileName(),
                report.getCreatedAt()
        );
    }

    private void assertCanViewReports(String roles) {
        if (roles == null || roles.isBlank()) {
            throw new IllegalStateException("User roles are required");
        }
        if (!hasRole(roles, "ADMIN") && !hasRole(roles, "BRANCH_HEAD") && !hasRole(roles, "APPLICANT")) {
            throw new IllegalStateException("User cannot view reports");
        }
    }

    private boolean hasRole(String roles, String expectedRole) {
        return List.of(roles.split(",")).stream()
                .map(String::trim)
                .anyMatch(role -> role.equals(expectedRole));
    }
}
