package com.remark.platform.kafka;

import com.remark.platform.events.KafkaTopics;
import com.remark.platform.events.RemarkResolvedMessage;
import com.remark.platform.service.RemarkResolvedApplicationEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RemarkResolvedKafkaPublisherTest {

    @Test
    void publishSendsRemarkResolvedMessage() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        RemarkResolvedKafkaPublisher publisher = new RemarkResolvedKafkaPublisher(kafkaTemplate);
        UUID remarkId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        RemarkResolvedApplicationEvent event = new RemarkResolvedApplicationEvent(
                remarkId,
                "REM-2026-00001",
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "creator@example.com",
                "Building",
                "Fixed",
                UUID.fromString("44444444-4444-4444-4444-444444444444")
        );
        ArgumentCaptor<RemarkResolvedMessage> messageCaptor = ArgumentCaptor.forClass(RemarkResolvedMessage.class);

        publisher.publish(event);

        verify(kafkaTemplate).send(
                org.mockito.ArgumentMatchers.eq(KafkaTopics.REMARK_RESOLVED),
                org.mockito.ArgumentMatchers.eq(remarkId.toString()),
                messageCaptor.capture()
        );
        assertThat(messageCaptor.getValue().remarkId()).isEqualTo(remarkId);
        assertThat(messageCaptor.getValue().creatorEmail()).isEqualTo("creator@example.com");
        assertThat(messageCaptor.getValue().eventId()).isNotNull();
        assertThat(messageCaptor.getValue().timestamp()).isNotNull();
    }
}
