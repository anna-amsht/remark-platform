package com.remark.platform.service;

import com.remark.platform.entity.Branch;
import com.remark.platform.exceptions.BranchNotFoundException;
import com.remark.platform.repository.BranchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @InjectMocks
    private BranchService branchService;

    @Test
    void findActiveBranchesReturnsMappedResponses() {
        Branch branch = new Branch();
        branch.setId(UUID.randomUUID());
        branch.setCode("1212");
        branch.setName("Test branch");
        branch.setActive(true);

        when(branchRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(branch));

        var result = branchService.findActiveBranches();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().code()).isEqualTo("1212");
    }

    @Test
    void findByIdThrowsWhenBranchMissing() {
        UUID id = UUID.randomUUID();
        when(branchRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> branchService.findById(id))
                .isInstanceOf(BranchNotFoundException.class);
    }
}
