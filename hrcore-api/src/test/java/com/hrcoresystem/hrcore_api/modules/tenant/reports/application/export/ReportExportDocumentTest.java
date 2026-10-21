package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.*;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReportExportDocumentTest {
    private static final List<QueryResultV1.Column> COLUMNS = List.of(
            new QueryResultV1.Column("employee.full_name", SemanticCatalogV1.ValueType.TEXT),
            new QueryResultV1.Column("attendance.late_minutes", SemanticCatalogV1.ValueType.INTEGER),
            new QueryResultV1.Column("attendance.work_date", SemanticCatalogV1.ValueType.DATE),
            new QueryResultV1.Column("attendance.first_in_local_time", SemanticCatalogV1.ValueType.LOCAL_TIME));
    private Map<String, Object> values(String name) {
        var values = new LinkedHashMap<String, Object>();
        values.put("employee.full_name", name); values.put("attendance.late_minutes", -12L);
        values.put("attendance.work_date", LocalDate.of(2026, 1, 15));
        values.put("attendance.first_in_local_time", LocalTime.of(8, 5, 30)); return values;
    }

    @Test void csvRoundTripsQuotesDelimitersNewlinesNullsAndProtectsTextFormulas() {
        var out = new ByteArrayOutputStream();
        try (var csv = new CsvExportDocument(out)) {
            csv.columns(COLUMNS); csv.row(values("  =SUM(1,2) \"Áda\"\r\nΩ Ж"));
            csv.row(Collections.emptyMap()); csv.finish();
        }
        var rows = parseCsv(out.toString(StandardCharsets.UTF_8));
        assertEquals(3, rows.size()); assertEquals("Nombre completo", rows.getFirst().getFirst());
        assertEquals("'  =SUM(1,2) \"Áda\"\r\nΩ Ж", rows.get(1).getFirst());
        assertEquals("-12", rows.get(1).get(1)); assertEquals("2026-01-15", rows.get(1).get(2));
        assertEquals("08:05:30", rows.get(1).get(3));
        assertEquals(List.of("", "", "", ""), rows.get(2));
        for (String text : List.of("+x", "-x", "@x", "\ttext", "\rtext", "\ntext", "＝x", "\uFEFF=x", "\u00A0=x"))
            assertTrue(CsvExportDocument.safeText(text).startsWith("'"));
        assertEquals("Áda", CsvExportDocument.safeText("Áda"));
    }

    @Test void xlsxReopensWithTypedCellsNoFormulasAndBlankNulls() throws Exception {
        var out = new ByteArrayOutputStream();
        try (var xlsx = new XlsxExportDocument(out)) {
            xlsx.columns(COLUMNS); xlsx.row(values("=HYPERLINK(\"x\")"));
            xlsx.row(Collections.emptyMap()); xlsx.finish();
        }
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            var sheet = workbook.getSheet("Reporte"); assertNotNull(sheet);
            assertEquals("Nombre completo", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals(CellType.STRING, sheet.getRow(1).getCell(0).getCellType());
            assertEquals("=HYPERLINK(\"x\")", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals(-12d, sheet.getRow(1).getCell(1).getNumericCellValue());
            assertEquals(LocalDate.of(2026, 1, 15), sheet.getRow(1).getCell(2).getLocalDateTimeCellValue().toLocalDate());
            assertEquals(CellType.BLANK, sheet.getRow(2).getCell(0).getCellType());
            assertEquals("08:05:30", sheet.getRow(1).getCell(3).getStringCellValue());
        }
    }

    @Test void xlsxRejectsTextOrIntegersThatCannotBeRepresentedFaithfully() {
        try (var xlsx = new XlsxExportDocument(new ByteArrayOutputStream())) {
            xlsx.columns(COLUMNS);
            assertThrows(AiExportLimitException.class, () -> xlsx.row(values("a".repeat(32768))));
        }
        try (var xlsx = new XlsxExportDocument(new ByteArrayOutputStream())) {
            xlsx.columns(COLUMNS); var row = values("Ficticio"); row.put("attendance.late_minutes", Long.MAX_VALUE);
            assertThrows(AiExportLimitException.class, () -> xlsx.row(row));
        }
    }

    @Test void pdfReopensWithUnicodeRepeatedHeadersAndNumberedPages() throws Exception {
        var out = new ByteArrayOutputStream();
        var plan = new ObjectMapper().readTree("{\"dataset\":\"employees.roster\",\"asOfDate\":\"2026-01-15\",\"filters\":[],\"having\":[]}");
        try (var pdf = new PdfExportDocument(out, ZonedDateTime.of(2026, 10, 20, 10, 0, 0, 0,
                ZoneId.of("America/La_Paz")), plan)) {
            pdf.columns(COLUMNS);
            for (int i = 0; i < 180; i++) pdf.row(values("Áda Ω Ж número " + i));
            pdf.finish();
        }
        try (var reader = new PdfReader(out.toByteArray())) {
            assertTrue(reader.getNumberOfPages() > 1);
            var extractor = new PdfTextExtractor(reader);
            String all = "";
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                String text = extractor.getTextFromPage(page); all += text;
                assertTrue(text.contains("Nombre completo"), text);
                assertTrue(text.contains("Página " + page), text);
            }
            assertTrue(all.contains("Áda Ω Ж")); assertTrue(all.contains("número 179"));
            assertTrue(all.contains("America/La_Paz")); assertTrue(all.contains("2026-01-15"));
        }
    }

    @Test void emptyFilesStillHaveColumnsAndPdfExplainsNoRows() throws Exception {
        var out = new ByteArrayOutputStream();
        try (var pdf = new PdfExportDocument(out, ZonedDateTime.now(ZoneId.of("America/La_Paz")),
                new ObjectMapper().readTree("{\"dataset\":\"employees.roster\",\"filters\":[],\"having\":[]}"))) {
            pdf.columns(COLUMNS); pdf.finish();
        }
        try (var reader = new PdfReader(out.toByteArray())) {
            assertTrue(new PdfTextExtractor(reader).getTextFromPage(1).contains("Sin filas"));
        }
        out.reset();
        try (var xlsx = new XlsxExportDocument(out)) { xlsx.columns(COLUMNS); xlsx.finish(); }
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            assertEquals(0, workbook.getSheetAt(0).getLastRowNum());
        }
        out.reset();
        try (var csv = new CsvExportDocument(out)) { csv.columns(COLUMNS); csv.finish(); }
        assertEquals(1, parseCsv(out.toString(StandardCharsets.UTF_8)).size());
    }

    @Test void pdfRejectsUnsupportedGlyphsInsteadOfSilentlyDroppingCharacters() throws Exception {
        try (var pdf = new PdfExportDocument(new ByteArrayOutputStream(), ZonedDateTime.now(ZoneId.of("America/La_Paz")),
                new ObjectMapper().readTree("{\"dataset\":\"employees.roster\",\"filters\":[],\"having\":[]}"))) {
            pdf.columns(COLUMNS);
            assertThrows(AiExportLimitException.class, () -> pdf.row(values("漢字")));
        }
    }

    @Test void budgetsRejectRawOutputAndElapsedTime() throws Exception {
        var budget = new ExportBudget(10, 30);
        assertThrows(AiExportLimitException.class, () -> budget.input(11));
        assertThrows(AiExportLimitException.class, () -> budget.output(new ByteArrayOutputStream()).write(new byte[11]));
        var expired = new ExportBudget(100, 1); Thread.sleep(1100);
        assertEquals(QueryPlanException.Code.QUERY_TIMEOUT, assertThrows(QueryPlanException.class, expired::check).code());
    }

    /** Independent RFC 4180 test parser, including embedded CRLF and doubled quotes. */
    static List<List<String>> parseCsv(String csv) {
        var rows = new ArrayList<List<String>>(); var cells = new ArrayList<String>();
        var cell = new StringBuilder(); boolean quoted = false;
        for (int i = 0; i < csv.length(); i++) {
            char ch = csv.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < csv.length() && csv.charAt(i + 1) == '"') { cell.append('"'); i++; }
                else quoted = !quoted;
            } else if (!quoted && ch == ',') { cells.add(cell.toString()); cell.setLength(0); }
            else if (!quoted && ch == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') {
                cells.add(cell.toString()); rows.add(List.copyOf(cells)); cells.clear(); cell.setLength(0); i++;
            } else cell.append(ch);
        }
        assertFalse(quoted); assertEquals(0, cell.length()); return rows;
    }
}
