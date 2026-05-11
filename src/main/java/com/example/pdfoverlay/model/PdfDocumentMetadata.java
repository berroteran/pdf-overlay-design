package com.example.pdfoverlay.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Metadatos estructurales del PDF cargado.
 */
public final class PdfDocumentMetadata {
    private final Path sourcePath;
    private final List<PdfPageMetadata> pages;

    /**
     * Crea una instancia validando datos mínimos del documento.
     *
     * @param sourcePath ruta del PDF fuente.
     * @param pages      lista de metadatos por página.
     */
    public PdfDocumentMetadata(Path sourcePath, List<PdfPageMetadata> pages) {
        this.sourcePath = Objects.requireNonNull(sourcePath, "sourcePath is required");
        this.pages = List.copyOf(Objects.requireNonNull(pages, "pages is required"));
        if (this.pages.isEmpty()) {
            throw new IllegalArgumentException("PDF must contain at least one page");
        }
    }

    /**
     * @return ruta del PDF fuente.
     */
    public Path getSourcePath() {
        return sourcePath;
    }

    /**
     * @return lista inmutable de páginas.
     */
    public List<PdfPageMetadata> getPages() {
        return pages;
    }

    /**
     * @return cantidad total de páginas.
     */
    public int pageCount() {
        return pages.size();
    }

    /**
     * Obtiene metadatos de una página específica.
     *
     * @param pageIndex índice base 0.
     * @return metadatos de la página solicitada.
     */
    public PdfPageMetadata getPage(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= pages.size()) {
            throw new IllegalArgumentException("Invalid page index: " + pageIndex);
        }
        return pages.get(pageIndex);
    }
}

