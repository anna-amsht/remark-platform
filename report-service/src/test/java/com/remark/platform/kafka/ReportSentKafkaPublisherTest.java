package com.remark.platform.kafka;

import com.remark.platform.events.KafkaTopics;
import com.remark.platform.events.ReportSentMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReportSentKafkaPublisherTest {

    @Test
    void publishSendsReportSentMessage() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        ReportSentKafkaPublisher publisher = new ReportSentKafkaPublisher(kafkaTemplate);
        UUID remarkId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        ArgumentCaptor<ReportSentMessage> messageCaptor = ArgumentCaptor.forClass(ReportSentMessage.class);

        publisher.publish(remarkId, "REM-2026-00001");

        verify(kafkaTemplate).send(
                org.mockito.ArgumentMatchers.eq(KafkaTopics.REPORT_SENT),
                org.mockito.ArgumentMatchers.eq(remarkId.toString()),
                messageCaptor.capture()
        );
        assertThat(messageCaptor.getValue().remarkId()).isEqualTo(remarkId);
        assertThat(messageCaptor.getValue().remarkNumber()).isEqualTo("REM-2026-00001");
        assertThat(messageCaptor.getValue().eventId()).isNotNull();
        assertThat(messageCaptor.getValue().timestamp()).isNotNull();
    }
}
