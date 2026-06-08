package com.example.pdfoverlay.service;

import com.example.pdfoverlay.model.DocumentStatus;
import com.example.pdfoverlay.model.OverlayElement;
import com.example.pdfoverlay.model.OverlayElementType;
import com.example.pdfoverlay.model.OverlayPage;
import com.example.pdfoverlay.model.OverlayProject;
import com.example.pdfoverlay.model.PdfDocumentMetadata;
import com.example.pdfoverlay.model.PdfPageMetadata;
import com.example.pdfoverlay.template.HtmlTemplateRepository;
import com.example.pdfoverlay.template.HtmlTemplateType;

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
    private static final String TEMPLATE_CODE_PREFIX = "HTML_TEMPLATE=";
    private static final String PRINT_STYLE_PLACEHOLDER = "{{ print_style }}";
    private static final String BODY_PLACEHOLDER = "{{ body }}";
    private static final String ROOT_SELECTOR_BODY = "body";
    private static final String ROOT_SELECTOR_FRAGMENT = ".preprinted-page";

    private final PdfService pdfService;
    private final HtmlTemplateRepository htmlTemplateRepository;

    /**
     * Crea el servicio con dependencia al render de PDF.
     *
     * @param pdfService servicio de render PDF.
     */
    public HtmlExportService(PdfService pdfService) {
        this(pdfService, new HtmlTemplateRepository());
    }

    /**
     * Crea el servicio con dependencias explícitas.
     *
     * @param pdfService             servicio de render PDF.
     * @param htmlTemplateRepository repositorio de templates HTML.
     */
    public HtmlExportService(PdfService pdfService, HtmlTemplateRepository htmlTemplateRepository) {
        this.pdfService = Objects.requireNonNull(pdfService, "pdfService is required");
        this.htmlTemplateRepository = Objects.requireNonNull(
                htmlTemplateRepository,
                "htmlTemplateRepository is required"
        );
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

        String html = buildHtmlContent(project, renderDpi, includePdfBackground, exportOptions);

        Files.writeString(htmlPath, html, StandardCharsets.UTF_8);
        LOGGER.log(Level.INFO, "HTML exported: {0}", htmlPath);
        return htmlPath;
    }

    /**
     * Construye el HTML exportable en memoria.
     *
     * @param project              proyecto fuente.
     * @param renderDpi            resolución para render de fondo.
     * @param includePdfBackground true para incrustar fondo PDF.
     * @param exportOptions        opciones visuales de exportación.
     * @return contenido HTML completo.
     * @throws IOException cuando falla la conversión de páginas PDF.
     */
    public String buildHtmlContent(OverlayProject project, float renderDpi,
                                   boolean includePdfBackground, ExportOptions exportOptions) throws IOException {
        Objects.requireNonNull(project, "project is required");
        Objects.requireNonNull(exportOptions, "exportOptions is required");
        if (renderDpi <= 0) {
            throw new IllegalArgumentException("renderDpi must be > 0");
        }

        StringBuilder html = new StringBuilder();
        String printStyle = buildPrintStyle(
                project,
                exportOptions,
                ROOT_SELECTOR_BODY,
                true,
                buildPagePrintCss(project.getMetadata().getPages())
        );
        StringBuilder bodyContent = new StringBuilder();
        String bodyAttributes = buildBodyAttributes(project);

        for (PdfPageMetadata pageMetadata : project.getMetadata().getPages()) {
            int pageIndex = pageMetadata.pageIndex();
            String imageDataUri = "";
            if (includePdfBackground) {
                byte[] pngBytes = pdfService.renderPageToPngBytes(project.getPdfPath(), pageIndex, renderDpi);
                imageDataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes);
            }
            OverlayPage overlayPage = project.getOverlayPage(pageIndex);
            bodyContent.append(buildPageMarkup(pageMetadata, overlayPage, imageDataUri, includePdfBackground));
        }

        html.append(renderUsingTemplate(printStyle, bodyContent.toString(), bodyAttributes));
        appendMetadataBeforeClosingBody(html, buildMetadataComment(project, HtmlTemplateType.ERPNEXT_PRINT_FORMAT));
        return html.toString();
    }

    /**
     * Construye un fragmento HTML sin plantilla ni metadata, listo para incrustar en otro sistema.
     *
     * @param project              proyecto fuente.
     * @param renderDpi            resolución para render de fondo.
     * @param includePdfBackground true para incrustar fondo PDF.
     * @param exportOptions        opciones visuales de exportación.
     * @return fragmento HTML con estilos y body imprimible.
     * @throws IOException cuando falla la conversión de páginas PDF.
     */
    public String buildEmbedHtmlFragment(OverlayProject project, float renderDpi,
                                         boolean includePdfBackground, ExportOptions exportOptions) throws IOException {
        Objects.requireNonNull(project, "project is required");
        Objects.requireNonNull(exportOptions, "exportOptions is required");
        if (renderDpi <= 0) {
            throw new IllegalArgumentException("renderDpi must be > 0");
        }

        String rootClass = buildFragmentRootClass(project);
        String printStyle = buildPrintStyle(project, exportOptions, ROOT_SELECTOR_FRAGMENT, false, "");
        StringBuilder bodyContent = new StringBuilder();

        for (PdfPageMetadata pageMetadata : project.getMetadata().getPages()) {
            int pageIndex = pageMetadata.pageIndex();
            String imageDataUri = "";
            if (includePdfBackground) {
                byte[] pngBytes = pdfService.renderPageToPngBytes(project.getPdfPath(), pageIndex, renderDpi);
                imageDataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes);
            }
            OverlayPage overlayPage = project.getOverlayPage(pageIndex);
            bodyContent.append(buildPageMarkup(pageMetadata, overlayPage, imageDataUri, includePdfBackground));
        }

        return """
                <style>
                %s
                </style>
                <div class="%s">
                %s
                </div>
                """.formatted(printStyle, rootClass, bodyContent);
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

    private String buildPrintStyle(OverlayProject project, ExportOptions options,
                                   String rootSelector, boolean includeBodyPageChrome,
                                   String pagePrintCss) {
        DocumentStatus documentStatus = project.getDocumentStatus();
        boolean statusWatermarkEnabled = project.isStatusWatermarkEnabled();
        String statusText = escapeHtml(documentStatus.getWatermarkText());
        String watermarkCss = statusWatermarkEnabled
                ? """
                        %s.status-draft::before,
                        %s.status-voided::before {
                            position: fixed;
                            top: 50%%;
                            left: 50%%;
                            transform: translate(-50%%, -50%%) rotate(-35deg);
                            font-size: clamp(72px, 16vw, 190px);
                            font-weight: 700;
                            color: rgba(90, 90, 90, 0.18);
                            letter-spacing: 0.08em;
                            pointer-events: none;
                            user-select: none;
                            z-index: 9999;
                            white-space: nowrap;
                        }
                        %s.status-draft::before { content: "%s"; }
                        %s.status-voided::before { content: "ANULADO"; }
                    """.formatted(rootSelector, rootSelector, rootSelector, statusText, rootSelector)
                : "";
        String fontFamily = options.exportFont() ? "font-family: \"Courier New\", Courier, monospace;" : "";
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
        String rootChrome = includeBodyPageChrome
                ? """
                        %s {
                            margin: 0;
                            padding: 12px;
                            background: #f0f2f4;
                            %s
                        }
                    """.formatted(rootSelector, fontFamily)
                : """
                        %s {
                            position: relative;
                            width: fit-content;
                            %s
                        }
                    """.formatted(rootSelector, fontFamily);
        String printRootReset = includeBodyPageChrome
                ? """
                        %s {
                            margin: 0;
                            padding: 0;
                            background: white;
                        }
                    """.formatted(rootSelector)
                : """
                        %s {
                            background: transparent;
                        }
                    """.formatted(rootSelector);

        return """
                * { box-sizing: border-box; }
                %s
                %s
                %s
                %s {
                    position: relative;
                    min-height: 100vh;
                }
                %s .preprinted-sheet {
                    position: relative;
                    page-break-after: always;
                    break-after: page;
                    margin: 0 0 12px 0;
                    padding: 0;
                    border: none;
                    background-repeat: no-repeat;
                    background-size: 100%% 100%%;
                    background-position: top left;
                    overflow: hidden;
                }
                %s .overlay-item {
                    position: absolute;
                    margin: 0;
                    padding: 0;
                    line-height: 1;
                    white-space: pre-wrap;
                }
                %s .overlay-text {
                    %s
                    background: transparent;
                    color: #0a0d11;
                    %s
                }
                %s .overlay-label {
                    color: #0a0d11;
                    %s
                    white-space: nowrap;
                }
                %s .overlay-button {
                    border: 1px solid #3d4d5d;
                    background: #e7edf4;
                    color: #0a0d11;
                    %s
                    text-align: center;
                }
                %s .overlay-marker {
                    border-radius: 999px;
                    border: 1px solid #8d0000;
                    background: #e00000;
                }
                %s table.overlay-table {
                    position: absolute;
                    table-layout: fixed;
                    border-collapse: collapse;
                    border-spacing: 0;
                    background: transparent;
                }
                %s table.overlay-table > colgroup > col {
                    border: none;
                }
                %s table.overlay-table > thead > tr > th {
                    %s
                    %s
                    color: #0a0d11;
                    %s
                    font-weight: 700;
                    padding: 2px 4px;
                    line-height: 1;
                    text-align: left;
                    vertical-align: top;
                }
                %s table.overlay-table > tbody > tr > td {
                    %s
                    %s
                    color: #0a0d11;
                    %s
                    padding: 2px 4px;
                    line-height: 1;
                    vertical-align: top;
                }
                @media print {
                    %s
                    %s .preprinted-sheet {
                        margin: 0;
                        border: none;
                    }
                }
                """.formatted(
                rootChrome,
                watermarkCss,
                pagePrintCss,
                rootSelector,
                rootSelector,
                rootSelector,
                rootSelector,
                textFieldBorder,
                fontSize10,
                rootSelector,
                fontSize10,
                rootSelector,
                fontSize9,
                rootSelector,
                fontSize9,
                rootSelector,
                rootSelector,
                rootSelector,
                tableHeaderBorder,
                tableHeaderBackground,
                fontSize10,
                rootSelector,
                tableCellBorder,
                tableCellBackground,
                fontSize10,
                printRootReset,
                rootSelector
        );
    }

    private String buildPageMarkup(PdfPageMetadata pageMetadata, OverlayPage overlayPage,
                                   String imageDataUri, boolean includePdfBackground) {
        String widthMillimeters = formatDouble(pageMetadata.widthMillimeters());
        String heightMillimeters = formatDouble(pageMetadata.heightMillimeters());
        String pageClass = "page-" + (pageMetadata.pageIndex() + 1);
        StringBuilder builder = new StringBuilder();

        builder.append("""
                <div class="preprinted-sheet %s" style="%s">
                """.formatted(pageClass, """
                width:%smm;
                height:%smm;
                %s
                """.formatted(
                widthMillimeters,
                heightMillimeters,
                includePdfBackground ? "background-image:url('" + escapeHtml(imageDataUri) + "');" : "background-image:none;"
        ).replace("\n", "")));

        for (OverlayElement element : overlayPage.mutableElements()) {
            builder.append(buildElementMarkup(element, pageMetadata));
        }
        builder.append("""
                </div>
                """);
        return builder.toString();
    }

    private String buildElementMarkup(OverlayElement element, PdfPageMetadata pageMetadata) {
        String left = millimeters(element.getXRatio(), pageMetadata.widthMillimeters());
        String top = millimeters(element.getYRatio(), pageMetadata.heightMillimeters());
        String width = millimeters(element.getWidthRatio(), pageMetadata.widthMillimeters());
        String height = millimeters(element.getHeightRatio(), pageMetadata.heightMillimeters());
        String text = escapeHtml(element.getText());
        String elementId = escapeHtml(element.getId());

        String commonStyle = "left:%smm;top:%smm;width:%smm;height:%smm;".formatted(left, top, width, height);

        return switch (element.getType()) {
            case TEXT_FIELD -> """
                    <div id="%s" class="overlay-item overlay-text" style="%s">%s</div>
                    """.formatted(elementId, commonStyle, text);
            case LABEL -> """
                    <div id="%s" class="overlay-item overlay-label" style="%s">%s</div>
                    """.formatted(elementId, commonStyle, text);
            case BUTTON -> """
                    <div id="%s" class="overlay-item overlay-button" style="%s">%s</div>
                    """.formatted(elementId, commonStyle, text.isBlank() ? "Button" : text);
            case MARKER -> """
                    <div id="%s" class="overlay-item overlay-marker" style="%s"></div>
                    """.formatted(elementId, commonStyle);
            case TABLE -> buildTableElementMarkup(element, commonStyle, elementId, pageMetadata.widthMillimeters());
        };
    }

    private String buildTableElementMarkup(OverlayElement element, String commonStyle,
                                           String elementId, double pageWidthMillimeters) {
        int columnCount = Math.max(1, element.getTableColumnCount());
        int detailRows = element.getTableDataRows();
        double tableWidthMillimeters = Math.max(1.0d, element.getWidthRatio() * pageWidthMillimeters);
        List<Double> columnWidths = parseTableColumnWidths(
                element.getTableColumnWidths(),
                columnCount,
                tableWidthMillimeters
        );
        List<String> headers = parseTableHeaders(element.getText(), columnCount);

        StringBuilder builder = new StringBuilder();
        builder.append("<table id=\"")
                .append(elementId)
                .append("\" class=\"overlay-item overlay-table\" style=\"")
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

    private String buildMetadataComment(OverlayProject project, HtmlTemplateType templateType) {
        StringBuilder metadata = new StringBuilder();
        metadata.append("\n<!-- ").append(METADATA_BEGIN).append('\n');
        metadata.append("VERSION=1\n");
        metadata.append(TEMPLATE_CODE_PREFIX).append(templateType.getCode()).append('\n');
        metadata.append("DOC_STATUS_ENABLED=").append(project.isStatusWatermarkEnabled()).append('\n');
        if (project.isStatusWatermarkEnabled()) {
            metadata.append("DOC_STATUS=").append(project.getDocumentStatus().name()).append('\n');
        }
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
                        .append(base64Encode(element.getTableColumnWidths())).append(',')
                        .append(base64Encode(element.getId()))
                        .append('\n');
            }
        }

        metadata.append(METADATA_END).append(" -->\n");
        return metadata.toString();
    }

    private String renderUsingTemplate(String printStyle, String bodyContent, String bodyAttributes) throws IOException {
        String rendered = htmlTemplateRepository.render(
                HtmlTemplateType.ERPNEXT_PRINT_FORMAT,
                Map.of(
                        PRINT_STYLE_PLACEHOLDER, printStyle,
                        BODY_PLACEHOLDER, bodyContent
                )
        );
        if (bodyAttributes.isBlank()) {
            return rendered;
        }
        return rendered.replaceFirst("<body>", "<body" + bodyAttributes + ">");
    }

    private String buildBodyAttributes(OverlayProject project) {
        if (!project.isStatusWatermarkEnabled()) {
            return "";
        }
        return " class=\"" + project.getDocumentStatus().getBodyClass() + "\"";
    }

    private String buildFragmentRootClass(OverlayProject project) {
        if (!project.isStatusWatermarkEnabled()) {
            return "preprinted-page";
        }
        return "preprinted-page " + project.getDocumentStatus().getBodyClass();
    }

    private void appendMetadataBeforeClosingBody(StringBuilder html, String metadataComment) {
        int bodyCloseIndex = html.lastIndexOf("</body>");
        if (bodyCloseIndex < 0) {
            html.append(metadataComment);
            return;
        }
        html.insert(bodyCloseIndex, metadataComment);
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
        DocumentStatus documentStatus = DocumentStatus.DRAFT;
        boolean statusWatermarkEnabled = false;
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

            if (line.startsWith(TEMPLATE_CODE_PREFIX)) {
                HtmlTemplateType.fromCode(line.substring(TEMPLATE_CODE_PREFIX.length()));
                continue;
            }

            if (line.startsWith("DOC_STATUS=")) {
                String statusValue = line.substring("DOC_STATUS=".length());
                documentStatus = DocumentStatus.fromMetadataValue(statusValue);
                statusWatermarkEnabled = true;
                continue;
            }

            if (line.startsWith("DOC_STATUS_ENABLED=")) {
                String enabledValue = line.substring("DOC_STATUS_ENABLED=".length()).strip();
                statusWatermarkEnabled = Boolean.parseBoolean(enabledValue);
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
                String[] tokens = payload.split(",", 11);
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
                String elementId = tokens.length >= 11 ? base64Decode(tokens[10]) : null;

                OverlayPage overlayPage = overlayPages.computeIfAbsent(pageIndex, OverlayPage::new);
                OverlayElement element = new OverlayElement(type, xRatio, yRatio, widthRatio, heightRatio, text);
                if (elementId != null && !elementId.isBlank()) {
                    element.setId(elementId);
                }
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
        project.setDocumentStatus(documentStatus);
        project.setStatusWatermarkEnabled(statusWatermarkEnabled);

        for (Map.Entry<Integer, OverlayPage> entry : overlayPages.entrySet()) {
            OverlayPage targetPage = project.getOverlayPage(entry.getKey());
            for (OverlayElement element : entry.getValue().mutableElements()) {
                targetPage.addElement(element.copy());
            }
        }
        return project;
    }

    private String buildPagePrintCss(List<PdfPageMetadata> pagesMetadata) {
        if (pagesMetadata.isEmpty()) {
            return "";
        }

        PdfPageMetadata firstPage = pagesMetadata.getFirst();
        return """
                @media print {
                    @page {
                        size: %smm %smm;
                        margin: 0;
                    }
                }
                """.formatted(
                formatDouble(firstPage.widthMillimeters()),
                formatDouble(firstPage.heightMillimeters())
        );
    }

    private List<Double> parseTableColumnWidths(String rawWidths, int columnCount, double tableWidthMillimeters) {
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
            normalized.add((value / total) * tableWidthMillimeters);
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

    private String millimeters(double ratio, double totalMillimeters) {
        double safeRatio = Math.max(0.0d, Math.min(1.0d, ratio));
        return formatDouble(safeRatio * totalMillimeters);
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
