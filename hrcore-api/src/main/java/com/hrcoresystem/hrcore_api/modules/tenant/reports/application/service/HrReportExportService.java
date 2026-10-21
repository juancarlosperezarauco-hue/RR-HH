package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.dto.ReportFilter;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.springframework.stereotype.Service;

@Service
public class HrReportExportService {
    private static final Color BLUE = new Color(15, 73, 143);
    private static final Map<String, String> LABELS = labels();
    private final HrReportService reports;

    public HrReportExportService(HrReportService reports) {
        this.reports = reports;
    }

    public ExportedReport export(String report, String requestedFormat, ReportFilter filter) {
        reports.assertExportAllowed(report);
        ExportFormat format = ExportFormat.parse(requestedFormat);
        Map<String, Object> data = reports.data(report, filter);
        byte[] content = switch (format) {
            case XLSX -> excel(report, data, filter);
            case DOCX -> word(report, data, filter);
            case PDF -> pdf(report, data, filter);
        };
        return new ExportedReport(content, format.contentType, format.extension,
                "informe-" + report + "." + format.extension);
    }

    private byte[] excel(String report, Map<String, Object> data, ReportFilter filter) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Informe");
            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true); titleFont.setFontHeightInPoints((short) 16); titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            titleStyle.setFont(titleFont);
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont(); headerFont.setBold(true); headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont); headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex()); headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND); headerStyle.setAlignment(HorizontalAlignment.CENTER);
            Row title = sheet.createRow(0); title.createCell(0).setCellValue("Informe de RR. HH. - " + reportLabel(report)); title.getCell(0).setCellStyle(titleStyle);
            int rowIndex = writeExcelFilters(sheet, filter, 2, headerStyle);
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getValue() instanceof List<?> rows) {
                    rowIndex++;
                    Row section = sheet.createRow(rowIndex++); section.createCell(0).setCellValue(label(entry.getKey())); section.getCell(0).setCellStyle(headerStyle);
                    section.createCell(1).setCellValue("Valor"); section.getCell(1).setCellStyle(headerStyle);
                    for (Object item : rows) {
                        Map<?, ?> values = item instanceof Map<?, ?> map ? map : Map.of("label", item, "total", "");
                        Row row = sheet.createRow(rowIndex++);
                        row.createCell(0).setCellValue(valueLabel(values.get("label")));
                        setExcelValue(row, 1, mapValue(values, "total"));
                    }
                } else {
                    Row row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(label(entry.getKey()));
                    setExcelValue(row, 1, entry.getValue());
                }
            }
            sheet.setColumnWidth(0, 42 * 256); sheet.setColumnWidth(1, 24 * 256);
            workbook.write(output); return output.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException("No se pudo generar el archivo Excel");
        }
    }

    private int writeExcelFilters(Sheet sheet, ReportFilter filter, int rowIndex, CellStyle headerStyle) {
        Row header = sheet.createRow(rowIndex++); header.createCell(0).setCellValue("Criterio"); header.getCell(0).setCellStyle(headerStyle); header.createCell(1).setCellValue("Valor"); header.getCell(1).setCellStyle(headerStyle);
        for (Map.Entry<String, Object> entry : filterValues(filter).entrySet()) {
            Row row = sheet.createRow(rowIndex++); row.createCell(0).setCellValue(entry.getKey()); row.createCell(1).setCellValue(String.valueOf(entry.getValue()));
        }
        return rowIndex;
    }

    private void setExcelValue(Row row, int column, Object value) {
        if (value instanceof Number number) row.createCell(column).setCellValue(number.doubleValue());
        else row.createCell(column).setCellValue(value == null ? "" : String.valueOf(value));
    }

    private byte[] word(String report, Map<String, Object> data, ReportFilter filter) {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFParagraph title = document.createParagraph(); title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun(); titleRun.setText("Informe de RR. HH. - " + reportLabel(report)); titleRun.setBold(true); titleRun.setFontSize(18); titleRun.setColor("0F498F");
            addWordTable(document, "Criterios aplicados", filterValues(filter));
            Map<String, Object> indicators = new LinkedHashMap<>();
            data.forEach((key, value) -> { if (!(value instanceof List<?>)) indicators.put(label(key), value); });
            addWordTable(document, "Indicadores", indicators);
            data.forEach((key, value) -> { if (value instanceof List<?> rows) addWordRows(document, label(key), rows); });
            document.write(output); return output.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException("No se pudo generar el archivo Word");
        }
    }

    private void addWordTable(XWPFDocument document, String heading, Map<String, ?> values) {
        XWPFParagraph paragraph = document.createParagraph(); XWPFRun run = paragraph.createRun(); run.setText(heading); run.setBold(true); run.setFontSize(13);
        XWPFTable table = document.createTable(Math.max(1, values.size()) + 1, 2);
        wordHeader(table.getRow(0).getCell(0), "Indicador"); wordHeader(table.getRow(0).getCell(1), "Valor");
        int row = 1;
        if (values.isEmpty()) { table.getRow(row).getCell(0).setText("Sin criterios adicionales"); table.getRow(row).getCell(1).setText("-"); return; }
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            table.getRow(row).getCell(0).setText(entry.getKey()); table.getRow(row).getCell(1).setText(String.valueOf(entry.getValue())); row++;
        }
    }

    private void addWordRows(XWPFDocument document, String heading, List<?> rows) {
        XWPFParagraph paragraph = document.createParagraph(); XWPFRun run = paragraph.createRun(); run.setText(heading); run.setBold(true); run.setFontSize(13);
        XWPFTable table = document.createTable(Math.max(1, rows.size()) + 1, 2);
        wordHeader(table.getRow(0).getCell(0), "Categoría"); wordHeader(table.getRow(0).getCell(1), "Valor");
        int index = 1;
        if (rows.isEmpty()) { table.getRow(index).getCell(0).setText("Sin datos"); table.getRow(index).getCell(1).setText("0"); return; }
        for (Object item : rows) {
            Map<?, ?> values = item instanceof Map<?, ?> map ? map : Map.of("label", item, "total", "");
            table.getRow(index).getCell(0).setText(valueLabel(values.get("label")));
            table.getRow(index).getCell(1).setText(String.valueOf(mapValue(values, "total"))); index++;
        }
    }

    private void wordHeader(XWPFTableCell cell, String value) {
        cell.setColor("0F498F"); cell.setText(value);
        for (XWPFParagraph paragraph : cell.getParagraphs()) for (XWPFRun run : paragraph.getRuns()) { run.setBold(true); run.setColor("FFFFFF"); }
    }

    private byte[] pdf(String report, Map<String, Object> data, ReportFilter filter) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(); PdfWriter.getInstance(document, output); document.open();
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 17, BLUE);
            Paragraph title = new Paragraph("Informe de RR. HH. - " + reportLabel(report), titleFont); title.setAlignment(Element.ALIGN_CENTER); title.setSpacingAfter(16); document.add(title);
            addPdfTable(document, "Criterios aplicados", filterValues(filter));
            Map<String, Object> indicators = new LinkedHashMap<>(); data.forEach((key, value) -> { if (!(value instanceof List<?>)) indicators.put(label(key), value); });
            addPdfTable(document, "Indicadores", indicators);
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getValue() instanceof List<?> rows) {
                    addPdfRows(document, label(entry.getKey()), rows);
                }
            }
            document.close(); return output.toByteArray();
        } catch (Exception exception) {
            throw new BusinessException("No se pudo generar el archivo PDF");
        }
    }

    private void addPdfTable(Document document, String heading, Map<String, ?> values) throws Exception {
        document.add(new Paragraph(heading, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        PdfPTable table = pdfTable();
        if (values.isEmpty()) { pdfCell(table, "Sin criterios adicionales", false); pdfCell(table, "-", false); }
        else for (Map.Entry<String, ?> entry : values.entrySet()) { pdfCell(table, entry.getKey(), false); pdfCell(table, String.valueOf(entry.getValue()), false); }
        document.add(table);
    }

    private void addPdfRows(Document document, String heading, List<?> rows) throws Exception {
        document.add(new Paragraph(heading, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        PdfPTable table = pdfTable();
        if (rows.isEmpty()) { pdfCell(table, "Sin datos", false); pdfCell(table, "0", false); }
        else for (Object item : rows) {
            Map<?, ?> values = item instanceof Map<?, ?> map ? map : Map.of("label", item, "total", "");
            pdfCell(table, valueLabel(values.get("label")), false); pdfCell(table, String.valueOf(mapValue(values, "total")), false);
        }
        document.add(table);
    }

    private PdfPTable pdfTable() {
        PdfPTable table = new PdfPTable(new float[]{3, 1}); table.setWidthPercentage(100); table.setSpacingBefore(6); table.setSpacingAfter(12);
        pdfCell(table, "Indicador", true); pdfCell(table, "Valor", true); return table;
    }

    private void pdfCell(PdfPTable table, String value, boolean header) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, header ? Color.WHITE : Color.BLACK);
        PdfPCell cell = new PdfPCell(new Phrase(value, font)); cell.setPadding(6);
        if (header) { cell.setBackgroundColor(BLUE); cell.setHorizontalAlignment(Element.ALIGN_CENTER); }
        table.addCell(cell);
    }

    private Map<String, Object> filterValues(ReportFilter filter) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (filter.fromDate() != null) values.put("Fecha inicial", filter.fromDate());
        if (filter.toDate() != null) values.put("Fecha final", filter.toDate());
        if (filter.departmentId() != null) values.put("Departamento", filter.departmentId());
        if (filter.positionId() != null) values.put("Cargo", filter.positionId());
        if (filter.employeeId() != null) values.put("Empleado", filter.employeeId());
        if (filter.employmentPeriodId() != null) values.put("Periodo laboral", filter.employmentPeriodId());
        if (filter.status() != null && !filter.status().isBlank()) values.put("Estado", valueLabel(filter.status()));
        return values;
    }

    private String reportLabel(String report) {
        return switch (report) {
            case "employees" -> "Empleados"; case "departments" -> "Departamentos"; case "positions" -> "Cargos";
            case "contracts" -> "Contratos"; case "files" -> "Expedientes"; case "shifts" -> "Turnos";
            case "attendance" -> "Asistencia"; case "leaves" -> "Ausencias y permisos"; case "incidents" -> "Incidencias";
            case "integrated" -> "Integrado"; default -> report;
        };
    }

    private String label(String key) {
        return LABELS.getOrDefault(key, key.replaceAll("([a-z])([A-Z])", "$1 $2").replace('_', ' '));
    }

    private String valueLabel(Object value) {
        if (value == null) return "Sin dato";
        String text = String.valueOf(value); String translated = LABELS.get(text);
        if (translated != null) return translated;
        String normalized = text.toLowerCase(Locale.ROOT).replace('_', ' ');
        return normalized.isEmpty() ? normalized : Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private Object mapValue(Map<?, ?> values, String key) {
        Object value = values.get(key);
        return value == null ? "" : value;
    }

    private static Map<String, String> labels() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("totalEmployees","Total de empleados"); values.put("activeEmployees","Empleados activos"); values.put("inactiveEmployees","Empleados inactivos");
        values.put("terminatedEmployees","Empleados retirados"); values.put("suspendedEmployees","Empleados suspendidos"); values.put("onLeaveEmployees","Empleados con permiso");
        values.put("totalDepartments","Total de departamentos"); values.put("totalPositions","Total de cargos"); values.put("totalContracts","Total de contratos");
        values.put("totalFiles","Total de expedientes"); values.put("totalDocuments","Total de documentos"); values.put("totalShifts","Total de turnos");
        values.put("totalRecords","Total de registros"); values.put("totalRequests","Total de solicitudes"); values.put("totalIncidents","Total de incidencias");
        values.put("byDepartment","Por departamento"); values.put("byPosition","Por cargo"); values.put("byStatus","Por estado"); values.put("byType","Por tipo");
        values.put("bySeverity","Por gravedad"); values.put("byDocumentType","Documentos por tipo"); values.put("byShift","Empleados por turno");
        values.put("dailyTrend","Evolución diaria"); values.put("attendanceByDepartment","Asistencia por departamento"); values.put("absencesByDepartment","Ausencias por departamento");
        values.put("ACTIVE","Activo"); values.put("INACTIVE","Inactivo"); values.put("TERMINATED","Finalizado"); values.put("APPROVED","Aprobado");
        values.put("REJECTED","Rechazado"); values.put("PENDING","Pendiente"); values.put("OPEN","Abierto"); values.put("CLOSED","Cerrado");
        values.put("RESOLVED","Resuelto"); values.put("UNDER_REVIEW","En revisión"); values.put("PRESENT","Presente"); values.put("ABSENT","Ausente"); values.put("LATE","Retraso");
        return Map.copyOf(values);
    }

    public record ExportedReport(byte[] content, String contentType, String extension, String filename) {}

    private enum ExportFormat {
        XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
        DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
        PDF("application/pdf", "pdf");
        private final String contentType; private final String extension;
        ExportFormat(String contentType, String extension) { this.contentType = contentType; this.extension = extension; }
        static ExportFormat parse(String value) {
            try { return valueOf(value.trim().toUpperCase(Locale.ROOT)); }
            catch (RuntimeException exception) { throw new BusinessException("Formato de exportación no soportado"); }
        }
    }
}
