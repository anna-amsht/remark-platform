package com.remark.platform.controller;

import com.remark.platform.dto.NotificationResponse;
import com.remark.platform.dto.SendNotificationRequest;
import com.remark.platform.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/notifications")
@RequiredArgsConstructor
public class InternalNotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public NotificationResponse send(@Valid @RequestBody SendNotificationRequest request) {
        return notificationService.send(request);
    }
}
