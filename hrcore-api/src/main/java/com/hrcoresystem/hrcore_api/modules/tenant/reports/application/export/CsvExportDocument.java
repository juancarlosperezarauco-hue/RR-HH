package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.export;

import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.QueryResultV1;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

final class CsvExportDocument implements ReportExportDocument {
    private final Writer writer;
    private List<QueryResultV1.Column> columns;
    CsvExportDocument(OutputStream output) { writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8)); }
    public void columns(List<QueryResultV1.Column> columns) {
        this.columns = columns;
        line(columns.stream().map(c -> ExportLabels.label(c.key())).toList());
    }
    public void row(Map<String, Object> row) {
        line(columns.stream().map(c -> {
            Object value = row.get(c.key());
            return value == null ? "" : value instanceof String text ? safeText(text) : value.toString();
        }).toList());
    }
    static String safeText(String text) {
        int start = 0;
        while (start < text.length()) {
            char ch = text.charAt(start);
            if (!Character.isWhitespace(ch) && !Character.isSpaceChar(ch) && !Character.isISOControl(ch) && ch != '\uFEFF') break;
            start++;
        }
        String leading = text.substring(start);
        boolean control = !text.isEmpty() && (text.charAt(0) == '\t' || text.charAt(0) == '\r' || text.charAt(0) == '\n');
        return control || (!leading.isEmpty() && "=+-@＝＋－＠".indexOf(leading.charAt(0)) >= 0) ? "'" + text : text;
    }
    private void line(List<String> cells) {
        try {
            for (int i = 0; i < cells.size(); i++) {
                if (i > 0) writer.write(',');
                writer.write('"'); writer.write(cells.get(i).replace("\"", "\"\"")); writer.write('"');
            }
            writer.write("\r\n");
        } catch (IOException failure) { throw new UncheckedIOException(failure); }
    }
    public void finish() { try { writer.flush(); } catch (IOException failure) { throw new UncheckedIOException(failure); } }
    public void close() { /* The service owns the output stream. */ }
}
