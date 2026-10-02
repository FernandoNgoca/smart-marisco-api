package mz.com.sgp.reports;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Minimal OOXML workbook: numeric cells remain numeric and user text is never a formula. */
public final class ReportWorkbook {
    private ReportWorkbook() {}
    public static byte[] write(ReportService.Report report) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            entry(zip, "[Content_Types].xml", "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
            entry(zip, "_rels/.rels", "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            entry(zip, "xl/workbook.xml", "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Relatório\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            entry(zip, "xl/_rels/workbook.xml.rels", "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
            var xml = new StringBuilder("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><cols><col min=\"1\" max=\"8\" width=\"24\" customWidth=\"1\"/></cols><sheetData>");
            row(xml, List.of(report.title())); row(xml, List.of(report.period())); row(xml, List.of("Gerado em", report.generatedAt()));
            for (var metric : report.metrics()) row(xml, List.of(metric.label(), metric.value()));
            row(xml, report.columns().stream().map(ReportService.Column::label).toList());
            for (var values : report.rows()) row(xml, values);
            xml.append("</sheetData></worksheet>"); entry(zip, "xl/worksheets/sheet1.xml", xml.toString());
        }
        return bytes.toByteArray();
    }
    private static void row(StringBuilder xml, List<?> values) {
        xml.append("<row>");
        for (Object value : values) {
            if (value instanceof Number) xml.append("<c><v>").append(value).append("</v></c>");
            else xml.append("<c t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(escape(String.valueOf(value))).append("</t></is></c>");
        }
        xml.append("</row>");
    }
    private static String escape(String text) {
        return text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
    private static void entry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name)); zip.write(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + content).getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
    }
}
