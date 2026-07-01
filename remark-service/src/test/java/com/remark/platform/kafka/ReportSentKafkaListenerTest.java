package com.remark.platform.kafka;

import com.remark.platform.events.KafkaTopics;
import com.remark.platform.events.ReportSentMessage;
import com.remark.platform.service.ProcessedEventService;
import com.remark.platform.service.RemarkService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportSentKafkaListenerTest {

    private final ProcessedEventService processedEventService = mock(ProcessedEventService.class);
    private final RemarkService remarkService = mock(RemarkService.class);
    private final ReportSentKafkaListener listener = new ReportSentKafkaListener(processedEventService, remarkService);

    @Test
    void onReportSentMarksRemarkReportSentAndEventProcessed() {
        ReportSentMessage message = message();
        when(processedEventService.isProcessed(message.eventId())).thenReturn(false);

        listener.onReportSent(message);

        verify(remarkService).markReportSent(message.remarkId());
        verify(processedEventService).markProcessed(message.eventId(), KafkaTopics.REPORT_SENT);
    }

    @Test
    void onReportSentSkipsAlreadyProcessedEvent() {
        ReportSentMessage message = message();
        when(processedEventService.isProcessed(message.eventId())).thenReturn(true);

        listener.onReportSent(message);

        verify(remarkService, never()).markReportSent(message.remarkId());
        verify(processedEventService, never()).markProcessed(message.eventId(), KafkaTopics.REPORT_SENT);
    }

    private static ReportSentMessage message() {
        return new ReportSentMessage(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                Instant.parse("2026-01-01T10:00:00Z"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "REM-2026-00001"
        );
    }
}
