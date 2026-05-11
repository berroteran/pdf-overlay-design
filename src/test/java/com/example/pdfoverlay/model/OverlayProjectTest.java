package com.example.pdfoverlay.model;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas unitarias del modelo raíz del proyecto.
 */
class OverlayProjectTest {

    /**
     * Verifica la inicialización de páginas a partir de metadatos PDF.
     */
    @Test
    void shouldInitializeOneOverlayPagePerPdfPage() {
        PdfDocumentMetadata metadata = new PdfDocumentMetadata(
                Path.of("sample.pdf"),
                List.of(
                        new PdfPageMetadata(0, 612, 792),
                        new PdfPageMetadata(1, 612, 792)
                )
        );

        OverlayProject project = new OverlayProject(Path.of("sample.pdf"), metadata);

        assertEquals(2, project.getMetadata().pageCount());
        assertEquals(0, project.getOverlayPage(0).getPageIndex());
        assertEquals(1, project.getOverlayPage(1).getPageIndex());
    }
}
