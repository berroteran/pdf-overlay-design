package com.example.pdfoverlay.service;

/**
 * Opciones de exportación para personalizar el HTML generado.
 *
 * @param exportFont         exporta estilos tipográficos.
 * @param exportTableColors  exporta colores de tablas.
 * @param exportTableBorders exporta bordes de tablas.
 * @param exportTextBorders  exporta bordes de campos de texto.
 */
public record ExportOptions(
        boolean exportFont,
        boolean exportTableColors,
        boolean exportTableBorders,
        boolean exportTextBorders
) {

    /**
     * Opciones recomendadas por defecto.
     *
     * @return configuración con bordes útiles para impresión y sin colores.
     */
    public static ExportOptions defaultOptions() {
        return new ExportOptions(true, false, true, true);
    }
}
