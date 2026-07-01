package com.remark.platform.controller;

import com.remark.platform.dto.GenerateReportRequest;
import com.remark.platform.dto.ReportResponse;
import com.remark.platform.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/reports")
@RequiredArgsConstructor
public class InternalReportController {

    private final ReportService reportService;

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse generate(@Valid @RequestBody GenerateReportRequest request) {
        return reportService.generate(request);
    }
}
