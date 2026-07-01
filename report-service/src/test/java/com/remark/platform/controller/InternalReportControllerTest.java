package com.remark.platform.controller;

import com.remark.platform.dto.GenerateReportRequest;
import com.remark.platform.dto.ReportResponse;
import com.remark.platform.service.ReportService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InternalReportControllerTest {

    private final ReportService reportService = mock(ReportService.class);
    private final InternalReportController controller = new InternalReportController(reportService);

    @Test
    void generateDelegatesToService() {
        UUID remarkId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        GenerateReportRequest request = new GenerateReportRequest(remarkId);
        ReportResponse expected = new ReportResponse(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                remarkId,
                "REM-2026-00001",
                "report.pdf",
                Instant.parse("2026-01-01T10:00:00Z")
        );
        when(reportService.generate(request)).thenReturn(expected);

        ReportResponse actual = controller.generate(request);

        assertThat(actual).isSameAs(expected);
    }
}
