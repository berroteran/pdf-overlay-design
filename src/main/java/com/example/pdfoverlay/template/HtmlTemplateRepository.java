package com.example.pdfoverlay.template;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * Repositorio de templates HTML almacenadas en recursos.
 */
public final class HtmlTemplateRepository {

    public String load(HtmlTemplateType templateType) throws IOException {
        Objects.requireNonNull(templateType, "templateType is required");
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(templateType.getResourcePath())) {
            if (inputStream == null) {
                throw new IOException("HTML template not found: " + templateType.getResourcePath());
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public String render(HtmlTemplateType templateType, Map<String, String> replacements) throws IOException {
        Objects.requireNonNull(replacements, "replacements is required");
        String template = load(templateType);
        String rendered = template;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            rendered = rendered.replace(entry.getKey(), entry.getValue());
        }
        return rendered;
    }
}
