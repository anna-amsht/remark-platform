package com.remark.platform.kafka;

import com.remark.platform.events.KafkaTopics;
import com.remark.platform.events.ReportSentMessage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class ReportSentKafkaPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ReportSentKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(UUID remarkId, String remarkNumber) {
        ReportSentMessage message = new ReportSentMessage(
                UUID.randomUUID(),
                Instant.now(),
                remarkId,
                remarkNumber
        );
        kafkaTemplate.send(KafkaTopics.REPORT_SENT, remarkId.toString(), message);
    }
}
