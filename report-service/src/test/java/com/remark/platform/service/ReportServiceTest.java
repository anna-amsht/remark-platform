package com.remark.platform.service;

import com.remark.platform.client.NotificationClient;
import com.remark.platform.client.ReferenceServiceClient;
import com.remark.platform.client.RemarkServiceClient;
import com.remark.platform.client.UserServiceClient;
import com.remark.platform.dto.GenerateReportRequest;
import com.remark.platform.dto.InternalRemarkResponse;
import com.remark.platform.kafka.ReportSentKafkaPublisher;
import com.remark.platform.repository.ReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    void generate_rejectsRemarkThatIsNotResolved() {
        UUID remarkId = UUID.randomUUID();
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
}
