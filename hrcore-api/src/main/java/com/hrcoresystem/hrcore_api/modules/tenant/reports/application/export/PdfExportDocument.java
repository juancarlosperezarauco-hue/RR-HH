package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.QueryResultV1;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AiExportLimitException;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.awt.Color;
import java.io.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

final class PdfExportDocument implements ReportExportDocument {
    private static final Color BRAND = new Color(30, 64, 105);
    private final Document document = new Document(PageSize.A4.rotate(), 32, 32, 36, 36);
    private final Font normal, bold;
    private PdfPTable table;
    private List<QueryResultV1.Column> columns;
    private int count;

    PdfExportDocument(OutputStream output, ZonedDateTime generated, JsonNode plan) {
        try {
            normal = font("Regular", 9, Color.DARK_GRAY);
            bold = font("Bold", 9, Color.WHITE);
            PdfWriter writer = PdfWriter.getInstance(document, output);
            writer.setCloseStream(false);
            writer.setPageEvent(new PdfPageEventHelper() {
                public void onEndPage(PdfWriter writer, Document doc) {
                    ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                            new Phrase("Página " + writer.getPageNumber(), normal), doc.right(), 18, 0);
                }
            });
            document.addTitle("HRCore — Reporte analítico"); document.addAuthor("HRCore");
            document.open();
            document.add(new Paragraph("HRCore", font("Bold", 19, BRAND)));
            document.add(new Paragraph("employees.roster".equals(plan.path("dataset").asText())
                    ? "Reporte de empleados" : "Reporte de asistencia", font("Bold", 13, BRAND)));
            document.add(new Paragraph("Generado: " + generated.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss VV")), normal));
            document.add(new Paragraph("Datos actuales al ejecutar la exportación. Los valores vacíos se muestran como —.", normal));
            if (plan.has("asOfDate")) document.add(new Paragraph("Fecha de referencia: " + plan.path("asOfDate").asText(), normal));
            if (plan.has("dateRange")) document.add(new Paragraph("Período: " + plan.at("/dateRange/from").asText()
                    + " hasta antes de " + plan.at("/dateRange/toExclusive").asText(), normal));
            for (JsonNode filter : plan.path("filters")) {
                document.add(new Paragraph(ExportLabels.label(filter.path("field").asText()) + " "
                        + operator(filter.path("op").asText()) + (filter.has("value") ? " " + supported(display(filter.get("value"))) : ""), normal));
            }
            for (JsonNode having : plan.path("having")) document.add(new Paragraph(
                    ExportLabels.label(having.path("metric").asText()) + " " + operator(having.path("op").asText())
                            + " " + display(having.get("value")), normal));
            document.add(new Paragraph(" ", normal));
        } catch (IOException failure) { throw new UncheckedIOException(failure); }
    }

    private static String display(JsonNode value) { return value.isValueNode() ? value.asText() : value.toString(); }
    private static String operator(String op) {
        return switch (op) {
            case "eq" -> "="; case "neq" -> "≠"; case "gt" -> ">"; case "gte" -> "≥";
            case "lt" -> "<"; case "lte" -> "≤"; case "in" -> "en"; case "between" -> "entre";
            case "is_null" -> "sin valor"; case "is_not_null" -> "con valor";
            default -> op;
        };
    }
    private String supported(String text) {
        if (text.codePoints().anyMatch(cp -> cp != '\n' && cp != '\r' && cp != '\t' && !normal.getBaseFont().charExists(cp)))
            throw new AiExportLimitException("La fuente PDF no puede representar algunos caracteres del reporte. Descarga Excel o CSV para conservarlos.");
        return text.replace('\t', ' ');
    }
    private static Font font(String variant, float size, Color color) throws IOException {
        try (InputStream input = PdfExportDocument.class.getResourceAsStream("/liberation/LiberationSans-" + variant + ".ttf")) {
            if (input == null) throw new IOException("Export font is unavailable");
            BaseFont base = BaseFont.createFont("LiberationSans-" + variant + ".ttf", BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED, true, input.readAllBytes(), null);
            return new Font(base, size, Font.NORMAL, color);
        }
    }
    public void columns(List<QueryResultV1.Column> columns) {
        this.columns = columns;
        table = new PdfPTable(columns.size()); table.setWidthPercentage(100); table.setHeaderRows(1);
        table.setSplitRows(true); table.setSplitLate(true); table.setComplete(false);
        for (var column : columns) {
            PdfPCell cell = new PdfPCell(new Phrase(ExportLabels.label(column.key()), bold));
            cell.setBackgroundColor(BRAND); cell.setPadding(5); table.addCell(cell);
        }
    }
    public void row(Map<String, Object> values) {
        for (var column : columns) {
            Object value = values.get(column.key());
            PdfPCell cell = new PdfPCell(new Phrase(value == null ? "—" : supported(value.toString()), normal));
            cell.setPadding(4); cell.setBorderColor(new Color(220, 225, 230));
            if (value instanceof Number) cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(cell);
        }
        if (++count % 25 == 0) { document.add(table); table.flushContent(); }
    }
    public void finish() {
        if (count == 0) {
            PdfPCell empty = new PdfPCell(new Phrase("Sin filas para los filtros seleccionados.", normal));
            empty.setColspan(columns.size()); empty.setPadding(6); table.addCell(empty);
        }
        table.setComplete(true); document.add(table); document.close();
    }
    public void close() { if (document.isOpen()) document.close(); }
}
