package com.remark.platform.service;

import com.remark.platform.entity.ProcessedEvent;
import com.remark.platform.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessedEventServiceTest {

    private final ProcessedEventRepository processedEventRepository = mock(ProcessedEventRepository.class);
    private final ProcessedEventService processedEventService = new ProcessedEventService(processedEventRepository);

    @Test
    void isProcessedDelegatesToRepository() {
        UUID eventId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        assertThat(processedEventService.isProcessed(eventId)).isTrue();
    }

    @Test
    void markProcessedStoresProcessedEvent() {
        UUID eventId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        ArgumentCaptor<ProcessedEvent> captor = ArgumentCaptor.forClass(ProcessedEvent.class);

        processedEventService.markProcessed(eventId, "ReportSent");

        verify(processedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo(eventId);
        assertThat(captor.getValue().getEventType()).isEqualTo("ReportSent");
        assertThat(captor.getValue().getProcessedAt()).isNotNull();
    }
}
