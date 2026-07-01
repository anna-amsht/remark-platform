package com.remark.platform.kafka;

import com.remark.platform.events.KafkaTopics;
import com.remark.platform.events.ReportSentMessage;
import com.remark.platform.service.ProcessedEventService;
import com.remark.platform.service.RemarkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReportSentKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(ReportSentKafkaListener.class);

    private final ProcessedEventService processedEventService;
    private final RemarkService remarkService;

    public ReportSentKafkaListener(ProcessedEventService processedEventService, RemarkService remarkService) {
        this.processedEventService = processedEventService;
        this.remarkService = remarkService;
    }

    @KafkaListener(
            topics = KafkaTopics.REPORT_SENT,
            groupId = "remark-service",
            containerFactory = "reportSentKafkaListenerContainerFactory"
    )
    public void onReportSent(ReportSentMessage message) {
        if (processedEventService.isProcessed(message.eventId())) {
            log.debug("Skip duplicate event {}", message.eventId());
            return;
        }

        remarkService.markReportSent(message.remarkId());
        processedEventService.markProcessed(message.eventId(), KafkaTopics.REPORT_SENT);
        log.info("Marked report_sent_at for remark {}", message.remarkId());
    }
}
