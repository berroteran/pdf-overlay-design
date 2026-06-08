package com.example.pdfoverlay.template;

/**
 * Templates HTML soportadas por la aplicación.
 */
public enum HtmlTemplateType {
    ERPNEXT_PRINT_FORMAT("erpnext-print-format", "templates/erpnext/print-format.html");

    private final String code;
    private final String resourcePath;

    HtmlTemplateType(String code, String resourcePath) {
        this.code = code;
        this.resourcePath = resourcePath;
    }

    public String getCode() {
        return code;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public static HtmlTemplateType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return ERPNEXT_PRINT_FORMAT;
        }
        for (HtmlTemplateType value : values()) {
            if (value.code.equalsIgnoreCase(code.strip())) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unsupported HTML template: " + code);
    }
}
