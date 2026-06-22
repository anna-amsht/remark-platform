package com.remark.platform.controller;

import com.remark.platform.dto.InternalRemarkResponse;
import com.remark.platform.service.RemarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/remarks")
@RequiredArgsConstructor
public class InternalRemarkController {

    private final RemarkService remarkService;

    @GetMapping("/{id}")
    public InternalRemarkResponse findById(@PathVariable UUID id) {
        return remarkService.findByIdInternal(id);
    }

    @PatchMapping("/{id}/report-sent")
    public InternalRemarkResponse markReportSent(@PathVariable UUID id) {
        return remarkService.markReportSent(id);
    }
}
