package com.remark.platform.service;

import com.remark.platform.config.MailProperties;
import com.remark.platform.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    public void send(Notification notification) {
        if (!mailProperties.enabled()) {
            log.info(
                    "Mail disabled — notification logged: type={}, to={}, subject={}",
                    notification.getType(),
                    notification.getRecipientEmail(),
                    notification.getSubject()
            );
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(notification.getRecipientEmail());
        message.setSubject(notification.getSubject());
        message.setText(notification.getBody());
        mailSender.send(message);
    }
}
