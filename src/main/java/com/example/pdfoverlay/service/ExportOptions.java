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
     * @return configuración con todas las opciones activas.
     */
    public static ExportOptions defaultOptions() {
        return new ExportOptions(true, true, true, true);
    }
}

