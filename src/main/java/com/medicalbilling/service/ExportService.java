package com.medicalbilling.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.medicalbilling.entity.Sale;
import com.medicalbilling.entity.SaleItem;
import com.medicalbilling.entity.ShopSettings;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final SettingsService settingsService;

    public byte[] exportToPdf(Sale sale) throws DocumentException {
        ShopSettings settings = settingsService.getSettings();
        Document document = new Document();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        document.add(new Paragraph(settings.getShopName(), titleFont));
        document.add(new Paragraph(settings.getAddress(), normalFont));
        document.add(new Paragraph("GST: " + settings.getGstNumber(), normalFont));
        document.add(Chunk.NEWLINE);

        document.add(new Paragraph("INVOICE", titleFont));
        document.add(new Paragraph("Bill No: " + sale.getBillNumber(), normalFont));
        document.add(new Paragraph("Date: " + sale.getSaleDate(), normalFont));
        document.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        addTableHeader(table, "Medicine", "Qty", "Price", "GST", "Total");

        for (SaleItem item : sale.getItems()) {
            table.addCell(cell(item.getMedicine().getMedicineName(), normalFont));
            table.addCell(cell(String.valueOf(item.getQuantity()), normalFont));
            table.addCell(cell(item.getUnitPrice().toString(), normalFont));
            table.addCell(cell(item.getGstAmount().toString(), normalFont));
            table.addCell(cell(item.getSubtotal().toString(), normalFont));
        }
        document.add(table);
        document.add(Chunk.NEWLINE);
        document.add(new Paragraph("Grand Total: Rs. " + sale.getGrandTotal(), titleFont));
        document.add(new Paragraph("Payment: " + sale.getPaymentMode(), normalFont));
        if (settings.getInvoiceFooter() != null) {
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph(settings.getInvoiceFooter(), normalFont));
        }

        document.close();
        return baos.toByteArray();
    }

    public byte[] exportReportToExcel(Map<String, Object> report) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Report");
            int rowNum = 0;
            for (Map.Entry<String, Object> entry : report.entrySet()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(entry.getValue() != null ? entry.getValue().toString() : "");
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    public byte[] exportReportToCsv(Map<String, Object> report) {
        StringBuilder csv = new StringBuilder("Key,Value\n");
        for (Map.Entry<String, Object> entry : report.entrySet()) {
            csv.append(entry.getKey()).append(",")
               .append(entry.getValue() != null ? "\"" + entry.getValue().toString().replace("\"", "\"\"") + "\"" : "")
               .append("\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void addTableHeader(PdfPTable table, String... headers) {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(cell);
        }
    }

    private PdfPCell cell(String text, Font font) {
        return new PdfPCell(new Phrase(text, font));
    }
}
