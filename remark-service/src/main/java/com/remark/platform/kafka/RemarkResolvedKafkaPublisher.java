package com.remark.platform.kafka;

import com.remark.platform.events.KafkaTopics;
import com.remark.platform.events.RemarkResolvedMessage;
import com.remark.platform.service.RemarkResolvedApplicationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.UUID;

@Component
public class RemarkResolvedKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(RemarkResolvedKafkaPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RemarkResolvedKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(RemarkResolvedApplicationEvent event) {
        RemarkResolvedMessage message = new RemarkResolvedMessage(
                UUID.randomUUID(),
                Instant.now(),
                event.remarkId(),
                event.remarkNumber(),
                event.branchId(),
                event.createdByUserId(),
                event.creatorEmail(),
                event.objectBuilding(),
                event.resolutionMessage(),
                event.resolvedByUserId()
        );
        kafkaTemplate.send(KafkaTopics.REMARK_RESOLVED, event.remarkId().toString(), message);
        log.info("Published {} for remark {}", KafkaTopics.REMARK_RESOLVED, event.remarkId());
    }
}
