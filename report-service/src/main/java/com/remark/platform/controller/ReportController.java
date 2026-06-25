package com.remark.platform.controller;

import com.remark.platform.dto.ReportResponse;
import com.remark.platform.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLES_HEADER = "X-User-Roles";

    private final ReportService reportService;

    @GetMapping
    public List<ReportResponse> findAll(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestHeader(USER_ROLES_HEADER) String roles
    ) {
        return reportService.findAll(userId, roles);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(
            @PathVariable UUID id,
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestHeader(USER_ROLES_HEADER) String roles
    ) {
        return reportService.download(id, userId, roles);
    }
}
