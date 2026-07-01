package com.remark.platform.kafka;

import com.remark.platform.events.KafkaTopics;
import com.remark.platform.events.UserApprovedMessage;
import com.remark.platform.service.UserApprovedApplicationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.UUID;

@Component
public class UserApprovedKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserApprovedKafkaPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public UserApprovedKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(UserApprovedApplicationEvent event) {
        UserApprovedMessage message = new UserApprovedMessage(
                UUID.randomUUID(),
                Instant.now(),
                event.userId(),
                event.email(),
                event.fullName()
        );
        kafkaTemplate.send(KafkaTopics.USER_APPROVED, event.userId().toString(), message);
        log.info("Published {} for user {}", KafkaTopics.USER_APPROVED, event.userId());
    }
}
