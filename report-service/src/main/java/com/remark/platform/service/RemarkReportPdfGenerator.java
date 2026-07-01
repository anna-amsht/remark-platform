package com.remark.platform.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.remark.platform.dto.ReportPdfContext;
import org.springframework.stereotype.Component;

import java.awt.Color;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

@Component
public class RemarkReportPdfGenerator {

    private static final Color CORPORATE_BLUE = new Color(0, 102, 179);
    private static final Color CORPORATE_LIGHT = new Color(232, 242, 250);
    private static final Color TEXT_DARK = new Color(33, 37, 41);
    private static final Color BORDER_COLOR = new Color(198, 214, 228);
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy г.", Locale.forLanguageTag("ru"));
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy г., HH:mm", Locale.forLanguageTag("ru"));
    private static final ZoneId REPORT_ZONE = ZoneId.of("Europe/Minsk");

    public byte[] generate(ReportPdfContext context) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 48, 48, 48, 48);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            BaseFont regularBase = loadFont("/fonts/Arial.ttf");
            BaseFont boldBase = loadFont("/fonts/Arial-Bold.ttf");

            Font companyFont = new Font(boldBase, 11, Font.NORMAL, Color.WHITE);
            Font titleFont = new Font(boldBase, 16, Font.NORMAL, CORPORATE_BLUE);
            Font subtitleFont = new Font(regularBase, 11, Font.NORMAL, TEXT_DARK);
            Font bodyFont = new Font(regularBase, 12, Font.NORMAL, TEXT_DARK);
            Font labelFont = new Font(boldBase, 11, Font.NORMAL, CORPORATE_BLUE);
            Font valueFont = new Font(regularBase, 11, Font.NORMAL, TEXT_DARK);
            Font footerFont = new Font(regularBase, 9, Font.ITALIC, new Color(108, 117, 125));

            document.add(buildHeader(companyFont));
            document.add(new Paragraph(" ", bodyFont));
            document.add(new Paragraph("ОТЧЁТ ОБ УСТРАНЕНИИ ПРОИЗВОДСТВЕННОГО ЗАМЕЧАНИЯ", titleFont));
            document.add(new Paragraph("№ " + context.remarkNumber(), subtitleFont));
            document.add(new Paragraph(" ", bodyFont));

            document.add(new Paragraph(
                    "Уважаемый(ая) " + context.applicantFullName() + "!",
                    bodyFont
            ));
            document.add(new Paragraph(" ", bodyFont));
            document.add(new Paragraph(
                    """
                    Сообщаем Вам, что Ваше заявление об обнаружении дефектов на производстве \
                    было рассмотрено в установленном порядке. По результатам рассмотрения \
                    незамедлительно были приняты меры по их устранению.""",
                    bodyFont
            ));
            document.add(new Paragraph(" ", bodyFont));
            document.add(new Paragraph("Дополнительная информация:", labelFont));
            document.add(new Paragraph(" ", bodyFont));
            document.add(buildDetailsTable(context, labelFont, valueFont));
            document.add(new Paragraph(" ", bodyFont));

            if (context.resolutionMessage() != null && !context.resolutionMessage().isBlank()) {
                document.add(new Paragraph("Сведения о выполненных мерах:", labelFont));
                document.add(new Paragraph(context.resolutionMessage().trim(), bodyFont));
                document.add(new Paragraph(" ", bodyFont));
            }

            document.add(new Paragraph(" ", bodyFont));
            document.add(new Paragraph(
                    "Настоящий отчёт сформирован автоматически информационной системой «Журнал замечаний».",
                    footerFont
            ));
            document.add(new Paragraph(
                    "ОАО «Газпром трансгаз Беларусь»",
                    new Font(boldBase, 10, Font.NORMAL, CORPORATE_BLUE)
            ));

            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("Failed to generate PDF report", ex);
        }
    }

    private PdfPTable buildHeader(Font companyFont) throws DocumentException, IOException {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1.4f, 2.6f});

        PdfPCell logoCell = new PdfPCell(loadLogo(), false);
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setPadding(8);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setBackgroundColor(CORPORATE_LIGHT);

        PdfPCell titleCell = new PdfPCell(new Phrase("ОАО «Газпром трансгаз Беларусь»", companyFont));
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPadding(16);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        titleCell.setBackgroundColor(CORPORATE_BLUE);

        header.addCell(logoCell);
        header.addCell(titleCell);
        return header;
    }

    private Image loadLogo() throws DocumentException, IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/report/gazprom-transgaz-logo.png")) {
            if (inputStream == null) {
                throw new IllegalStateException("Corporate logo resource is missing");
            }
            Image logo = Image.getInstance(inputStream.readAllBytes());
            logo.scaleToFit(140, 52);
            return logo;
        }
    }

    private PdfPTable buildDetailsTable(ReportPdfContext context, Font labelFont, Font valueFont)
            throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.8f, 3.2f});
        table.setSpacingBefore(4);

        addDetailRow(table, "Филиал", context.branchName(), labelFont, valueFont);
        addDetailRow(table, "Объект", context.objectBuilding(), labelFont, valueFont);
        addDetailRow(table, "Краткое описание дефекта", context.description(), labelFont, valueFont);
        addDetailRow(
                table,
                "Дата обнаружения дефекта",
                formatDate(context.discoveredAt()),
                labelFont,
                valueFont
        );
        addDetailRow(
                table,
                "Служба, ответственная за устранение",
                context.responsibleOrganization(),
                labelFont,
                valueFont
        );
        addDetailRow(
                table,
                "Дата устранения",
                formatDateTime(context.resolvedAt()),
                labelFont,
                valueFont
        );

        return table;
    }

    private void addDetailRow(
            PdfPTable table,
            String label,
            String value,
            Font labelFont,
            Font valueFont
    ) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBackgroundColor(CORPORATE_LIGHT);
        labelCell.setPadding(10);
        labelCell.setBorderColor(BORDER_COLOR);

        PdfPCell valueCell = new PdfPCell(new Phrase(Objects.toString(value, "—"), valueFont));
        valueCell.setPadding(10);
        valueCell.setBorderColor(BORDER_COLOR);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private String formatDate(java.time.LocalDate date) {
        if (date == null) {
            return "—";
        }
        return DATE_FORMATTER.format(date);
    }

    private String formatDateTime(java.time.Instant instant) {
        if (instant == null) {
            return "—";
        }
        return DATE_TIME_FORMATTER.format(instant.atZone(REPORT_ZONE));
    }

    private BaseFont loadFont(String resourcePath) throws DocumentException, IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Font resource is missing: " + resourcePath);
            }
            return BaseFont.createFont(
                    resourcePath,
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED,
                    true,
                    inputStream.readAllBytes(),
                    null
            );
        }
    }
}
