package com.example.pdfoverlay.model;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Elemento editable de la capa de overlay.
 */
public final class OverlayElement {
    private static final AtomicInteger DEFAULT_ID_SEQUENCE = new AtomicInteger(0);

    private String id;
    private final OverlayElementType type;
    private double xRatio;
    private double yRatio;
    private double widthRatio;
    private double heightRatio;
    private String text;
    private int tableColumnCount;
    private int tableDataRows;
    private String tableColumnWidths;

    /**
     * Crea un elemento de overlay con identificador único.
     *
     * @param type        tipo del elemento.
     * @param xRatio      posición X relativa [0..1].
     * @param yRatio      posición Y relativa [0..1].
     * @param widthRatio  ancho relativo [0..1].
     * @param heightRatio alto relativo [0..1].
     * @param text        contenido textual opcional.
     */
    public OverlayElement(OverlayElementType type, double xRatio, double yRatio,
                          double widthRatio, double heightRatio, String text) {
        this("element" + DEFAULT_ID_SEQUENCE.incrementAndGet(), type, xRatio, yRatio, widthRatio, heightRatio, text);
    }

    /**
     * Crea un elemento de overlay con identificador controlado.
     *
     * @param id          identificador del elemento.
     * @param type        tipo del elemento.
     * @param xRatio      posición X relativa [0..1].
     * @param yRatio      posición Y relativa [0..1].
     * @param widthRatio  ancho relativo [0..1].
     * @param heightRatio alto relativo [0..1].
     * @param text        contenido textual opcional.
     */
    public OverlayElement(String id, OverlayElementType type, double xRatio, double yRatio,
                          double widthRatio, double heightRatio, String text) {
        setId(id);
        this.type = Objects.requireNonNull(type, "type is required");
        setXRatio(xRatio);
        setYRatio(yRatio);
        setWidthRatio(widthRatio);
        setHeightRatio(heightRatio);
        setText(text);
        setTableColumnCount(0);
        setTableDataRows(1);
        setTableColumnWidths("");
    }

    /**
     * @return identificador del elemento.
     */
    public String getId() {
        return id;
    }

    /**
     * Actualiza el identificador del elemento.
     *
     * @param id nuevo identificador.
     */
    public void setId(String id) {
        String normalized = Objects.requireNonNull(id, "id is required").strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        this.id = normalized;
    }

    /**
     * @return tipo del elemento.
     */
    public OverlayElementType getType() {
        return type;
    }

    /**
     * @return posición X relativa.
     */
    public double getXRatio() {
        return xRatio;
    }

    /**
     * Actualiza posición X relativa.
     *
     * @param xRatio valor relativo [0..1].
     */
    public void setXRatio(double xRatio) {
        this.xRatio = clampRatio(xRatio, "xRatio");
    }

    /**
     * @return posición Y relativa.
     */
    public double getYRatio() {
        return yRatio;
    }

    /**
     * Actualiza posición Y relativa.
     *
     * @param yRatio valor relativo [0..1].
     */
    public void setYRatio(double yRatio) {
        this.yRatio = clampRatio(yRatio, "yRatio");
    }

    /**
     * @return ancho relativo.
     */
    public double getWidthRatio() {
        return widthRatio;
    }

    /**
     * Actualiza ancho relativo.
     *
     * @param widthRatio ancho relativo [0..1].
     */
    public void setWidthRatio(double widthRatio) {
        this.widthRatio = clampRatio(widthRatio, "widthRatio");
    }

    /**
     * @return alto relativo.
     */
    public double getHeightRatio() {
        return heightRatio;
    }

    /**
     * Actualiza alto relativo.
     *
     * @param heightRatio alto relativo [0..1].
     */
    public void setHeightRatio(double heightRatio) {
        this.heightRatio = clampRatio(heightRatio, "heightRatio");
    }

    /**
     * @return texto asociado al elemento.
     */
    public String getText() {
        return text;
    }

    /**
     * Define el texto del elemento.
     *
     * @param text texto a establecer.
     */
    public void setText(String text) {
        this.text = text == null ? "" : text.strip();
    }

    /**
     * @return cantidad de columnas para elementos tipo tabla.
     */
    public int getTableColumnCount() {
        return tableColumnCount;
    }

    /**
     * Define la cantidad de columnas para el elemento tabla.
     *
     * @param tableColumnCount cantidad de columnas.
     */
    public void setTableColumnCount(int tableColumnCount) {
        if (tableColumnCount < 0) {
            throw new IllegalArgumentException("tableColumnCount must be >= 0");
        }
        this.tableColumnCount = tableColumnCount;
    }

    /**
     * @return cantidad de filas de detalle para la tabla (1 o 4).
     */
    public int getTableDataRows() {
        return tableDataRows;
    }

    /**
     * Define las filas de detalle para tabla.
     *
     * @param tableDataRows cantidad de filas permitidas (1 o 4).
     */
    public void setTableDataRows(int tableDataRows) {
        if (tableDataRows != 1 && tableDataRows != 4) {
            throw new IllegalArgumentException("tableDataRows only supports values 1 or 4");
        }
        this.tableDataRows = tableDataRows;
    }

    /**
     * @return definición de anchos por columna en porcentaje separado por comas.
     */
    public String getTableColumnWidths() {
        return tableColumnWidths;
    }

    /**
     * Define anchos por columna para la tabla.
     *
     * @param tableColumnWidths valores por columna separados por coma.
     */
    public void setTableColumnWidths(String tableColumnWidths) {
        this.tableColumnWidths = tableColumnWidths == null ? "" : tableColumnWidths.strip();
    }

    /**
     * Crea una copia independiente del elemento.
     *
     * @return copia del elemento actual.
     */
    public OverlayElement copy() {
        OverlayElement copy = new OverlayElement(id, type, xRatio, yRatio, widthRatio, heightRatio, text);
        copy.setTableColumnCount(tableColumnCount);
        copy.setTableDataRows(tableDataRows);
        copy.setTableColumnWidths(tableColumnWidths);
        return copy;
    }

    private static double clampRatio(double value, String fieldName) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(fieldName + " must be finite");
        }
        return Math.max(0.0d, Math.min(1.0d, value));
    }
}
