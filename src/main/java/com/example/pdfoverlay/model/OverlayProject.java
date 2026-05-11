package com.example.pdfoverlay.model;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Modelo raíz del proyecto en edición.
 */
public final class OverlayProject {
    private final Path pdfPath;
    private final PdfDocumentMetadata metadata;
    private final Map<Integer, OverlayPage> pages;

    /**
     * Construye un proyecto inicial creando una capa por página PDF.
     *
     * @param pdfPath  ruta del PDF fuente.
     * @param metadata metadatos del PDF.
     */
    public OverlayProject(Path pdfPath, PdfDocumentMetadata metadata) {
        this.pdfPath = Objects.requireNonNull(pdfPath, "pdfPath is required");
        this.metadata = Objects.requireNonNull(metadata, "metadata is required");
        this.pages = new LinkedHashMap<>();
        for (PdfPageMetadata page : metadata.getPages()) {
            pages.put(page.pageIndex(), new OverlayPage(page.pageIndex()));
        }
    }

    /**
     * @return ruta del PDF fuente.
     */
    public Path getPdfPath() {
        return pdfPath;
    }

    /**
     * @return metadatos de páginas PDF.
     */
    public PdfDocumentMetadata getMetadata() {
        return metadata;
    }

    /**
     * Obtiene la capa editable de una página.
     *
     * @param pageIndex índice base 0.
     * @return página de overlay.
     */
    public OverlayPage getOverlayPage(int pageIndex) {
        OverlayPage page = pages.get(pageIndex);
        if (page == null) {
            throw new IllegalArgumentException("Unknown page index: " + pageIndex);
        }
        return page;
    }
}

