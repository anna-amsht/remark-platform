package com.remark.platform.kafka;

import com.remark.platform.events.KafkaTopics;
import com.remark.platform.events.UserApprovedMessage;
import com.remark.platform.service.UserApprovedApplicationEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UserApprovedKafkaPublisherTest {

    @Test
    void publishSendsUserApprovedMessage() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        UserApprovedKafkaPublisher publisher = new UserApprovedKafkaPublisher(kafkaTemplate);
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        ArgumentCaptor<UserApprovedMessage> messageCaptor = ArgumentCaptor.forClass(UserApprovedMessage.class);

        publisher.publish(new UserApprovedApplicationEvent(userId, "user@example.com", "User"));

        verify(kafkaTemplate).send(eq(KafkaTopics.USER_APPROVED), eq(userId.toString()), messageCaptor.capture());
        assertThat(messageCaptor.getValue().userId()).isEqualTo(userId);
        assertThat(messageCaptor.getValue().email()).isEqualTo("user@example.com");
        assertThat(messageCaptor.getValue().fullName()).isEqualTo("User");
        assertThat(messageCaptor.getValue().eventId()).isNotNull();
        assertThat(messageCaptor.getValue().timestamp()).isNotNull();
    }
}
