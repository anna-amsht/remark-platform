package com.remark.platform.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.remark.platform.dto.InternalRemarkResponse;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Component
public class RemarkReportPdfGenerator {

    public byte[] generate(InternalRemarkResponse remark) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

            document.add(new Paragraph("Remark Report", titleFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Number: " + remark.remarkNumber(), bodyFont));
            document.add(new Paragraph("Branch ID: " + remark.branchId(), bodyFont));
            document.add(new Paragraph("Object: " + remark.objectBuilding(), bodyFont));
            document.add(new Paragraph("Description: " + remark.description(), bodyFont));
            document.add(new Paragraph("Discovered at: " + remark.discoveredAt(), bodyFont));
            document.add(new Paragraph("Status: " + remark.status(), bodyFont));
            if (remark.resolutionMessage() != null) {
                document.add(new Paragraph("Resolution: " + remark.resolutionMessage(), bodyFont));
            }
            if (remark.resolvedAt() != null) {
                document.add(new Paragraph("Resolved at: " + remark.resolvedAt(), bodyFont));
            }

            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Failed to generate PDF report", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate PDF report", ex);
        }
    }
}
