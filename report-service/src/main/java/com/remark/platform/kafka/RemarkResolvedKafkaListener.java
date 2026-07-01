package com.remark.platform.kafka;

import com.remark.platform.dto.GenerateReportRequest;
import com.remark.platform.events.KafkaTopics;
import com.remark.platform.events.RemarkResolvedMessage;
import com.remark.platform.service.ProcessedEventService;
import com.remark.platform.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RemarkResolvedKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(RemarkResolvedKafkaListener.class);

    private final ProcessedEventService processedEventService;
    private final ReportService reportService;

    public RemarkResolvedKafkaListener(ProcessedEventService processedEventService, ReportService reportService) {
        this.processedEventService = processedEventService;
        this.reportService = reportService;
    }

    @KafkaListener(
            topics = KafkaTopics.REMARK_RESOLVED,
            groupId = "report-service",
            containerFactory = "remarkResolvedKafkaListenerContainerFactory"
    )
    public void onRemarkResolved(RemarkResolvedMessage message) {
        if (processedEventService.isProcessed(message.eventId())) {
            return;
        }

        reportService.generate(new GenerateReportRequest(message.remarkId()));
        processedEventService.markProcessed(message.eventId(), KafkaTopics.REMARK_RESOLVED);
        log.info("Generated report for remark {}", message.remarkId());
    }
}
