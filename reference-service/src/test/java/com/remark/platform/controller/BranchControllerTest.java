package com.remark.platform.controller;

import com.remark.platform.dto.BranchResponse;
import com.remark.platform.exceptions.BranchNotFoundException;
import com.remark.platform.service.BranchService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BranchControllerTest {

    private final BranchService branchService = mock(BranchService.class);
    private final BranchController controller = new BranchController(branchService);

    @Test
    void listBranchesDelegatesToService() {
        BranchResponse branch = branch();
        when(branchService.findActiveBranches()).thenReturn(List.of(branch));

        var response = controller.listBranches();

        assertThat(response).containsExactly(branch);
        verify(branchService).findActiveBranches();
    }

    @Test
    void getBranchDelegatesToService() {
        UUID branchId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        BranchResponse branch = branch();
        when(branchService.findById(branchId)).thenReturn(branch);

        var response = controller.getBranch(branchId);

        assertThat(response).isSameAs(branch);
        verify(branchService).findById(branchId);
    }

    @Test
    void handleNotFoundReturnsProblemDetail() {
        UUID branchId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        var response = controller.handleNotFound(new BranchNotFoundException(branchId));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getDetail()).contains(branchId.toString());
    }

    private static BranchResponse branch() {
        return new BranchResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "BR-1",
                "Minsk branch",
                true
        );
    }
}
