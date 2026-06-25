package com.remark.platform.service;

import com.remark.platform.config.MailProperties;
import com.remark.platform.dto.SendNotificationRequest;
import com.remark.platform.entity.Notification;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
@RequiredArgsConstructor
public class EmailNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    public void send(Notification notification, SendNotificationRequest request) {
        if (!mailProperties.enabled()) {
            log.info(
                    "Mail disabled - notification logged: type={}, to={}, subject={}",
                    notification.getType(),
                    notification.getRecipientEmail(),
                    notification.getSubject()
            );
            return;
        }

        if (hasAttachment(request)) {
            sendWithAttachment(notification, request);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(notification.getRecipientEmail());
        message.setSubject(notification.getSubject());
        message.setText(notification.getBody());
        mailSender.send(message);
    }

    private void sendWithAttachment(Notification notification, SendNotificationRequest request) {
        try {
            byte[] content = Base64.getDecoder().decode(request.attachmentBase64());
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(mailProperties.from());
            helper.setTo(notification.getRecipientEmail());
            helper.setSubject(notification.getSubject());
            helper.setText(notification.getBody());
            helper.addAttachment(
                    request.attachmentFileName(),
                    new ByteArrayResource(content),
                    request.attachmentContentType()
            );
            mailSender.send(message);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to send email with attachment", ex);
        }
    }

    private boolean hasAttachment(SendNotificationRequest request) {
        return request.attachmentFileName() != null && !request.attachmentFileName().isBlank()
                && request.attachmentContentType() != null && !request.attachmentContentType().isBlank()
                && request.attachmentBase64() != null && !request.attachmentBase64().isBlank();
    }
}
