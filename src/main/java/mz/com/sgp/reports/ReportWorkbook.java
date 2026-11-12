package mz.com.sgp.reports;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/**
 * Minimal OOXML workbook: numeric cells remain numeric and user text is never a formula.
 * Inclui o logótipo (classpath report-logo.png), título e cabeçalho formatados e a tabela fixa ao deslocar.
 */
public final class ReportWorkbook {
    private ReportWorkbook() {}

    private static final String LOGO = "/report-logo.png";
    // Logótipo 240x158 px, mostrado a 96x63 px (1 px = 9525 EMU).
    private static final long LOGO_CX = 96L * 9525, LOGO_CY = 63L * 9525;

    // Índices em cellXfs (styles.xml).
    private static final int TITLE = 1, MUTED = 2, HEADER = 3, LABEL = 4, MONEY = 5, NUMBER = 6, METRIC = 7;

    public static byte[] write(ReportService.Report report) throws IOException {
        byte[] logo = logo();
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            entry(zip, "[Content_Types].xml", "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Default Extension=\"png\" ContentType=\"image/png\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
                + (logo != null ? "<Override PartName=\"/xl/drawings/drawing1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.drawing+xml\"/>" : "")
                + "</Types>");
            entry(zip, "_rels/.rels", "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            entry(zip, "xl/workbook.xml", "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Relatório\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            entry(zip, "xl/_rels/workbook.xml.rels", "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/><Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>");
            entry(zip, "xl/styles.xml", STYLES);
            entry(zip, "xl/worksheets/sheet1.xml", sheet(report, logo != null));
            if (logo != null) {
                entry(zip, "xl/worksheets/_rels/sheet1.xml.rels", "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing\" Target=\"../drawings/drawing1.xml\"/></Relationships>");
                entry(zip, "xl/drawings/drawing1.xml", "<xdr:wsDr xmlns:xdr=\"http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing\" xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><xdr:oneCellAnchor><xdr:from><xdr:col>0</xdr:col><xdr:colOff>76200</xdr:colOff><xdr:row>0</xdr:row><xdr:rowOff>38100</xdr:rowOff></xdr:from><xdr:ext cx=\"" + LOGO_CX + "\" cy=\"" + LOGO_CY + "\"/><xdr:pic><xdr:nvPicPr><xdr:cNvPr id=\"2\" name=\"Logótipo\"/><xdr:cNvPicPr><a:picLocks noChangeAspect=\"1\"/></xdr:cNvPicPr></xdr:nvPicPr><xdr:blipFill><a:blip r:embed=\"rId1\"/><a:stretch><a:fillRect/></a:stretch></xdr:blipFill><xdr:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"" + LOGO_CX + "\" cy=\"" + LOGO_CY + "\"/></a:xfrm><a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></xdr:spPr></xdr:pic><xdr:clientData/></xdr:oneCellAnchor></xdr:wsDr>");
                entry(zip, "xl/drawings/_rels/drawing1.xml.rels", "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"../media/logo.png\"/></Relationships>");
                zip.putNextEntry(new ZipEntry("xl/media/logo.png")); zip.write(logo); zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private static String sheet(ReportService.Report report, boolean withLogo) {
        int columns = Math.max(report.columns().size(), 2);
        var rows = new StringBuilder();
        int r = 0;
        // Cabeçalho: logótipo na coluna A (linhas 1-3), texto a partir da coluna B.
        rows.append(row(++r, 26, cell(r, 1, "Mariscos do Índico — " + report.title(), TITLE)));
        rows.append(row(++r, 0, cell(r, 1, report.period(), MUTED)));
        rows.append(row(++r, 0, cell(r, 1, "Gerado em " + date(report.generatedAt()) + " · " + report.totalElements() + " registos", MUTED)));
        r++; // linha em branco
        for (var metric : report.metrics()) {
            r++;
            rows.append(row(r, 0, cell(r, 0, metric.label(), LABEL) + cell(r, 1, metric.value(), "money".equals(metric.kind()) ? METRIC : NUMBER)));
        }
        r++; // linha em branco
        int header = ++r;
        var headerCells = new StringBuilder();
        for (int c = 0; c < report.columns().size(); c++) headerCells.append(cell(r, c, report.columns().get(c).label(), HEADER));
        rows.append(row(r, 22, headerCells.toString()));
        for (var values : report.rows()) {
            r++;
            var cells = new StringBuilder();
            for (int c = 0; c < values.size(); c++) {
                String kind = c < report.columns().size() ? report.columns().get(c).kind() : "";
                Object value = "date".equals(kind) && values.get(c) != null ? date(String.valueOf(values.get(c))) : values.get(c);
                cells.append(cell(r, c, value, "money".equals(kind) ? MONEY : "number".equals(kind) ? NUMBER : 0));
            }
            rows.append(row(r, 0, cells.toString()));
        }
        String lastColumn = column(columns - 1);
        return "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
            + "<sheetViews><sheetView workbookViewId=\"0\" showGridLines=\"0\"><pane ySplit=\"" + header + "\" topLeftCell=\"A" + (header + 1) + "\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>"
            + "<cols><col min=\"1\" max=\"1\" width=\"" + (withLogo ? 26 : 24) + "\" customWidth=\"1\"/><col min=\"2\" max=\"" + Math.max(columns, 8) + "\" width=\"22\" customWidth=\"1\"/></cols>"
            + "<sheetData>" + rows + "</sheetData>"
            + (report.rows().isEmpty() ? "" : "<autoFilter ref=\"A" + header + ":" + lastColumn + r + "\"/>")
            + "<pageSetup orientation=\"landscape\"/>"
            + (withLogo ? "<drawing r:id=\"rId1\"/>" : "")
            + "</worksheet>";
    }

    private static String row(int index, int height, String cells) {
        return "<row r=\"" + index + "\"" + (height > 0 ? " ht=\"" + height + "\" customHeight=\"1\"" : "") + ">" + cells + "</row>";
    }

    private static String cell(int row, int col, Object value, int style) {
        String ref = column(col) + row;
        String s = style > 0 ? " s=\"" + style + "\"" : "";
        if (value instanceof Number) return "<c r=\"" + ref + "\"" + s + "><v>" + value + "</v></c>";
        return "<c r=\"" + ref + "\"" + s + " t=\"inlineStr\"><is><t xml:space=\"preserve\">" + escape(String.valueOf(value)) + "</t></is></c>";
    }

    private static String column(int index) {
        var name = new StringBuilder();
        for (int i = index + 1; i > 0; i = (i - 1) / 26) name.insert(0, (char) ('A' + (i - 1) % 26));
        return name.toString();
    }

    private static final java.time.format.DateTimeFormatter DATE = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Datas ISO (2026-09-30T23:59:59) passam a 30/09/2026 23:59; outro texto fica como está.
    private static String date(String value) {
        try {
            return java.time.LocalDateTime.parse(value).format(DATE);
        } catch (java.time.format.DateTimeParseException e) {
            return value;
        }
    }

    private static byte[] logo() throws IOException {
        try (InputStream in = ReportWorkbook.class.getResourceAsStream(LOGO)) {
            return in == null ? null : in.readAllBytes();
        }
    }

    private static String escape(String text) {
        return text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static void entry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name)); zip.write(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + content).getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
    }

    // Azul da marca #0077B6; cinzento #5B6B7A. numFmt 164 = quantidades com até 3 casas.
    private static final String STYLES = "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
        + "<numFmts count=\"1\"><numFmt numFmtId=\"164\" formatCode=\"#,##0.###\"/></numFmts>"
        + "<fonts count=\"5\">"
        + "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
        + "<font><b/><sz val=\"16\"/><color rgb=\"FF0077B6\"/><name val=\"Calibri\"/></font>"
        + "<font><sz val=\"10\"/><color rgb=\"FF5B6B7A\"/><name val=\"Calibri\"/></font>"
        + "<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font>"
        + "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font>"
        + "</fonts>"
        + "<fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill>"
        + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF0077B6\"/><bgColor indexed=\"64\"/></patternFill></fill></fills>"
        + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
        + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
        + "<cellXfs count=\"8\">"
        + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
        + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"
        + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"
        + "<xf numFmtId=\"0\" fontId=\"3\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyAlignment=\"1\"><alignment vertical=\"center\"/></xf>"
        + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"
        + "<xf numFmtId=\"4\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"
        + "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"
        + "<xf numFmtId=\"4\" fontId=\"4\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyNumberFormat=\"1\"/>"
        + "</cellXfs>"
        + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
        + "</styleSheet>";
}
