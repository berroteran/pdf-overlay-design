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
        OverlayProject project = new OverlayProject(
                Path.of("sample.pdf"),
                new PdfDocumentMetadata(Path.of("sample.pdf"), List.of(new PdfPageMetadata(0, 612, 792)))
        );
        project.setDocumentStatus(DocumentStatus.VOIDED);
        project.setStatusWatermarkEnabled(true);

        String htmlContent = service.buildHtmlContent(project, 300, false, ExportOptions.defaultOptions());
        assertTrue(htmlContent.contains("{{ include_style('print.bundle.css') }}"));
        assertTrue(htmlContent.contains("<div class=\"print-format\">"));
        assertTrue(htmlContent.contains("DOC_STATUS_ENABLED=true"));
        assertTrue(htmlContent.contains("HTML_TEMPLATE=erpnext-print-format"));
        assertTrue(htmlContent.contains("DOC_STATUS=VOIDED"));
        assertTrue(htmlContent.contains("body.status-voided::before"));
        assertTrue(htmlContent.contains("<body class=\"status-voided\">"));

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
        OverlayProject project = new OverlayProject(
                Path.of("sample.pdf"),
                new PdfDocumentMetadata(Path.of("sample.pdf"), List.of(new PdfPageMetadata(0, 612, 792)))
        );
        project.setStatusWatermarkEnabled(false);

        String htmlContent = service.buildHtmlContent(project, 300, false, ExportOptions.defaultOptions());

        assertTrue(htmlContent.contains("DOC_STATUS_ENABLED=false"));
        assertTrue(htmlContent.contains("HTML_TEMPLATE=erpnext-print-format"));
        assertFalse(htmlContent.contains("DOC_STATUS=VOIDED"));
        assertFalse(htmlContent.contains("status-draft::before"));
        assertTrue(htmlContent.contains("<div class=\"print-format-gutter\">"));
    }

    /**
     * Verifica que el fragmento embebible no incluya plantilla ni metadata del proyecto.
     */
    @Test
    void shouldBuildEmbedFragmentWithoutTemplateOrMetadata() throws Exception {
        HtmlExportService service = new HtmlExportService(new PdfService());
        OverlayProject project = new OverlayProject(
                Path.of("sample.pdf"),
                new PdfDocumentMetadata(Path.of("sample.pdf"), List.of(new PdfPageMetadata(0, 612, 792)))
        );
        project.getOverlayPage(0).addElement(new OverlayElement(OverlayElementType.LABEL, 0.1, 0.1, 0.2, 0.05, "Customer"));

        String fragment = service.buildEmbedHtmlFragment(project, 300, false, ExportOptions.defaultOptions());

        assertTrue(fragment.contains("<style>"));
        assertTrue(fragment.contains(".preprinted-page table.print-page"));
        assertTrue(fragment.contains("Customer"));
        assertTrue(fragment.contains("<div class=\"preprinted-page\">"));
        assertFalse(fragment.contains("{{ include_style('print.bundle.css') }}"));
        assertFalse(fragment.contains("PDF_OVERLAY_METADATA_BEGIN"));
        assertFalse(fragment.contains("<html>"));
        assertFalse(fragment.contains("<body>"));
        assertFalse(fragment.contains("<style media=\"print\">"));
    }
}
