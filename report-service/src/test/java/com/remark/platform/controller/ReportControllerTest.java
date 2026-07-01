package com.remark.platform.controller;

import com.remark.platform.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportControllerTest {

    private final ReportService reportService = mock(ReportService.class);
    private final ReportController controller = new ReportController(reportService);

    @Test
    void findAllDelegatesToService() {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        controller.findAll(userId, "APPLICANT");

        verify(reportService).findAll(userId, "APPLICANT");
    }

    @Test
    void downloadDelegatesToService() {
        UUID reportId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        ResponseEntity<byte[]> expected = ResponseEntity.ok("pdf".getBytes());
        when(reportService.download(reportId, userId, "BRANCH_HEAD")).thenReturn(expected);

        var response = controller.download(reportId, userId, "BRANCH_HEAD");

        assertThat(response).isSameAs(expected);
    }
}
