package com.example.pdfoverlay.model;

/**
 * Metadatos geométricos de una página PDF.
 *
 * @param pageIndex    índice de página (base 0).
 * @param widthPoints  ancho en puntos PDF.
 * @param heightPoints alto en puntos PDF.
 */
public record PdfPageMetadata(int pageIndex, float widthPoints, float heightPoints) {

    /**
     * Obtiene el ancho físico de la página en pulgadas.
     *
     * @return ancho en pulgadas.
     */
    public double widthInches() {
        return widthPoints / 72.0d;
    }

    /**
     * Obtiene el alto físico de la página en pulgadas.
     *
     * @return alto en pulgadas.
     */
    public double heightInches() {
        return heightPoints / 72.0d;
    }
}

