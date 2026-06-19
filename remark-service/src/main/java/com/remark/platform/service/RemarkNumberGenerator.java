package com.remark.platform.service;

import com.remark.platform.entity.RemarkSequence;
import com.remark.platform.repository.RemarkSequenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Service
public class RemarkNumberGenerator {

    private final RemarkSequenceRepository remarkSequenceRepository;

    public RemarkNumberGenerator(RemarkSequenceRepository remarkSequenceRepository) {
        this.remarkSequenceRepository = remarkSequenceRepository;
    }

    @Transactional
    public String nextRemarkNumber() {
        int year = Year.now().getValue();
        RemarkSequence sequence = remarkSequenceRepository.findByYearForUpdate(year)
                .orElseGet(() -> {
                    RemarkSequence created = new RemarkSequence();
                    created.setYear(year);
                    created.setLastValue(0L);
                    return remarkSequenceRepository.save(created);
                });

        long nextValue = sequence.getLastValue() + 1;
        sequence.setLastValue(nextValue);
        remarkSequenceRepository.save(sequence);

        return "REM-%d-%05d".formatted(year, nextValue);
    }
}
