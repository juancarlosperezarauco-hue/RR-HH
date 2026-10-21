package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.export;

import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AiExportLimitException;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.QueryResultV1;
import java.io.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

final class XlsxExportDocument implements ReportExportDocument {
    private final SXSSFWorkbook workbook = new SXSSFWorkbook(100);
    private final OutputStream output;
    private final Sheet sheet;
    private final CellStyle header, date;
    private List<QueryResultV1.Column> columns;
    private int rowNumber = 1;
    XlsxExportDocument(OutputStream output) {
        this.output = output;
        workbook.setCompressTempFiles(true);
        sheet = workbook.createSheet("Reporte"); sheet.createFreezePane(0, 1);
        header = workbook.createCellStyle();
        var font = workbook.createFont(); font.setBold(true); font.setColor(IndexedColors.WHITE.getIndex());
        header.setFont(font); header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        date = workbook.createCellStyle(); date.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy"));
    }
    public void columns(List<QueryResultV1.Column> columns) {
        this.columns = columns;
        Row row = sheet.createRow(0);
        for (int i = 0; i < columns.size(); i++) {
            Cell cell = row.createCell(i, CellType.STRING);
            cell.setCellValue(ExportLabels.label(columns.get(i).key())); cell.setCellStyle(header);
            sheet.setColumnWidth(i, 24 * 256);
        }
    }
    public void row(Map<String, Object> values) {
        Row row = sheet.createRow(rowNumber++);
        for (int i = 0; i < columns.size(); i++) {
            Object value = values.get(columns.get(i).key());
            Cell cell = row.createCell(i);
            if (value == null) continue;
            if (value instanceof Number number) {
                long integer = number.longValue();
                // Excel cannot preserve integers with more than 15 decimal digits as numeric cells.
                if (Math.abs((double) integer) > 999_999_999_999_999d) throw new AiExportLimitException();
                cell.setCellValue(number.doubleValue());
            } else if (value instanceof LocalDate localDate) {
                if (localDate.getYear() < 1900 || localDate.getYear() > 9999) throw new AiExportLimitException();
                cell.setCellValue(localDate); cell.setCellStyle(date);
            } else {
                String text = value.toString();
                if (text.length() > 32767) throw new AiExportLimitException();
                cell.setCellValue(text);
            }
        }
    }
    public void finish() { try { workbook.write(output); } catch (IOException failure) { throw new UncheckedIOException(failure); } }
    public void close() {
        try {
            try { ((org.apache.poi.xssf.streaming.SXSSFSheet) sheet).flushRows(); }
            finally { workbook.close(); }
        } catch (IOException failure) { throw new UncheckedIOException(failure); }
    }
}
