package com.remark.platform.kafka;

import com.remark.platform.dto.GenerateReportRequest;
import com.remark.platform.events.KafkaTopics;
import com.remark.platform.events.RemarkResolvedMessage;
import com.remark.platform.service.ProcessedEventService;
import com.remark.platform.service.ReportService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RemarkResolvedKafkaListenerTest {

    private final ProcessedEventService processedEventService = mock(ProcessedEventService.class);
    private final ReportService reportService = mock(ReportService.class);
    private final RemarkResolvedKafkaListener listener =
            new RemarkResolvedKafkaListener(processedEventService, reportService);

    @Test
    void onRemarkResolvedGeneratesReportAndMarksEventProcessed() {
        RemarkResolvedMessage message = message();
        when(processedEventService.isProcessed(message.eventId())).thenReturn(false);

        listener.onRemarkResolved(message);

        verify(reportService).generate(new GenerateReportRequest(message.remarkId()));
        verify(processedEventService).markProcessed(message.eventId(), KafkaTopics.REMARK_RESOLVED);
    }

    @Test
    void onRemarkResolvedSkipsAlreadyProcessedEvent() {
        RemarkResolvedMessage message = message();
        when(processedEventService.isProcessed(message.eventId())).thenReturn(true);

        listener.onRemarkResolved(message);

        verify(reportService, never()).generate(any());
        verify(processedEventService, never()).markProcessed(any(), any());
    }

    private static RemarkResolvedMessage message() {
        return new RemarkResolvedMessage(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                Instant.parse("2026-01-01T10:00:00Z"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "REM-2026-00001",
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                "creator@example.com",
                "Building",
                "Fixed",
                UUID.fromString("55555555-5555-5555-5555-555555555555")
        );
    }
}
