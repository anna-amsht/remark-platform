package com.remark.platform.service;

import com.remark.platform.client.NotificationClient;
import com.remark.platform.client.RemarkServiceClient;
import com.remark.platform.client.UserServiceClient;
import com.remark.platform.dto.GenerateReportRequest;
import com.remark.platform.dto.InternalRemarkResponse;
import com.remark.platform.dto.ReportResponse;
import com.remark.platform.entity.Report;
import com.remark.platform.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final RemarkServiceClient remarkServiceClient;
    private final RemarkReportPdfGenerator remarkReportPdfGenerator;
    private final NotificationClient notificationClient;
    private final UserServiceClient userServiceClient;

    @Transactional
    public ReportResponse generate(GenerateReportRequest request) {
        InternalRemarkResponse remark = remarkServiceClient.getRemark(request.remarkId());
        if (!"RESOLVED".equals(remark.status()) && !"CLOSED".equals(remark.status())) {
            throw new IllegalStateException("Report can be generated only for RESOLVED or CLOSED remarks");
        }

        byte[] pdfContent = remarkReportPdfGenerator.generate(remark);
        String fileName = remark.remarkNumber() + "-report.pdf";

        Report report = new Report();
        report.setRemarkId(remark.id());
        report.setRemarkNumber(remark.remarkNumber());
        report.setFileName(fileName);
        report.setContent(pdfContent);
        Report saved = reportRepository.save(report);

        String body = """
                Сформирован PDF-отчёт по замечанию %s.
                Объект: %s
                Статус: %s
                """.formatted(
                remark.remarkNumber(),
                remark.objectBuilding(),
                remark.status()
        );

        UserServiceClient.UserProfileResponse creator = userServiceClient.getActiveUser(remark.createdByUserId());

        notificationClient.sendReportNotification(
                creator.email(),
                remark.remarkNumber(),
                body
        );

        remarkServiceClient.markReportSent(remark.id());

        return new ReportResponse(saved.getId(), saved.getRemarkId(), saved.getRemarkNumber(), saved.getFileName());
    }
}
