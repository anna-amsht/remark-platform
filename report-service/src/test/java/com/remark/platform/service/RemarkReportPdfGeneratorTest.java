package com.remark.platform.service;

import com.remark.platform.dto.ReportPdfContext;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RemarkReportPdfGeneratorTest {

    private final RemarkReportPdfGenerator generator = new RemarkReportPdfGenerator();

    @Test
    void generateCreatesPdfDocument() {
        ReportPdfContext context = new ReportPdfContext(
                "REM-2026-00001",
                "Applicant Name",
                "Minsk branch",
                "Building A",
                "Defect description",
                LocalDate.of(2026, 1, 1),
                "Master Name (Mechanic)",
                Instant.parse("2026-01-02T10:00:00Z"),
                "Resolved successfully"
        );

        byte[] pdf = generator.generate(context);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void generateCreatesPdfWhenOptionalFieldsAreMissing() {
        ReportPdfContext context = new ReportPdfContext(
                "REM-2026-00002",
                "Applicant Name",
                "Minsk branch",
                "Building B",
                "Defect description",
                null,
                "Not assigned",
                null,
                " "
        );

        byte[] pdf = generator.generate(context);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }
}
