package com.example.pdfoverlay.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pruebas unitarias del modelo de elementos overlay.
 */
class OverlayElementTest {

    /**
     * Verifica normalización básica de ratios y texto.
     */
    @Test
    void shouldClampRatiosAndTrimText() {
        OverlayElement element = new OverlayElement(
                OverlayElementType.TEXT_FIELD,
                -1.0,
                2.0,
                1.2,
                -0.4,
                "  Customer Name  "
        );

        assertEquals(0.0, element.getXRatio());
        assertEquals(1.0, element.getYRatio());
        assertEquals(1.0, element.getWidthRatio());
        assertEquals(0.0, element.getHeightRatio());
        assertEquals("Customer Name", element.getText());
    }

    /**
     * Valida que los valores no finitos se rechazan explícitamente.
     */
    @Test
    void shouldRejectNonFiniteRatios() {
        assertThrows(IllegalArgumentException.class,
                () -> new OverlayElement(OverlayElementType.LABEL, Double.NaN, 0.2, 0.2, 0.1, "x"));
    }

    /**
     * Verifica configuración específica de tablas y validaciones asociadas.
     */
    @Test
    void shouldHandleTableConfiguration() {
        OverlayElement tableElement = new OverlayElement(
                OverlayElementType.TABLE,
                0.1,
                0.2,
                0.5,
                0.3,
                "ColA|ColB|ColC"
        );
        tableElement.setTableColumnCount(3);
        tableElement.setTableDataRows(4);
        tableElement.setTableColumnWidths("20,30,40");

        OverlayElement copy = tableElement.copy();

        assertEquals(3, copy.getTableColumnCount());
        assertEquals(4, copy.getTableDataRows());
        assertEquals("20,30,40", copy.getTableColumnWidths());
    }

    /**
     * Valida que las filas permitidas para tabla sean únicamente 1 o 4.
     */
    @Test
    void shouldRejectInvalidTableRowCount() {
        OverlayElement tableElement = new OverlayElement(
                OverlayElementType.TABLE,
                0.1,
                0.2,
                0.5,
                0.3,
                "ColA|ColB"
        );
        assertThrows(IllegalArgumentException.class, () -> tableElement.setTableDataRows(2));
    }

    /**
     * Verifica edición de ID y validación de valor vacío.
     */
    @Test
    void shouldAllowUpdatingIdAndRejectBlankValues() {
        OverlayElement element = new OverlayElement(
                OverlayElementType.TEXT_FIELD,
                0.1,
                0.2,
                0.3,
                0.1,
                "x"
        );
        element.setId("textbox12");
        assertEquals("textbox12", element.getId());
        assertEquals("textbox12", element.copy().getId());
        assertThrows(IllegalArgumentException.class, () -> element.setId("   "));
    }
}
