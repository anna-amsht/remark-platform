package com.remark.platform.service;

import com.remark.platform.entity.RemarkSequence;
import com.remark.platform.repository.RemarkSequenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Year;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemarkNumberGeneratorTest {

    @Mock
    private RemarkSequenceRepository remarkSequenceRepository;

    @InjectMocks
    private RemarkNumberGenerator remarkNumberGenerator;

    @Test
    void nextRemarkNumber_incrementsExistingSequence() {
        int year = Year.now().getValue();
        RemarkSequence sequence = new RemarkSequence();
        sequence.setYear(year);
        sequence.setLastValue(41L);

        when(remarkSequenceRepository.findByYearForUpdate(year)).thenReturn(Optional.of(sequence));
        when(remarkSequenceRepository.save(sequence)).thenReturn(sequence);

        String remarkNumber = remarkNumberGenerator.nextRemarkNumber();

        assertThat(remarkNumber).isEqualTo("REM-%d-%05d".formatted(year, 42));
        assertThat(sequence.getLastValue()).isEqualTo(42L);
        verify(remarkSequenceRepository).save(sequence);
    }

    @Test
    void nextRemarkNumber_createsSequenceForNewYear() {
        int year = Year.now().getValue();

        when(remarkSequenceRepository.findByYearForUpdate(year)).thenReturn(Optional.empty());
        when(remarkSequenceRepository.save(any(RemarkSequence.class))).thenAnswer(invocation -> {
            RemarkSequence created = invocation.getArgument(0);
            if (created.getLastValue() == 0L) {
                return created;
            }
            return created;
        });

        String remarkNumber = remarkNumberGenerator.nextRemarkNumber();

        assertThat(remarkNumber).isEqualTo("REM-%d-%05d".formatted(year, 1));
        verify(remarkSequenceRepository, times(2)).save(any(RemarkSequence.class));
    }
}
