package com.example.pdfoverlay.model;

import java.util.Locale;

/**
 * Estados de documento para generar marca de agua en exportación HTML.
 */
public enum DocumentStatus {
    DRAFT("BORRADOR", "draft"),
    VOIDED("ANULADO", "voided");

    private final String watermarkText;
    private final String cssSuffix;

    DocumentStatus(String watermarkText, String cssSuffix) {
        this.watermarkText = watermarkText;
        this.cssSuffix = cssSuffix;
    }

    /**
     * @return texto visible de la marca de agua.
     */
    public String getWatermarkText() {
        return watermarkText;
    }

    /**
     * @return clase CSS aplicada al `body`.
     */
    public String getBodyClass() {
        return "status-" + cssSuffix;
    }

    /**
     * Convierte un valor textual de metadata en estado de documento.
     *
     * @param rawValue valor leído de metadata.
     * @return estado válido; `DRAFT` cuando no se reconoce.
     */
    public static DocumentStatus fromMetadataValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return DRAFT;
        }
        String normalized = rawValue.strip().toUpperCase(Locale.ROOT);
        try {
            return DocumentStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return DRAFT;
        }
    }

    @Override
    public String toString() {
        return watermarkText;
    }
}
