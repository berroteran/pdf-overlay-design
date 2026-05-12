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
    private DocumentStatus documentStatus;
    private boolean statusWatermarkEnabled;

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
        this.documentStatus = DocumentStatus.DRAFT;
        this.statusWatermarkEnabled = false;
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

    /**
     * @return estado del documento para marca de agua.
     */
    public DocumentStatus getDocumentStatus() {
        return documentStatus;
    }

    /**
     * Actualiza el estado del documento.
     *
     * @param documentStatus estado nuevo.
     */
    public void setDocumentStatus(DocumentStatus documentStatus) {
        this.documentStatus = Objects.requireNonNull(documentStatus, "documentStatus is required");
    }

    /**
     * @return `true` cuando la marca de agua de estado está activa.
     */
    public boolean isStatusWatermarkEnabled() {
        return statusWatermarkEnabled;
    }

    /**
     * Activa o desactiva la marca de agua de estado.
     *
     * @param statusWatermarkEnabled bandera de activación.
     */
    public void setStatusWatermarkEnabled(boolean statusWatermarkEnabled) {
        this.statusWatermarkEnabled = statusWatermarkEnabled;
    }
}
