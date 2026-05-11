package com.example.pdfoverlay.service;

import com.example.pdfoverlay.model.OverlayElement;
import com.example.pdfoverlay.model.OverlayElementType;
import com.example.pdfoverlay.model.OverlayPage;
import com.example.pdfoverlay.model.OverlayProject;
import com.example.pdfoverlay.model.PdfDocumentMetadata;
import com.example.pdfoverlay.model.PdfPageMetadata;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio para generar y cargar un HTML imprimible alineado al PDF.
 */
public final class HtmlExportService {
    private static final Logger LOGGER = Logger.getLogger(HtmlExportService.class.getName());

    private static final String METADATA_BEGIN = "PDF_OVERLAY_METADATA_BEGIN";
    private static final String METADATA_END = "PDF_OVERLAY_METADATA_END";

    private final PdfService pdfService;

    /**
     * Crea el servicio con dependencia al render de PDF.
     *
     * @param pdfService servicio de render PDF.
     */
    public HtmlExportService(PdfService pdfService) {
        this.pdfService = Objects.requireNonNull(pdfService, "pdfService is required");
    }

    /**
     * Exporta el proyecto como archivo HTML único (single file).
     *
     * @param project   proyecto a exportar.
     * @param htmlPath  ruta destino del archivo HTML.
     * @param renderDpi resolución para rasterizar páginas del PDF.
     * @return ruta del archivo HTML generado.
     * @throws IOException cuando falla la escritura.
     */
    public Path exportProjectAsHtml(OverlayProject project, Path htmlPath, float renderDpi) throws IOException {
        return exportProjectAsHtml(project, htmlPath, renderDpi, true, ExportOptions.defaultOptions());
    }

    /**
     * Exporta el proyecto como archivo HTML único con control de inclusión de fondo PDF.
     *
     * @param project              proyecto a exportar.
     * @param htmlPath             ruta destino del archivo HTML.
     * @param renderDpi            resolución para rasterizar páginas del PDF.
     * @param includePdfBackground true para incluir PDF como fondo, false para exportar solo overlay.
     * @return ruta del archivo HTML generado.
     * @throws IOException cuando falla la escritura.
     */
    public Path exportProjectAsHtml(OverlayProject project, Path htmlPath, float renderDpi, boolean includePdfBackground)
            throws IOException {
        return exportProjectAsHtml(project, htmlPath, renderDpi, includePdfBackground, ExportOptions.defaultOptions());
    }

    /**
     * Exporta el proyecto como archivo HTML único con opciones detalladas.
     *
     * @param project              proyecto a exportar.
     * @param htmlPath             ruta destino del archivo HTML.
     * @param renderDpi            resolución para rasterizar páginas del PDF.
     * @param includePdfBackground true para incluir PDF como fondo.
     * @param exportOptions        opciones visuales de exportación.
     * @return ruta del archivo HTML generado.
     * @throws IOException cuando falla la escritura.
     */
    public Path exportProjectAsHtml(OverlayProject project, Path htmlPath, float renderDpi,
                                    boolean includePdfBackground, ExportOptions exportOptions)
            throws IOException {
        Objects.requireNonNull(project, "project is required");
        Objects.requireNonNull(htmlPath, "htmlPath is required");
        Objects.requireNonNull(exportOptions, "exportOptions is required");
        if (renderDpi <= 0) {
            throw new IllegalArgumentException("renderDpi must be > 0");
        }

        Path parent = htmlPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        StringBuilder html = new StringBuilder();
        html.append(buildHtmlHeader(exportOptions));

        for (PdfPageMetadata pageMetadata : project.getMetadata().getPages()) {
            int pageIndex = pageMetadata.pageIndex();
            String imageDataUri = "";
            if (includePdfBackground) {
                byte[] pngBytes = pdfService.renderPageToPngBytes(project.getPdfPath(), pageIndex, renderDpi);
                imageDataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes);
            }
            OverlayPage overlayPage = project.getOverlayPage(pageIndex);
            html.append(buildPageMarkup(pageMetadata, overlayPage, imageDataUri, includePdfBackground));
        }

        html.append(buildMetadataComment(project));
        html.append("</body>\n</html>\n");

        Files.writeString(htmlPath, html.toString(), StandardCharsets.UTF_8);
        LOGGER.log(Level.INFO, "HTML exported: {0}", htmlPath);
        return htmlPath;
    }

    /**
     * Carga un proyecto desde un HTML previamente generado por esta aplicación.
     *
     * @param htmlPath ruta del archivo HTML.
     * @return proyecto reconstruido para edición.
     * @throws IOException cuando falla lectura del archivo.
     */
    public OverlayProject loadProjectFromHtml(Path htmlPath) throws IOException {
        Objects.requireNonNull(htmlPath, "htmlPath is required");
        if (!Files.exists(htmlPath) || !Files.isRegularFile(htmlPath)) {
            throw new IllegalArgumentException("Invalid HTML file: " + htmlPath);
        }

        String htmlContent = Files.readString(htmlPath, StandardCharsets.UTF_8);
        String metadata = extractMetadata(htmlContent);
        return parseProject(metadata);
    }

    private String buildHtmlHeader(ExportOptions options) {
        String fontFamily = options.exportFont() ? "font-family: \"Segoe UI\", Tahoma, sans-serif;" : "";
        String fontSize10 = options.exportFont() ? "font-size: 10pt;" : "";
        String fontSize9 = options.exportFont() ? "font-size: 9pt;" : "";
        String textFieldBorder = options.exportTextBorders() ? "border: 1px solid #4b5f73;" : "border: none;";
        String tableHeaderBorder = options.exportTableBorders() ? "border: 1px solid #3f5162;" : "border: none;";
        String tableCellBorder = options.exportTableBorders() ? "border: 1px solid #4f6273;" : "border: none;";
        String tableHeaderBackground = options.exportTableColors()
                ? "background: rgba(225, 235, 245, 0.85);"
                : "background: transparent;";
        String tableCellBackground = options.exportTableColors()
                ? "background: rgba(255, 255, 255, 0.55);"
                : "background: transparent;";

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>PDF Overlay Print</title>
                    <style>
                        * { box-sizing: border-box; }
                        body {
                            margin: 0;
                            padding: 12px;
                            background: #f0f2f4;
                            %s
                        }
                        table.print-page {
                            position: relative;
                            page-break-after: always;
                            break-after: page;
                            margin: 0 auto 12px;
                            padding: 0;
                            border: 1px solid #d1d7df;
                            border-collapse: collapse;
                            border-spacing: 0;
                            table-layout: fixed;
                            background-repeat: no-repeat;
                            background-size: 100%% 100%%;
                            background-position: center center;
                            overflow: hidden;
                        }
                        table.print-page > tbody > tr > td {
                            padding: 0;
                            margin: 0;
                            border: none;
                        }
                        .overlay-canvas {
                            width: 100%%;
                            height: 100%%;
                        }
                        table.overlay-item {
                            position: absolute;
                            border-collapse: collapse;
                            border-spacing: 0;
                            transform-origin: top left;
                        }
                        table.overlay-item > tbody > tr > td {
                            margin: 0;
                            padding: 1px 2px;
                            vertical-align: top;
                            line-height: 1.15;
                        }
                        table.overlay-text > tbody > tr > td {
                            %s
                            background: rgba(255, 255, 255, 0.15);
                            color: #0a0d11;
                            %s
                        }
                        table.overlay-label > tbody > tr > td {
                            color: #0a0d11;
                            %s
                            white-space: nowrap;
                        }
                        table.overlay-button > tbody > tr > td {
                            border: 1px solid #3d4d5d;
                            background: #e7edf4;
                            color: #0a0d11;
                            %s
                            text-align: center;
                        }
                        table.overlay-marker > tbody > tr > td {
                            padding: 0;
                            border-radius: 999px;
                            border: 1px solid #8d0000;
                            background: #e00000;
                        }
                        table.overlay-table {
                            table-layout: fixed;
                            border-collapse: collapse;
                            border-spacing: 0;
                            background: transparent;
                        }
                        table.overlay-table > colgroup > col {
                            border: none;
                        }
                        table.overlay-table > thead > tr > th {
                            %s
                            %s
                            color: #0a0d11;
                            %s
                            font-weight: 700;
                            padding: 2px 4px;
                            text-align: left;
                            vertical-align: top;
                        }
                        table.overlay-table > tbody > tr > td {
                            %s
                            %s
                            color: #0a0d11;
                            %s
                            padding: 2px 4px;
                            vertical-align: top;
                        }
                        @media print {
                            body {
                                margin: 0;
                                padding: 0;
                                background: white;
                            }
                            table.print-page {
                                margin: 0;
                                border: none;
                            }
                        }
                    </style>
                </head>
                <body>
                """.formatted(
                fontFamily,
                textFieldBorder,
                fontSize10,
                fontSize10,
                fontSize9,
                tableHeaderBorder,
                tableHeaderBackground,
                fontSize10,
                tableCellBorder,
                tableCellBackground,
                fontSize10
        );
    }

    private String buildPageMarkup(PdfPageMetadata pageMetadata, OverlayPage overlayPage,
                                   String imageDataUri, boolean includePdfBackground) {
        String widthInches = formatDouble(pageMetadata.widthInches());
        String heightInches = formatDouble(pageMetadata.heightInches());

        String pageClass = "page-" + (pageMetadata.pageIndex() + 1);
        StringBuilder builder = new StringBuilder();
        builder.append("""
                <style media="print">
                    @page %s {
                        size: %sin %sin;
                        margin: 0;
                    }
                    .%s {
                        page: %s;
                    }
                </style>
                """.formatted(pageClass, widthInches, heightInches, pageClass, pageClass));

        builder.append("""
                <table class="print-page %s" style="%s">
                    <tbody>
                        <tr>
                            <td class="overlay-canvas">
                """.formatted(pageClass, """
                width:%sin;
                height:%sin;
                %s
                """.formatted(
                widthInches,
                heightInches,
                includePdfBackground ? "background-image:url('" + escapeHtml(imageDataUri) + "');" : "background-image:none;"
        ).replace("\n", "")));

        for (OverlayElement element : overlayPage.mutableElements()) {
            builder.append(buildElementMarkup(element));
        }
        builder.append("""
                            </td>
                        </tr>
                    </tbody>
                </table>
                """);
        return builder.toString();
    }

    private String buildElementMarkup(OverlayElement element) {
        String left = percent(element.getXRatio());
        String top = percent(element.getYRatio());
        String width = percent(element.getWidthRatio());
        String height = percent(element.getHeightRatio());
        String text = escapeHtml(element.getText());

        String commonStyle = "left:%s;top:%s;width:%s;height:%s;".formatted(left, top, width, height);

        return switch (element.getType()) {
            case TEXT_FIELD -> """
                    <table class="overlay-item overlay-text" style="%s">
                        <tbody><tr><td>%s</td></tr></tbody>
                    </table>
                    """.formatted(commonStyle, text);
            case LABEL -> """
                    <table class="overlay-item overlay-label" style="%s">
                        <tbody><tr><td>%s</td></tr></tbody>
                    </table>
                    """.formatted(commonStyle, text);
            case BUTTON -> """
                    <table class="overlay-item overlay-button" style="%s">
                        <tbody><tr><td>%s</td></tr></tbody>
                    </table>
                    """.formatted(commonStyle, text.isBlank() ? "Button" : text);
            case MARKER -> """
                    <table class="overlay-item overlay-marker" style="%s">
                        <tbody><tr><td></td></tr></tbody>
                    </table>
                    """.formatted(commonStyle);
            case TABLE -> buildTableElementMarkup(element, commonStyle);
        };
    }

    private String buildTableElementMarkup(OverlayElement element, String commonStyle) {
        int columnCount = Math.max(1, element.getTableColumnCount());
        int detailRows = element.getTableDataRows();
        List<Double> columnWidths = parseTableColumnWidths(element.getTableColumnWidths(), columnCount);
        List<String> headers = parseTableHeaders(element.getText(), columnCount);

        StringBuilder builder = new StringBuilder();
        builder.append("<table class=\"overlay-item overlay-table\" style=\"")
                .append(commonStyle)
                .append("\">\n");

        builder.append("<colgroup>");
        for (double width : columnWidths) {
            builder.append("<col style=\"width:")
                    .append(formatDouble(width))
                    .append("%;\">");
        }
        builder.append("</colgroup>\n");

        builder.append("<thead><tr>");
        for (String header : headers) {
            builder.append("<th>")
                    .append(escapeHtml(header))
                    .append("</th>");
        }
        builder.append("</tr></thead>\n");

        builder.append("<tbody>\n");
        for (int rowIndex = 0; rowIndex < detailRows; rowIndex++) {
            builder.append("<tr>");
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                builder.append("<td></td>");
            }
            builder.append("</tr>\n");
        }
        builder.append("</tbody>\n</table>\n");

        return builder.toString();
    }

    private String buildMetadataComment(OverlayProject project) {
        StringBuilder metadata = new StringBuilder();
        metadata.append("\n<!-- ").append(METADATA_BEGIN).append('\n');
        metadata.append("VERSION=1\n");
        metadata.append("PDF_PATH_B64=").append(base64Encode(project.getPdfPath().toAbsolutePath().toString())).append('\n');

        for (PdfPageMetadata pageMetadata : project.getMetadata().getPages()) {
            metadata.append("PAGE=")
                    .append(pageMetadata.pageIndex()).append(',')
                    .append(formatDouble(pageMetadata.widthPoints())).append(',')
                    .append(formatDouble(pageMetadata.heightPoints()))
                    .append('\n');

            OverlayPage overlayPage = project.getOverlayPage(pageMetadata.pageIndex());
            for (OverlayElement element : overlayPage.mutableElements()) {
                metadata.append("ELEMENT=")
                        .append(pageMetadata.pageIndex()).append(',')
                        .append(element.getType().name()).append(',')
                        .append(formatDouble(element.getXRatio())).append(',')
                        .append(formatDouble(element.getYRatio())).append(',')
                        .append(formatDouble(element.getWidthRatio())).append(',')
                        .append(formatDouble(element.getHeightRatio())).append(',')
                        .append(base64Encode(element.getText())).append(',')
                        .append(element.getTableColumnCount()).append(',')
                        .append(element.getTableDataRows()).append(',')
                        .append(base64Encode(element.getTableColumnWidths()))
                        .append('\n');
            }
        }

        metadata.append(METADATA_END).append(" -->\n");
        return metadata.toString();
    }

    private String extractMetadata(String htmlContent) {
        String beginToken = "<!-- " + METADATA_BEGIN;
        String endToken = METADATA_END + " -->";

        int beginIndex = htmlContent.indexOf(beginToken);
        int endIndex = htmlContent.indexOf(endToken);
        if (beginIndex < 0 || endIndex < 0 || endIndex <= beginIndex) {
            throw new IllegalArgumentException(
                    "The HTML does not contain editable metadata. Use an HTML generated by this application."
            );
        }

        int metadataStart = beginIndex + beginToken.length();
        return htmlContent.substring(metadataStart, endIndex).trim();
    }

    private OverlayProject parseProject(String metadataBlock) {
        Path pdfPath = null;
        List<PdfPageMetadata> pagesMetadata = new ArrayList<>();
        Map<Integer, OverlayPage> overlayPages = new LinkedHashMap<>();

        String[] lines = metadataBlock.split("\\R");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isBlank() || line.equals("VERSION=1")) {
                continue;
            }

            if (line.startsWith("PDF_PATH_B64=")) {
                String encodedPath = line.substring("PDF_PATH_B64=".length());
                pdfPath = Path.of(base64Decode(encodedPath));
                continue;
            }

            if (line.startsWith("PAGE=")) {
                String payload = line.substring("PAGE=".length());
                String[] tokens = payload.split(",", 3);
                if (tokens.length != 3) {
                    throw new IllegalArgumentException("Invalid PAGE metadata line: " + line);
                }
                int pageIndex = Integer.parseInt(tokens[0]);
                float widthPoints = Float.parseFloat(tokens[1]);
                float heightPoints = Float.parseFloat(tokens[2]);
                PdfPageMetadata pageMetadata = new PdfPageMetadata(pageIndex, widthPoints, heightPoints);
                pagesMetadata.add(pageMetadata);
                overlayPages.put(pageIndex, new OverlayPage(pageIndex));
                continue;
            }

            if (line.startsWith("ELEMENT=")) {
                String payload = line.substring("ELEMENT=".length());
                String[] tokens = payload.split(",", 10);
                if (tokens.length < 7) {
                    throw new IllegalArgumentException("Invalid ELEMENT metadata line: " + line);
                }
                int pageIndex = Integer.parseInt(tokens[0]);
                OverlayElementType type = OverlayElementType.valueOf(tokens[1]);
                double xRatio = Double.parseDouble(tokens[2]);
                double yRatio = Double.parseDouble(tokens[3]);
                double widthRatio = Double.parseDouble(tokens[4]);
                double heightRatio = Double.parseDouble(tokens[5]);
                String text = base64Decode(tokens[6]);

                int tableColumnCount = tokens.length >= 8 ? Integer.parseInt(tokens[7]) : 0;
                int tableDataRows = tokens.length >= 9 ? Integer.parseInt(tokens[8]) : 1;
                String tableColumnWidths = tokens.length >= 10 ? base64Decode(tokens[9]) : "";

                OverlayPage overlayPage = overlayPages.computeIfAbsent(pageIndex, OverlayPage::new);
                OverlayElement element = new OverlayElement(type, xRatio, yRatio, widthRatio, heightRatio, text);
                element.setTableColumnCount(tableColumnCount);
                if (tableDataRows == 1 || tableDataRows == 4) {
                    element.setTableDataRows(tableDataRows);
                } else {
                    element.setTableDataRows(1);
                }
                element.setTableColumnWidths(tableColumnWidths);
                overlayPage.addElement(element);
            }
        }

        if (pdfPath == null) {
            throw new IllegalArgumentException("Missing PDF path in HTML metadata");
        }
        if (pagesMetadata.isEmpty()) {
            throw new IllegalArgumentException("No page metadata found in HTML file");
        }

        PdfDocumentMetadata documentMetadata = new PdfDocumentMetadata(pdfPath, pagesMetadata);
        OverlayProject project = new OverlayProject(pdfPath, documentMetadata);

        for (Map.Entry<Integer, OverlayPage> entry : overlayPages.entrySet()) {
            OverlayPage targetPage = project.getOverlayPage(entry.getKey());
            for (OverlayElement element : entry.getValue().mutableElements()) {
                targetPage.addElement(element.copy());
            }
        }
        return project;
    }

    private List<Double> parseTableColumnWidths(String rawWidths, int columnCount) {
        String source = rawWidths == null ? "" : rawWidths.strip();
        if (source.isBlank()) {
            source = buildDefaultColumnWidths(columnCount);
        }

        String[] tokens = source.split(",");
        if (tokens.length != columnCount) {
            source = buildDefaultColumnWidths(columnCount);
            tokens = source.split(",");
        }

        List<Double> values = new ArrayList<>();
        double total = 0.0d;
        for (String token : tokens) {
            double width = Double.parseDouble(token.strip());
            if (width <= 0.0d) {
                width = 1.0d;
            }
            values.add(width);
            total += width;
        }
        if (total <= 0.0d) {
            total = columnCount;
        }

        List<Double> normalized = new ArrayList<>();
        for (double value : values) {
            normalized.add((value / total) * 100.0d);
        }
        return normalized;
    }

    private List<String> parseTableHeaders(String rawHeaders, int columnCount) {
        String source = rawHeaders == null ? "" : rawHeaders.strip();
        if (source.isBlank()) {
            source = buildDefaultHeaders(columnCount);
        }

        String[] tokens = source.split("\\|", -1);
        List<String> headers = new ArrayList<>();
        for (int index = 0; index < columnCount; index++) {
            String value = index < tokens.length ? tokens[index].strip() : "";
            headers.add(value.isBlank() ? "Column " + (index + 1) : value);
        }
        return headers;
    }

    private String buildDefaultHeaders(int columnCount) {
        List<String> headers = new ArrayList<>();
        for (int index = 1; index <= columnCount; index++) {
            headers.add("Column " + index);
        }
        return String.join("|", headers);
    }

    private String buildDefaultColumnWidths(int columnCount) {
        List<String> widths = new ArrayList<>();
        double base = 100.0d / columnCount;
        double total = 0.0d;
        for (int index = 0; index < columnCount; index++) {
            double value = index == columnCount - 1 ? 100.0d - total : base;
            total += value;
            widths.add(formatDouble(value));
        }
        return String.join(",", widths);
    }

    private String percent(double ratio) {
        double safeRatio = Math.max(0.0d, Math.min(1.0d, ratio));
        return formatDouble(safeRatio * 100.0d) + "%";
    }

    private String formatDouble(double value) {
        return String.format(Locale.US, "%.6f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private String base64Encode(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String base64Decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return "";
        }
        byte[] rawBytes = Base64.getUrlDecoder().decode(encoded);
        return new String(rawBytes, StandardCharsets.UTF_8);
    }

    private String escapeHtml(String raw) {
        if (raw == null) {
            return "";
        }
        return raw
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
