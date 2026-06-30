package com.remark.platform.controller;

import com.remark.platform.dto.NotificationResponse;
import com.remark.platform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    public static final String USER_ID_HEADER = "X-User-Id";

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> findForCurrentUser(@RequestHeader(USER_ID_HEADER) UUID userId) {
        return notificationService.findForUser(userId);
    }
}
