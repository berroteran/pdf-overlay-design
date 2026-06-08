package com.example.pdfoverlay.ui;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;

/**
 * Fábrica centralizada de íconos vectoriales para botones de la UI.
 */
public final class ButtonIconFactory {
    private static final Color STROKE = Color.web("#2f4f66");
    private static final Color PAPER_FILL = Color.web("#fdfefe");
    private static final Color ACCENT_BLUE = Color.web("#1571c2");
    private static final Color ACCENT_RED = Color.web("#b71c1c");
    private static final Color ACCENT_GREEN = Color.web("#138f42");
    private static final Color ACCENT_GRAY = Color.web("#5f6d79");

    private ButtonIconFactory() {
    }

    /**
     * @return ícono para abrir PDF.
     */
    public static Node openPdfIcon() {
        Group icon = createDocumentBase();
        Rectangle footer = new Rectangle(3.2, 10.2, 7.2, 1.7);
        footer.setFill(ACCENT_RED);
        footer.setArcWidth(1.2);
        footer.setArcHeight(1.2);
        icon.getChildren().add(footer);
        return wrap(icon);
    }

    /**
     * @return ícono de carpeta para menús de archivo.
     */
    public static Node fileMenuIcon() {
        Rectangle body = new Rectangle(1.8, 4.0, 10.6, 7.0);
        body.setArcWidth(1.6);
        body.setArcHeight(1.6);
        body.setFill(Color.web("#f0d89b"));
        body.setStroke(STROKE);
        body.setStrokeWidth(1.0);

        Polygon tab = new Polygon(2.2, 4.0, 4.6, 2.3, 7.2, 2.3, 8.4, 4.0);
        tab.setFill(Color.web("#e6c671"));
        tab.setStroke(STROKE);
        tab.setStrokeWidth(1.0);
        return wrap(new Group(body, tab));
    }

    /**
     * @return ícono para abrir HTML.
     */
    public static Node openHtmlIcon() {
        Group icon = createDocumentBase();
        icon.getChildren().addAll(
                createLine(3.2, 6.1, 2.0, 7.4, ACCENT_BLUE, 1.2),
                createLine(2.0, 7.4, 3.2, 8.7, ACCENT_BLUE, 1.2),
                createLine(8.8, 6.1, 10.0, 7.4, ACCENT_BLUE, 1.2),
                createLine(10.0, 7.4, 8.8, 8.7, ACCENT_BLUE, 1.2),
                createLine(6.0, 6.0, 5.2, 8.8, ACCENT_BLUE, 1.1)
        );
        return wrap(icon);
    }

    /**
     * @return ícono para guardar HTML.
     */
    public static Node saveHtmlIcon() {
        Rectangle body = new Rectangle(1.4, 1.4, 11.2, 11.2);
        body.setArcWidth(1.6);
        body.setArcHeight(1.6);
        body.setFill(Color.web("#e8f2fb"));
        body.setStroke(STROKE);
        body.setStrokeWidth(1.0);

        Rectangle topBar = new Rectangle(3.0, 2.8, 5.8, 2.5);
        topBar.setFill(ACCENT_BLUE);
        topBar.setArcWidth(1.0);
        topBar.setArcHeight(1.0);

        Rectangle slot = new Rectangle(4.0, 7.0, 6.4, 3.6);
        slot.setFill(PAPER_FILL);
        slot.setStroke(STROKE);
        slot.setStrokeWidth(0.8);

        Group icon = new Group(body, topBar, slot);
        return wrap(icon);
    }

    /**
     * @return ícono para iniciar un proyecto nuevo.
     */
    public static Node newProjectIcon() {
        Group icon = createDocumentBase();
        Line horizontal = createLine(4.0, 7.0, 10.0, 7.0, ACCENT_GREEN, 1.6);
        Line vertical = createLine(7.0, 4.0, 7.0, 10.0, ACCENT_GREEN, 1.6);
        icon.getChildren().addAll(horizontal, vertical);
        return wrap(icon);
    }

    /**
     * @return ícono para archivos recientes.
     */
    public static Node recentFilesIcon() {
        Rectangle page = new Rectangle(3.0, 1.8, 8.2, 10.4);
        page.setFill(PAPER_FILL);
        page.setStroke(STROKE);
        page.setStrokeWidth(1.0);
        page.setArcWidth(1.2);
        page.setArcHeight(1.2);

        Line clockHour = createLine(7.0, 5.0, 7.0, 7.2, ACCENT_BLUE, 1.1);
        Line clockMinute = createLine(7.0, 7.2, 9.0, 7.2, ACCENT_BLUE, 1.1);
        Circle clock = new Circle(7.0, 7.2, 3.2);
        clock.setFill(Color.TRANSPARENT);
        clock.setStroke(ACCENT_BLUE);
        clock.setStrokeWidth(1.0);

        return wrap(new Group(page, clock, clockHour, clockMinute));
    }

    /**
     * @return ícono para imprimir HTML.
     */
    public static Node printHtmlIcon() {
        return wrap(createPrinterIcon(ACCENT_BLUE));
    }

    /**
     * @return ícono para imprimir solo el overlay HTML.
     */
    public static Node printHtmlOnlyIcon() {
        return wrap(createPrinterIcon(ACCENT_GREEN));
    }

    /**
     * @return ícono para imprimir PDF.
     */
    public static Node printPdfIcon() {
        return wrap(createPrinterIcon(ACCENT_RED));
    }

    /**
     * @return ícono de navegador/web.
     */
    public static Node browserIcon() {
        Circle globe = new Circle(7.0, 7.0, 4.8);
        globe.setFill(Color.web("#e7f3ff"));
        globe.setStroke(ACCENT_BLUE);
        globe.setStrokeWidth(1.1);

        Line equator = createLine(2.8, 7.0, 11.2, 7.0, ACCENT_BLUE, 0.9);
        Line meridian = createLine(7.0, 2.8, 7.0, 11.2, ACCENT_BLUE, 0.9);
        Line latTop = createLine(3.6, 4.8, 10.4, 4.8, ACCENT_BLUE, 0.8);
        Line latBottom = createLine(3.6, 9.2, 10.4, 9.2, ACCENT_BLUE, 0.8);
        Line lonLeftTop = createLine(5.0, 2.9, 4.1, 11.1, ACCENT_BLUE, 0.8);
        Line lonRightTop = createLine(9.0, 2.9, 9.9, 11.1, ACCENT_BLUE, 0.8);

        return wrap(new Group(globe, equator, meridian, latTop, latBottom, lonLeftTop, lonRightTop));
    }

    /**
     * @return ícono para salir/cerrar.
     */
    public static Node exitIcon() {
        Rectangle door = new Rectangle(2.2, 2.0, 5.0, 10.0);
        door.setFill(Color.web("#e9edf2"));
        door.setStroke(STROKE);
        door.setStrokeWidth(1.0);

        Circle knob = new Circle(6.0, 7.0, 0.5);
        knob.setFill(ACCENT_GRAY);

        Line arrowBody = createLine(7.6, 7.0, 12.0, 7.0, ACCENT_RED, 1.5);
        Line arrowTop = createLine(10.2, 5.2, 12.0, 7.0, ACCENT_RED, 1.5);
        Line arrowBottom = createLine(10.2, 8.8, 12.0, 7.0, ACCENT_RED, 1.5);

        return wrap(new Group(door, knob, arrowBody, arrowTop, arrowBottom));
    }

    /**
     * @return ícono de navegación anterior.
     */
    public static Node previousPageIcon() {
        Polygon arrow = new Polygon(9.6, 2.0, 4.2, 7.0, 9.6, 12.0);
        arrow.setFill(ACCENT_GRAY);
        arrow.setStroke(STROKE);
        arrow.setStrokeWidth(1.0);
        return wrap(new Group(arrow));
    }

    /**
     * @return ícono de navegación siguiente.
     */
    public static Node nextPageIcon() {
        Polygon arrow = new Polygon(4.4, 2.0, 9.8, 7.0, 4.4, 12.0);
        arrow.setFill(ACCENT_GRAY);
        arrow.setStroke(STROKE);
        arrow.setStrokeWidth(1.0);
        return wrap(new Group(arrow));
    }

    /**
     * @return ícono de herramienta de selección.
     */
    public static Node selectToolIcon() {
        Polygon pointer = new Polygon(2.0, 1.2, 10.5, 6.8, 6.5, 7.7, 8.4, 12.0, 6.8, 12.6, 5.0, 8.2, 2.0, 9.5);
        pointer.setFill(Color.web("#e8edf2"));
        pointer.setStroke(STROKE);
        pointer.setStrokeWidth(1.0);
        return wrap(new Group(pointer));
    }

    /**
     * @return ícono de herramienta de texto.
     */
    public static Node textToolIcon() {
        Line top = createLine(3.0, 3.0, 11.0, 3.0, STROKE, 1.4);
        Line stem = createLine(7.0, 3.0, 7.0, 11.8, STROKE, 1.4);
        return wrap(new Group(top, stem));
    }

    /**
     * @return ícono de herramienta etiqueta.
     */
    public static Node labelToolIcon() {
        Polygon tag = new Polygon(2.0, 5.4, 6.8, 2.0, 12.0, 2.0, 12.0, 9.0, 6.8, 9.0);
        tag.setFill(Color.web("#eef4fa"));
        tag.setStroke(STROKE);
        tag.setStrokeWidth(1.0);

        Circle hole = new Circle(8.9, 5.4, 1.0);
        hole.setFill(Color.web("#a7b6c3"));
        hole.setStroke(STROKE);
        hole.setStrokeWidth(0.8);

        return wrap(new Group(tag, hole));
    }

    /**
     * @return ícono de herramienta botón.
     */
    public static Node buttonToolIcon() {
        Rectangle button = new Rectangle(2.0, 3.2, 10.2, 8.0);
        button.setArcWidth(4.0);
        button.setArcHeight(4.0);
        button.setFill(Color.web("#ecf3f9"));
        button.setStroke(STROKE);
        button.setStrokeWidth(1.0);
        return wrap(new Group(button));
    }

    /**
     * @return ícono de herramienta marcador.
     */
    public static Node markerToolIcon() {
        Circle marker = new Circle(7.0, 7.0, 4.2);
        marker.setFill(Color.web("#e04747"));
        marker.setStroke(Color.web("#8f1f1f"));
        marker.setStrokeWidth(1.0);
        return wrap(new Group(marker));
    }

    /**
     * @return ícono de herramienta tabla.
     */
    public static Node tableToolIcon() {
        Rectangle border = new Rectangle(2.0, 2.0, 10.0, 10.0);
        border.setFill(Color.web("#eef4fa"));
        border.setStroke(STROKE);
        border.setStrokeWidth(1.0);

        Line v1 = createLine(5.3, 2.0, 5.3, 12.0, STROKE, 0.9);
        Line v2 = createLine(8.6, 2.0, 8.6, 12.0, STROKE, 0.9);
        Line h1 = createLine(2.0, 5.0, 12.0, 5.0, STROKE, 0.9);
        Line h2 = createLine(2.0, 8.5, 12.0, 8.5, STROKE, 0.9);

        return wrap(new Group(border, v1, v2, h1, h2));
    }

    /**
     * @return ícono de herramienta de medición.
     */
    public static Node measureToolIcon() {
        Polygon ruler = new Polygon(
                2.0, 10.8,
                10.8, 2.0,
                12.0, 3.2,
                3.2, 12.0
        );
        ruler.setFill(Color.web("#f5e4a6"));
        ruler.setStroke(STROKE);
        ruler.setStrokeWidth(1.0);

        Line tick1 = createLine(4.2, 9.8, 5.0, 10.6, STROKE, 0.8);
        Line tick2 = createLine(5.8, 8.2, 6.8, 9.2, STROKE, 0.8);
        Line tick3 = createLine(7.4, 6.6, 8.2, 7.4, STROKE, 0.8);
        Line tick4 = createLine(9.0, 5.0, 10.0, 6.0, STROKE, 0.8);

        return wrap(new Group(ruler, tick1, tick2, tick3, tick4));
    }

    /**
     * @return ícono para aplicar cambios de texto.
     */
    public static Node applyTextIcon() {
        Line lineA = createLine(2.6, 8.2, 5.6, 11.0, ACCENT_GREEN, 1.8);
        Line lineB = createLine(5.6, 11.0, 11.8, 3.0, ACCENT_GREEN, 1.8);
        return wrap(new Group(lineA, lineB));
    }

    /**
     * @return ícono para eliminar elemento.
     */
    public static Node deleteIcon() {
        Rectangle binBody = new Rectangle(3.2, 4.2, 7.2, 7.8);
        binBody.setFill(Color.web("#f4dede"));
        binBody.setStroke(Color.web("#a63d3d"));
        binBody.setStrokeWidth(1.0);
        binBody.setArcWidth(1.0);
        binBody.setArcHeight(1.0);

        Rectangle lid = new Rectangle(2.4, 2.4, 8.8, 1.7);
        lid.setFill(Color.web("#d78a8a"));
        lid.setStroke(Color.web("#a63d3d"));
        lid.setStrokeWidth(1.0);

        Line handle = createLine(5.4, 1.8, 8.2, 1.8, Color.web("#a63d3d"), 1.2);
        return wrap(new Group(binBody, lid, handle));
    }

    private static Group createDocumentBase() {
        Rectangle page = new Rectangle(2.0, 1.4, 9.8, 11.2);
        page.setFill(PAPER_FILL);
        page.setStroke(STROKE);
        page.setStrokeWidth(1.0);
        page.setArcWidth(1.4);
        page.setArcHeight(1.4);

        Polygon fold = new Polygon(8.6, 1.4, 11.8, 1.4, 11.8, 4.6);
        fold.setFill(Color.web("#dbe8f3"));
        fold.setStroke(STROKE);
        fold.setStrokeWidth(0.8);

        Line foldEdge = createLine(8.6, 1.4, 8.6, 4.6, STROKE, 0.8);
        return new Group(page, fold, foldEdge);
    }

    private static Group createPrinterIcon(Color accentColor) {
        Rectangle paper = new Rectangle(3.5, 1.0, 7.0, 3.4);
        paper.setFill(PAPER_FILL);
        paper.setStroke(STROKE);
        paper.setStrokeWidth(1.0);

        Rectangle body = new Rectangle(2.0, 4.0, 10.0, 5.3);
        body.setFill(Color.web("#e9f0f5"));
        body.setStroke(STROKE);
        body.setStrokeWidth(1.0);
        body.setArcWidth(2.0);
        body.setArcHeight(2.0);

        Rectangle tray = new Rectangle(3.0, 8.8, 8.0, 3.0);
        tray.setFill(PAPER_FILL);
        tray.setStroke(STROKE);
        tray.setStrokeWidth(1.0);

        Circle led = new Circle(10.2, 6.2, 0.9);
        led.setFill(accentColor);
        led.setStroke(STROKE);
        led.setStrokeWidth(0.6);

        return new Group(paper, body, tray, led);
    }

    private static Line createLine(double startX, double startY, double endX, double endY, Color color, double width) {
        Line line = new Line(startX, startY, endX, endY);
        line.setStroke(color);
        line.setStrokeWidth(width);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        return line;
    }

    private static Node wrap(Node iconNode) {
        StackPane pane = new StackPane(iconNode);
        pane.setAlignment(Pos.CENTER);
        pane.setMinSize(16, 16);
        pane.setPrefSize(16, 16);
        pane.setMaxSize(16, 16);
        return pane;
    }
}
