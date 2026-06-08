package com.example.pdfoverlay.service;

import com.example.pdfoverlay.model.DocumentStatus;
import com.example.pdfoverlay.model.OverlayElement;
import com.example.pdfoverlay.model.OverlayElementType;
import com.example.pdfoverlay.model.OverlayProject;
import com.example.pdfoverlay.model.PdfDocumentMetadata;
import com.example.pdfoverlay.model.PdfPageMetadata;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del servicio de exportación/carga HTML.
 */
class HtmlExportServiceTest {

    /**
     * Verifica que el estado del documento se persiste y se recupera desde metadata HTML.
     */
    @Test
    void shouldPersistDocumentStatusInHtmlMetadata() throws Exception {
        HtmlExportService service = new HtmlExportService(new PdfService());
        OverlayProject project = createSinglePageProject();
        project.setDocumentStatus(DocumentStatus.VOIDED);
        project.setStatusWatermarkEnabled(true);

        String htmlContent = service.buildHtmlContent(project, 300, false, ExportOptions.defaultOptions());
        assertFalse(htmlContent.contains("{{ include_style('print.bundle.css') }}"));
        assertTrue(htmlContent.contains("<div class=\"print-format\">"));
        assertPrintStyleInsidePrintFormat(htmlContent);
        assertTrue(htmlContent.contains("DOC_STATUS_ENABLED=true"));
        assertTrue(htmlContent.contains("HTML_TEMPLATE=erpnext-print-format"));
        assertTrue(htmlContent.contains("DOC_STATUS=VOIDED"));
        assertTrue(htmlContent.contains("body.status-voided::before"));
        assertTrue(htmlContent.contains("<body class=\"status-voided\">"));
        assertTrue(htmlContent.contains("width:215.9mm"));

        Path tempFile = Files.createTempFile("overlay-status-", ".html");
        Files.writeString(tempFile, htmlContent);
        OverlayProject loaded = service.loadProjectFromHtml(tempFile);

        assertEquals(DocumentStatus.VOIDED, loaded.getDocumentStatus());
        assertTrue(loaded.isStatusWatermarkEnabled());
    }

    /**
     * Verifica que no se exporta marca de agua cuando está desactivada.
     */
    @Test
    void shouldSkipStatusWatermarkWhenDisabled() throws Exception {
        HtmlExportService service = new HtmlExportService(new PdfService());
        OverlayProject project = createSinglePageProject();
        project.setStatusWatermarkEnabled(false);

        String htmlContent = service.buildHtmlContent(project, 300, false, ExportOptions.defaultOptions());

        assertTrue(htmlContent.contains("DOC_STATUS_ENABLED=false"));
        assertTrue(htmlContent.contains("HTML_TEMPLATE=erpnext-print-format"));
        assertFalse(htmlContent.contains("DOC_STATUS=VOIDED"));
        assertFalse(htmlContent.contains("status-draft::before"));
        assertTrue(htmlContent.contains("<div class=\"print-format-gutter\">"));
        assertTrue(htmlContent.contains("@page"));
    }

    /**
     * Verifica que el fragmento embebible no incluya plantilla ni metadata del proyecto.
     */
    @Test
    void shouldBuildEmbedFragmentWithoutTemplateOrMetadata() throws Exception {
        HtmlExportService service = new HtmlExportService(new PdfService());
        OverlayProject project = createSinglePageProject();
        project.getOverlayPage(0).addElement(new OverlayElement(OverlayElementType.LABEL, 0.1, 0.1, 0.2, 0.05, "Customer"));

        String fragment = service.buildEmbedHtmlFragment(project, 300, false, ExportOptions.defaultOptions());

        assertTrue(fragment.stripLeading().startsWith("<style>"));
        assertTrue(fragment.contains("<style>"));
        assertTrue(fragment.contains(".preprinted-page .preprinted-sheet"));
        assertTrue(fragment.contains("Customer"));
        assertTrue(fragment.contains("<div class=\"preprinted-page\">"));
        assertTrue(fragment.contains("left:21.59mm"));
        assertTrue(fragment.contains("width:43.18mm"));
        assertTrue(fragment.contains("<div id=\""));
        assertFalse(fragment.contains("{{ include_style('print.bundle.css') }}"));
        assertFalse(fragment.contains("PDF_OVERLAY_METADATA_BEGIN"));
        assertFalse(fragment.contains("<html>"));
        assertFalse(fragment.contains("<body>"));
        assertFalse(fragment.contains("<style media=\"print\">"));
    }

    /**
     * Verifica que las tablas exporten anchos directos en milímetros.
     */
    @Test
    void shouldExportTableColumnWidthsInMillimeters() throws Exception {
        HtmlExportService service = new HtmlExportService(new PdfService());
        OverlayProject project = createSinglePageProject();
        OverlayElement table = new OverlayElement(
                OverlayElementType.TABLE,
                0.1,
                0.1,
                0.5,
                0.2,
                "A|B|C"
        );
        table.setTableColumnCount(3);
        table.setTableColumnWidths("30,40,37.95");
        project.getOverlayPage(0).addElement(table);

        String fragment = service.buildEmbedHtmlFragment(project, 300, false, ExportOptions.defaultOptions());

        assertTrue(fragment.contains("<!-- BEGIN TABLE: " + table.getId() + " -->"));
        assertTrue(fragment.contains("<!-- END TABLE: " + table.getId() + " -->"));
        assertTrue(fragment.contains("<col style=\"width:30mm;\">"));
        assertTrue(fragment.contains("<col style=\"width:40mm;\">"));
        assertTrue(fragment.contains("<col style=\"width:37.95mm;\">"));
        assertFalse(fragment.contains("<col style=\"width:30%;\">"));
    }

    /**
     * Verifica que el orden del HTML exportado siga la posición visual desde 0,0.
     */
    @Test
    void shouldExportElementsOrderedByTopThenLeft() throws Exception {
        HtmlExportService service = new HtmlExportService(new PdfService());
        OverlayProject project = createSinglePageProject();
        OverlayElement lower = new OverlayElement("lower", OverlayElementType.LABEL, 0.30, 0.40, 0.1, 0.03, "lower");
        OverlayElement upperRight = new OverlayElement("upperRight", OverlayElementType.LABEL, 0.50, 0.10, 0.1, 0.03, "upperRight");
        OverlayElement upperLeft = new OverlayElement("upperLeft", OverlayElementType.LABEL, 0.10, 0.10, 0.1, 0.03, "upperLeft");
        project.getOverlayPage(0).addElement(lower);
        project.getOverlayPage(0).addElement(upperRight);
        project.getOverlayPage(0).addElement(upperLeft);

        String fragment = service.buildEmbedHtmlFragment(project, 300, false, ExportOptions.defaultOptions());

        int upperLeftIndex = fragment.indexOf("id=\"upperLeft\"");
        int upperRightIndex = fragment.indexOf("id=\"upperRight\"");
        int lowerIndex = fragment.indexOf("id=\"lower\"");

        assertTrue(upperLeftIndex >= 0);
        assertTrue(upperRightIndex > upperLeftIndex);
        assertTrue(lowerIndex > upperRightIndex);
    }

    /**
     * Verifica que el HTML standalone mantiene documento completo sin metadata editable.
     */
    @Test
    void shouldBuildStandaloneBrowserHtmlWithTemplateButWithoutProjectMetadata() throws Exception {
        HtmlExportService service = new HtmlExportService(new PdfService());
        OverlayProject project = createSinglePageProject();
        project.getOverlayPage(0).addElement(new OverlayElement(OverlayElementType.TEXT_FIELD, 0.5, 0.2, 0.1, 0.04, "textbox1"));

        String htmlContent = service.buildStandaloneBrowserHtml(project, 300, false, ExportOptions.defaultOptions());

        assertTrue(htmlContent.contains("<html>"));
        assertTrue(htmlContent.contains("<head>"));
        assertTrue(htmlContent.contains("<body>"));
        assertTrue(htmlContent.contains("<div class=\"print-format\">"));
        assertPrintStyleInsidePrintFormat(htmlContent);
        assertTrue(htmlContent.contains("textbox1"));
        assertTrue(htmlContent.contains("background-image:none;"));
        assertFalse(htmlContent.contains("<div class=\"action-banner print-hide\">"));
        assertFalse(htmlContent.contains(">...</div>"));
        assertFalse(htmlContent.contains("{{ include_style('print.bundle.css') }}"));
        assertFalse(htmlContent.contains("PDF_OVERLAY_METADATA_BEGIN"));
        assertFalse(htmlContent.contains("data:image/png;base64"));
    }

    private OverlayProject createSinglePageProject() {
        Path pdfPath = Path.of("sample.pdf");
        return new OverlayProject(
                pdfPath,
                new PdfDocumentMetadata(pdfPath, List.of(new PdfPageMetadata(0, 612, 792)))
        );
    }

    private String extractHead(String htmlContent) {
        int headStart = htmlContent.indexOf("<head>");
        int headEnd = htmlContent.indexOf("</head>");
        if (headStart < 0 || headEnd < 0 || headEnd <= headStart) {
            return "";
        }
        return htmlContent.substring(headStart, headEnd);
    }

    private void assertPrintStyleInsidePrintFormat(String htmlContent) {
        assertFalse(extractHead(htmlContent).contains("<style>"));
        int printFormatIndex = htmlContent.indexOf("<div class=\"print-format\">");
        int styleIndex = htmlContent.indexOf("<style>", printFormatIndex);
        int bodyIndex = htmlContent.indexOf("<div class=\"preprinted-sheet", printFormatIndex);
        assertTrue(printFormatIndex >= 0);
        assertTrue(styleIndex > printFormatIndex);
        assertTrue(bodyIndex > styleIndex);
    }
}
