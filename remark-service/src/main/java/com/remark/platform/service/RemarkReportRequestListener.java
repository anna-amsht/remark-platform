package com.remark.platform.service;

import com.remark.platform.client.ReportClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RemarkReportRequestListener {

    private final ReportClient reportClient;

    public RemarkReportRequestListener(ReportClient reportClient) {
        this.reportClient = reportClient;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void requestReport(RemarkResolvedEvent event) {
        reportClient.requestReport(event.remarkId());
    }
}
