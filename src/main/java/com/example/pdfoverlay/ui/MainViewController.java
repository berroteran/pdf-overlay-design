package com.example.pdfoverlay.ui;

import com.example.pdfoverlay.model.DocumentStatus;
import com.example.pdfoverlay.model.OverlayElement;
import com.example.pdfoverlay.model.OverlayElementType;
import com.example.pdfoverlay.model.OverlayPage;
import com.example.pdfoverlay.model.OverlayProject;
import com.example.pdfoverlay.model.PdfDocumentMetadata;
import com.example.pdfoverlay.model.PdfPageMetadata;
import com.example.pdfoverlay.service.ExportOptions;
import com.example.pdfoverlay.service.HtmlExportService;
import com.example.pdfoverlay.service.PdfService;
import com.example.pdfoverlay.service.PrintService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.prefs.Preferences;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controlador principal de la aplicación de edición.
 */
public final class MainViewController {
    private static final Logger LOGGER = Logger.getLogger(MainViewController.class.getName());

    private static final double DEFAULT_CANVAS_WIDTH = 900;
    private static final double DEFAULT_CANVAS_HEIGHT = 1200;
    private static final double RULER_SIZE = 26.0d;
    private static final double GRID_MAJOR_CM = 1.0d;
    private static final double GRID_MINOR_CM = 0.5d;
    private static final float PREVIEW_DPI = 180.0f;
    private static final int DEFAULT_TABLE_COLUMNS = 4;
    private static final int MAX_TABLE_COLUMNS = 12;
    private static final int MAX_RECENT_FILES = 10;
    private static final String RECENT_FILES_PREFERENCE_KEY = "recentFiles";
    private static final double ZOOM_STEP_PERCENT = 10.0d;
    private static final int AUTO_FIT_MAX_ATTEMPTS = 16;
    private static final double RESIZE_HANDLE_SIZE = 8.0d;
    private static final double MIN_RESIZABLE_SIZE = 8.0d;
    private static final double MIN_MARKER_SIZE = 6.0d;
    private static final double MIN_TABLE_COLUMN_WIDTH_MM = 0.01d;
    private static final String SOURCE_BLOCK_FULL = "Full document";
    private static final String SOURCE_BLOCK_HEAD = "HEAD";
    private static final String SOURCE_BLOCK_STYLE = "STYLE";
    private static final String SOURCE_BLOCK_BODY = "BODY";
    private static final String SOURCE_BLOCK_METADATA = "Metadata";
    private static final Pattern HTML_HEAD_PATTERN = Pattern.compile("(?is)<head\\b[^>]*>(.*?)</head>");
    private static final Pattern HTML_BODY_PATTERN = Pattern.compile("(?is)<body\\b[^>]*>(.*?)</body>");
    private static final Pattern HTML_STYLE_PATTERN = Pattern.compile("(?is)<style\\b[^>]*>.*?</style>");
    private static final Pattern OVERLAY_METADATA_PATTERN = Pattern.compile(
            "(?is)<!--\\s*PDF_OVERLAY_METADATA_BEGIN.*?PDF_OVERLAY_METADATA_END\\s*-->"
    );

    private final Stage ownerStage;
    private final PdfService pdfService;
    private final HtmlExportService htmlExportService;
    private final PrintService printService;

    private final BorderPane root;
    private final ImageView pageImageView;
    private final Canvas gridCanvas;
    private final Pane overlayPane;
    private final StackPane pageStack;
    private final ScrollPane canvasScrollPane;
    private final GridPane canvasWorkspacePane;
    private final Canvas topRulerCanvas;
    private final Canvas leftRulerCanvas;
    private final Region rulerCorner;
    private final TabPane workspaceTabPane;
    private final Tab graphicTab;
    private final Tab htmlSourceTab;
    private final Tab finalSourceTab;
    private final WebView htmlSourceWebView;
    private final WebView finalSourceWebView;
    private final ComboBox<String> sourceBlockSelector;
    private final ComboBox<UiTheme> themeSelector;

    private final ToggleGroup toolToggleGroup;
    private final Label pageLabel;
    private final Label statusLabel;
    private final Label documentSizeLabel;
    private final Label zoomValueLabel;
    private final Label selectedElementTypeLabel;
    private final TextField selectedElementIdField;
    private final Button applyElementIdButton;
    private final TextField selectedElementTextField;
    private final CheckBox enableStatusWatermarkCheck;
    private final CheckBox showMeasurementGridCheck;
    private final ComboBox<DocumentStatus> documentStatusCombo;
    private final TextField tableWidthMillimetersField;
    private final TextField tableColumnWidthsField;
    private final ComboBox<Integer> tableRowsCombo;
    private final Button applyTableConfigButton;
    private final ComboBox<Integer> exportDpiCombo;
    private final Slider zoomSlider;

    private final Button printHtmlButton;
    private final Button printHtmlOnlyButton;
    private final Button printPdfButton;
    private final Button openHtmlInBrowserButton;
    private final Button previousPageButton;
    private final Button nextPageButton;
    private final Button deleteSelectedButton;
    private final MenuItem newProjectMenuItem;
    private final Menu recentFilesMenu;
    private final Label measurementValueLabel;
    private final Region measurementRectangle;
    private final Label measurementOverlayLabel;

    private final Map<String, Region> elementNodes;
    private final Map<ResizeHandlePosition, Region> resizeHandles;
    private final Deque<DeletedElementSnapshot> deletedElementsHistory;
    private final Preferences preferences;

    private EditorTool activeTool;
    private OverlayProject currentProject;
    private Path currentHtmlPath;
    private int currentPageIndex;
    private OverlayElement selectedElement;
    private Region selectedNode;
    private ResizeState activeResizeState;
    private MeasurementState activeMeasurementState;
    private boolean autoFitZoomOnNextLoad;
    private int autoFitPendingAttempts;
    private String latestGeneratedHtmlSource;
    private String latestFinalSource;
    private UiTheme activeTheme;

    private double currentPagePixelWidth;
    private double currentPagePixelHeight;
    private boolean hasUnsavedChanges;
    private boolean suppressChangeTracking;
    private boolean bypassCloseConfirmation;

    /**
     * Crea el controlador principal y sus componentes visuales.
     *
     * @param ownerStage ventana principal.
     */
    public MainViewController(Stage ownerStage) {
        this.ownerStage = Objects.requireNonNull(ownerStage, "ownerStage is required");
        this.pdfService = new PdfService();
        this.htmlExportService = new HtmlExportService(pdfService);
        this.printService = new PrintService();

        this.root = new BorderPane();
        this.pageImageView = new ImageView();
        this.gridCanvas = new Canvas(DEFAULT_CANVAS_WIDTH, DEFAULT_CANVAS_HEIGHT);
        this.overlayPane = new Pane();
        this.pageStack = new StackPane();
        this.canvasScrollPane = new ScrollPane();
        this.canvasWorkspacePane = new GridPane();
        this.topRulerCanvas = new Canvas();
        this.leftRulerCanvas = new Canvas();
        this.rulerCorner = new Region();
        this.workspaceTabPane = new TabPane();
        this.graphicTab = new Tab("Graphic Mode");
        this.htmlSourceTab = new Tab("HTML Source");
        this.finalSourceTab = new Tab("Final Source");
        this.htmlSourceWebView = new WebView();
        this.finalSourceWebView = new WebView();
        this.sourceBlockSelector = new ComboBox<>();
        this.themeSelector = new ComboBox<>();

        this.toolToggleGroup = new ToggleGroup();
        this.pageLabel = new Label("Page -/-");
        this.statusLabel = new Label("Open a PDF to start");
        this.documentSizeLabel = new Label("Document size: -");
        this.zoomValueLabel = new Label("100%");
        this.selectedElementTypeLabel = new Label("None");
        this.selectedElementIdField = new TextField();
        this.applyElementIdButton = new Button("Apply ID");
        this.selectedElementTextField = new TextField();
        this.enableStatusWatermarkCheck = new CheckBox("Enable status watermark");
        this.showMeasurementGridCheck = new CheckBox("Show grid (cm)");
        this.documentStatusCombo = new ComboBox<>();
        this.tableWidthMillimetersField = new TextField();
        this.tableColumnWidthsField = new TextField();
        this.tableRowsCombo = new ComboBox<>();
        this.applyTableConfigButton = new Button("Apply table config");
        this.exportDpiCombo = new ComboBox<>();
        this.zoomSlider = new Slider(0.0, 300.0, 100.0);

        this.printHtmlButton = new Button("Print HTML");
        this.printHtmlOnlyButton = new Button("Print HTML Only");
        this.printPdfButton = new Button("Print PDF");
        this.openHtmlInBrowserButton = new Button("Open HTML");
        this.previousPageButton = new Button("< Prev");
        this.nextPageButton = new Button("Next >");
        this.deleteSelectedButton = new Button("Delete selected");
        this.newProjectMenuItem = new MenuItem("New Project");
        this.recentFilesMenu = new Menu("Recent Files");
        this.measurementValueLabel = new Label("W x H: -");
        this.measurementRectangle = new Region();
        this.measurementOverlayLabel = new Label();

        this.elementNodes = new HashMap<>();
        this.resizeHandles = new EnumMap<>(ResizeHandlePosition.class);
        this.deletedElementsHistory = new ArrayDeque<>();
        this.preferences = Preferences.userNodeForPackage(MainViewController.class);
        this.selectedElementIdField.setDisable(true);
        this.applyElementIdButton.setDisable(true);

        this.activeTool = EditorTool.SELECT;
        this.currentPageIndex = 0;
        this.currentHtmlPath = null;
        this.currentPagePixelWidth = DEFAULT_CANVAS_WIDTH;
        this.currentPagePixelHeight = DEFAULT_CANVAS_HEIGHT;
        this.hasUnsavedChanges = false;
        this.suppressChangeTracking = false;
        this.bypassCloseConfirmation = false;
        this.autoFitZoomOnNextLoad = false;
        this.autoFitPendingAttempts = 0;
        this.latestGeneratedHtmlSource = "<!-- Open a PDF or HTML project first -->";
        this.latestFinalSource = "<!-- Open a PDF or HTML project first -->";
        this.activeTheme = UiTheme.MODENA;

        configureCanvas();
        configureResizeHandles();
        configureMeasurementOverlay();
        configureActions();
        configureThemeSelector();
        zoomValueLabel.setText(formatZoomPercentage(zoomSlider.getValue()));

        root.getStyleClass().add("theme-root");
        root.setTop(buildTopArea());
        root.setCenter(buildWorkspacePane());
        root.setRight(buildInspectorPanel());
        root.setBottom(buildStatusBar());
        root.setFocusTraversable(true);
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                applyTheme(activeTheme);
            }
        });

        updateButtonsState();
        installCloseHandler();
    }

    /**
     * @return nodo raíz de la vista principal.
     */
    public BorderPane getRoot() {
        return root;
    }

    private void configureCanvas() {
        pageImageView.setPreserveRatio(false);
        pageImageView.setSmooth(true);
        gridCanvas.setMouseTransparent(true);

        overlayPane.setManaged(true);
        overlayPane.setPickOnBounds(true);
        overlayPane.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleMeasurementMousePressed);
        overlayPane.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handleMeasurementMouseDragged);
        overlayPane.addEventFilter(MouseEvent.MOUSE_RELEASED, this::handleMeasurementMouseReleased);
        overlayPane.addEventFilter(MouseEvent.MOUSE_CLICKED, this::handleMeasurementMouseClicked);
        overlayPane.setOnMouseClicked(this::handleCanvasClick);

        pageStack.getChildren().addAll(pageImageView, gridCanvas, overlayPane);
        StackPane.setAlignment(pageImageView, Pos.TOP_LEFT);
        StackPane.setAlignment(gridCanvas, Pos.TOP_LEFT);
        StackPane.setAlignment(overlayPane, Pos.TOP_LEFT);
        overlayPane.toFront();
        pageStack.setPrefSize(DEFAULT_CANVAS_WIDTH, DEFAULT_CANVAS_HEIGHT);
        pageStack.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Group zoomGroup = new Group(pageStack);
        canvasScrollPane.setContent(zoomGroup);
        canvasScrollPane.setPannable(true);
        canvasScrollPane.setFitToHeight(false);
        canvasScrollPane.setFitToWidth(false);
        canvasScrollPane.getStyleClass().add("editor-scroll-pane");

        topRulerCanvas.setHeight(RULER_SIZE);
        leftRulerCanvas.setWidth(RULER_SIZE);
        rulerCorner.getStyleClass().add("ruler-corner");

        ColumnConstraints rulerColumn = new ColumnConstraints(RULER_SIZE);
        ColumnConstraints contentColumn = new ColumnConstraints();
        contentColumn.setHgrow(Priority.ALWAYS);
        RowConstraints rulerRow = new RowConstraints(RULER_SIZE);
        RowConstraints contentRow = new RowConstraints();
        contentRow.setVgrow(Priority.ALWAYS);
        canvasWorkspacePane.getColumnConstraints().setAll(rulerColumn, contentColumn);
        canvasWorkspacePane.getRowConstraints().setAll(rulerRow, contentRow);
        canvasWorkspacePane.add(rulerCorner, 0, 0);
        canvasWorkspacePane.add(topRulerCanvas, 1, 0);
        canvasWorkspacePane.add(leftRulerCanvas, 0, 1);
        canvasWorkspacePane.add(canvasScrollPane, 1, 1);
    }

    private void configureResizeHandles() {
        for (ResizeHandlePosition position : ResizeHandlePosition.values()) {
            Region handle = new Region();
            handle.getStyleClass().add("resize-handle");
            handle.setManaged(false);
            handle.setVisible(false);
            handle.setCursor(position.cursor());
            handle.setPrefSize(RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE);
            handle.setMinSize(RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE);
            handle.setMaxSize(RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE);
            handle.setOnMousePressed(event -> handleResizePressed(event, position));
            handle.setOnMouseDragged(event -> handleResizeDragged(event, position));
            handle.setOnMouseReleased(event -> handleResizeReleased(event));
            resizeHandles.put(position, handle);
        }
    }

    private void configureMeasurementOverlay() {
        measurementRectangle.getStyleClass().add("measurement-rectangle");
        measurementRectangle.setManaged(false);
        measurementRectangle.setMouseTransparent(true);
        measurementRectangle.setVisible(false);

        measurementOverlayLabel.getStyleClass().add("measurement-overlay-label");
        measurementOverlayLabel.setManaged(false);
        measurementOverlayLabel.setMouseTransparent(true);
        measurementOverlayLabel.setVisible(false);
    }

    private void installMeasurementOverlay() {
        if (!overlayPane.getChildren().contains(measurementRectangle)) {
            overlayPane.getChildren().add(measurementRectangle);
        }
        if (!overlayPane.getChildren().contains(measurementOverlayLabel)) {
            overlayPane.getChildren().add(measurementOverlayLabel);
        }
        hideMeasurementOverlay();
    }

    private void bringMeasurementOverlayToFront() {
        measurementRectangle.toFront();
        measurementOverlayLabel.toFront();
    }

    private boolean isMeasurementOverlayVisible() {
        return measurementRectangle.isVisible() || measurementOverlayLabel.isVisible();
    }

    private void hideMeasurementOverlay() {
        measurementRectangle.setVisible(false);
        measurementOverlayLabel.setVisible(false);
        activeMeasurementState = null;
    }

    private void resetMeasurementReadout() {
        measurementValueLabel.setText("W x H: -");
        measurementOverlayLabel.setText("");
    }

    private void finishMeasurementMode(String statusMessage) {
        hideMeasurementOverlay();
        if (activeTool == EditorTool.MEASURE) {
            activateTool(EditorTool.SELECT);
        }
        statusLabel.setText(statusMessage);
    }

    private void installResizeHandles() {
        for (Region handle : resizeHandles.values()) {
            if (!overlayPane.getChildren().contains(handle)) {
                overlayPane.getChildren().add(handle);
            }
        }
        hideResizeHandles();
    }

    private void showResizeHandles() {
        if (selectedNode == null) {
            hideResizeHandles();
            return;
        }
        for (Region handle : resizeHandles.values()) {
            handle.setVisible(true);
        }
        positionResizeHandles();
        bringResizeHandlesToFront();
    }

    private void hideResizeHandles() {
        for (Region handle : resizeHandles.values()) {
            handle.setVisible(false);
        }
    }

    private void bringResizeHandlesToFront() {
        for (Region handle : resizeHandles.values()) {
            handle.toFront();
        }
    }

    private void positionResizeHandles() {
        if (selectedNode == null) {
            return;
        }

        double x = selectedNode.getLayoutX();
        double y = selectedNode.getLayoutY();
        double width = selectedNode.getPrefWidth();
        double height = selectedNode.getPrefHeight();
        double halfHandle = RESIZE_HANDLE_SIZE / 2.0d;

        for (Map.Entry<ResizeHandlePosition, Region> entry : resizeHandles.entrySet()) {
            ResizeHandlePosition position = entry.getKey();
            Region handle = entry.getValue();
            double handleCenterX = switch (position.horizontalAnchor()) {
                case START -> x;
                case CENTER -> x + width / 2.0d;
                case END -> x + width;
            };
            double handleCenterY = switch (position.verticalAnchor()) {
                case START -> y;
                case CENTER -> y + height / 2.0d;
                case END -> y + height;
            };
            handle.resizeRelocate(
                    handleCenterX - halfHandle,
                    handleCenterY - halfHandle,
                    RESIZE_HANDLE_SIZE,
                    RESIZE_HANDLE_SIZE
            );
        }
    }

    private void configureActions() {
        root.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (activeMeasurementState == null
                    && isMeasurementOverlayVisible()
                    && isClickOutsideMeasurementOverlay(event)) {
                finishMeasurementMode("Measurement mode cancelled");
            }
        });
        root.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (activeTool == EditorTool.MEASURE || isMeasurementOverlayVisible()) {
                finishMeasurementMode("Measurement mode cancelled");
                event.consume();
                return;
            }
            if (event.getCode() == KeyCode.DELETE) {
                removeSelectedElement();
                event.consume();
                return;
            }

            if (event.isShortcutDown()) {
                switch (event.getCode()) {
                    case Q -> {
                        requestApplicationClose();
                        event.consume();
                    }
                    case PLUS, ADD -> {
                        zoomIn();
                        event.consume();
                    }
                    case MINUS, SUBTRACT -> {
                        zoomOut();
                        event.consume();
                    }
                    case Z -> {
                        undoLastDeletion();
                        event.consume();
                    }
                    case EQUALS -> {
                        if (event.isShiftDown()) {
                            zoomIn();
                            event.consume();
                        }
                    }
                    default -> {
                        // No-op.
                    }
                }
            }
        });

        zoomSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            double percent = newValue.doubleValue();
            double scale = Math.max(0.0d, percent / 100.0d);
            pageStack.setScaleX(scale);
            pageStack.setScaleY(scale);
            zoomValueLabel.setText(formatZoomPercentage(percent));
            redrawMeasurementGuides();
        });

        exportDpiCombo.getItems().addAll(150, 200, 300, 600);
        exportDpiCombo.setValue(300);
        documentStatusCombo.getItems().addAll(DocumentStatus.DRAFT, DocumentStatus.VOIDED);
        documentStatusCombo.setValue(DocumentStatus.DRAFT);
        documentStatusCombo.setDisable(true);
        enableStatusWatermarkCheck.setSelected(false);
        showMeasurementGridCheck.setSelected(true);
        showMeasurementGridCheck.selectedProperty().addListener((obs, oldValue, newValue) -> redrawMeasurementGuides());
        enableStatusWatermarkCheck.selectedProperty().addListener((obs, oldValue, newValue) -> {
            documentStatusCombo.setDisable(!newValue);
            if (currentProject == null) {
                return;
            }
            currentProject.setStatusWatermarkEnabled(newValue);
            markProjectDirty();
            statusLabel.setText(newValue
                    ? "Status watermark enabled: " + currentProject.getDocumentStatus().getWatermarkText()
                    : "Status watermark disabled");
            if (workspaceTabPane.getSelectionModel().getSelectedItem() == htmlSourceTab) {
                refreshHtmlSourcePreview();
            } else if (workspaceTabPane.getSelectionModel().getSelectedItem() == finalSourceTab) {
                refreshFinalSourcePreview();
            }
        });
        documentStatusCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (currentProject == null || newValue == null) {
                return;
            }
            currentProject.setDocumentStatus(newValue);
            markProjectDirty();
            statusLabel.setText("Document status: " + newValue.getWatermarkText());
            if (workspaceTabPane.getSelectionModel().getSelectedItem() == htmlSourceTab) {
                refreshHtmlSourcePreview();
            } else if (workspaceTabPane.getSelectionModel().getSelectedItem() == finalSourceTab) {
                refreshFinalSourcePreview();
            }
        });
        sourceBlockSelector.getItems().addAll(
                SOURCE_BLOCK_FULL,
                SOURCE_BLOCK_HEAD,
                SOURCE_BLOCK_STYLE,
                SOURCE_BLOCK_BODY,
                SOURCE_BLOCK_METADATA
        );
        sourceBlockSelector.setValue(SOURCE_BLOCK_FULL);
        sourceBlockSelector.valueProperty().addListener((obs, oldValue, newValue) -> updateHtmlSourceView());

        selectedElementIdField.setPromptText("Unique element ID");
        selectedElementTextField.setPromptText("Text for selected element");
        applyElementIdButton.getStyleClass().add("action-button-medium");
        applyElementIdButton.setOnAction(event -> applySelectedElementId());
        tableWidthMillimetersField.setPromptText("Example: 120");
        tableColumnWidthsField.setPromptText("Example: 30,35,25,30");
        tableRowsCombo.getItems().addAll(1, 4);
        tableRowsCombo.setValue(1);
        applyTableConfigButton.setOnAction(event -> applySelectedTableConfiguration());

        workspaceTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == htmlSourceTab) {
                refreshHtmlSourcePreview();
            } else if (newTab == finalSourceTab) {
                refreshFinalSourcePreview();
            }
        });

        toolToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                if (oldToggle != null) {
                    oldToggle.setSelected(true);
                }
                return;
            }
            Object userData = newToggle.getUserData();
            if (userData instanceof EditorTool tool) {
                activeTool = tool;
                if (activeTool == EditorTool.MEASURE) {
                    clearSelection();
                    hideMeasurementOverlay();
                    resetMeasurementReadout();
                    statusLabel.setText("Drag on the overlay to measure W x H in millimeters");
                } else {
                    hideMeasurementOverlay();
                    statusLabel.setText("Active tool: " + activeTool.name());
                }
            }
        });

        canvasScrollPane.addEventFilter(ScrollEvent.SCROLL, this::handleZoomScroll);
        canvasScrollPane.hvalueProperty().addListener((obs, oldValue, newValue) -> redrawMeasurementGuides());
        canvasScrollPane.vvalueProperty().addListener((obs, oldValue, newValue) -> redrawMeasurementGuides());
        canvasScrollPane.viewportBoundsProperty().addListener((obs, oldValue, newValue) -> redrawMeasurementGuides());
    }

    private void configureThemeSelector() {
        themeSelector.getItems().addAll(UiTheme.values());
        themeSelector.setValue(activeTheme);
        themeSelector.setVisibleRowCount(UiTheme.values().length);
        themeSelector.getStyleClass().add("theme-selector");
        themeSelector.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                applyTheme(newValue);
            }
        });
    }

    private Node buildWorkspacePane() {
        graphicTab.setClosable(false);
        graphicTab.setContent(canvasWorkspacePane);

        Label sourceBlockLabel = new Label("Code block");
        sourceBlockLabel.getStyleClass().add("status-meta");

        HBox sourceToolbar = new HBox(8, sourceBlockLabel, sourceBlockSelector);
        sourceToolbar.setPadding(new Insets(8, 10, 8, 10));
        sourceToolbar.setAlignment(Pos.CENTER_LEFT);
        sourceToolbar.getStyleClass().add("toolbar");

        BorderPane sourcePane = new BorderPane();
        sourcePane.setTop(sourceToolbar);
        sourcePane.setCenter(htmlSourceWebView);
        updateHtmlSourceView();

        htmlSourceTab.setClosable(false);
        htmlSourceTab.setContent(sourcePane);

        Label finalSourceLabel = new Label("ERPNext export fragment");
        finalSourceLabel.getStyleClass().add("status-meta");

        HBox finalSourceToolbar = new HBox(8, finalSourceLabel);
        finalSourceToolbar.setPadding(new Insets(8, 10, 8, 10));
        finalSourceToolbar.setAlignment(Pos.CENTER_LEFT);
        finalSourceToolbar.getStyleClass().add("toolbar");

        BorderPane finalSourcePane = new BorderPane();
        finalSourcePane.setTop(finalSourceToolbar);
        finalSourcePane.setCenter(finalSourceWebView);
        updateFinalSourceView();

        finalSourceTab.setClosable(false);
        finalSourceTab.setContent(finalSourcePane);

        workspaceTabPane.getTabs().setAll(graphicTab, htmlSourceTab, finalSourceTab);
        return workspaceTabPane;
    }

    private void zoomIn() {
        zoomSlider.setValue(clamp(zoomSlider.getValue() + ZOOM_STEP_PERCENT, zoomSlider.getMin(), zoomSlider.getMax()));
    }

    private void zoomOut() {
        zoomSlider.setValue(clamp(zoomSlider.getValue() - ZOOM_STEP_PERCENT, zoomSlider.getMin(), zoomSlider.getMax()));
    }

    private void handleZoomScroll(ScrollEvent event) {
        if (!event.isShortcutDown()) {
            return;
        }

        double deltaY = event.getDeltaY();
        if (deltaY > 0) {
            zoomIn();
            event.consume();
        } else if (deltaY < 0) {
            zoomOut();
            event.consume();
        }
    }

    private Node buildTopArea() {
        VBox topArea = new VBox(buildMenuBar(), buildTopToolbar());
        return topArea;
    }

    private MenuBar buildMenuBar() {
        newProjectMenuItem.setOnAction(event -> newProject());
        newProjectMenuItem.setGraphic(ButtonIconFactory.newProjectIcon());

        MenuItem openPdfMenuItem = new MenuItem("Open PDF...");
        openPdfMenuItem.setOnAction(event -> openPdfFile());
        openPdfMenuItem.setGraphic(ButtonIconFactory.openPdfIcon());

        MenuItem openProjectMenuItem = new MenuItem("Open Project HTML...");
        openProjectMenuItem.setOnAction(event -> openHtmlFile());
        openProjectMenuItem.setGraphic(ButtonIconFactory.openHtmlIcon());

        MenuItem saveProjectMenuItem = new MenuItem("Save Project");
        saveProjectMenuItem.setOnAction(event -> saveProjectHtml());
        saveProjectMenuItem.setGraphic(ButtonIconFactory.saveHtmlIcon());

        MenuItem saveProjectAsMenuItem = new MenuItem("Save Project As...");
        saveProjectAsMenuItem.setOnAction(event -> saveProjectHtmlAs());
        saveProjectAsMenuItem.setGraphic(ButtonIconFactory.saveHtmlIcon());

        MenuItem exportErpNextMenuItem = new MenuItem("Export ERPNext...");
        exportErpNextMenuItem.setOnAction(event -> exportErpNextFragment());
        exportErpNextMenuItem.setGraphic(ButtonIconFactory.saveHtmlIcon());

        MenuItem printHtmlMenuItem = new MenuItem("Print HTML with PDF background...");
        printHtmlMenuItem.setOnAction(event -> printHtmlLayer());
        printHtmlMenuItem.setGraphic(ButtonIconFactory.printHtmlIcon());

        MenuItem printHtmlOnlyMenuItem = new MenuItem("Print HTML overlay only...");
        printHtmlOnlyMenuItem.setOnAction(event -> printHtmlOnlyLayer());
        printHtmlOnlyMenuItem.setGraphic(ButtonIconFactory.printHtmlOnlyIcon());

        MenuItem openHtmlOnlyInBrowserMenuItem = new MenuItem("Open HTML overlay in browser...");
        openHtmlOnlyInBrowserMenuItem.setOnAction(event -> openHtmlOnlyInBrowser());
        openHtmlOnlyInBrowserMenuItem.setGraphic(ButtonIconFactory.browserIcon());

        MenuItem printPdfMenuItem = new MenuItem("Print PDF...");
        printPdfMenuItem.setOnAction(event -> printPdfDocument());
        printPdfMenuItem.setGraphic(ButtonIconFactory.printPdfIcon());

        MenuItem exitMenuItem = new MenuItem("Exit");
        exitMenuItem.setOnAction(event -> requestApplicationClose());
        exitMenuItem.setGraphic(ButtonIconFactory.exitIcon());

        recentFilesMenu.setGraphic(ButtonIconFactory.recentFilesIcon());
        recentFilesMenu.setOnShowing(event -> refreshRecentFilesMenu());
        refreshRecentFilesMenu();

        Menu openMenu = new Menu("Open");
        openMenu.setGraphic(ButtonIconFactory.fileMenuIcon());
        openMenu.getItems().addAll(openPdfMenuItem, openProjectMenuItem, recentFilesMenu);

        Menu printMenu = new Menu("Print");
        printMenu.setGraphic(ButtonIconFactory.printHtmlIcon());
        printMenu.getItems().addAll(printPdfMenuItem, printHtmlMenuItem, printHtmlOnlyMenuItem);

        Menu fileMenu = new Menu("File");
        fileMenu.setGraphic(ButtonIconFactory.fileMenuIcon());
        fileMenu.getItems().addAll(
                newProjectMenuItem,
                new SeparatorMenuItem(),
                openMenu,
                new SeparatorMenuItem(),
                saveProjectMenuItem,
                saveProjectAsMenuItem,
                exportErpNextMenuItem,
                openHtmlOnlyInBrowserMenuItem,
                new SeparatorMenuItem(),
                printMenu,
                new SeparatorMenuItem(),
                exitMenuItem
        );

        MenuBar menuBar = new MenuBar(fileMenu);
        return menuBar;
    }

    private Node buildTopToolbar() {
        printHtmlButton.setOnAction(event -> printHtmlLayer());
        printHtmlOnlyButton.setOnAction(event -> printHtmlOnlyLayer());
        printPdfButton.setOnAction(event -> printPdfDocument());
        openHtmlInBrowserButton.setOnAction(event -> openHtmlOnlyInBrowser());
        applyToolbarButtonStyle(printHtmlButton);
        applyToolbarButtonStyle(printHtmlOnlyButton);
        applyToolbarButtonStyle(printPdfButton);
        applyToolbarButtonStyle(openHtmlInBrowserButton);
        applyButtonIcon(printHtmlButton, ButtonIconFactory.printHtmlIcon());
        applyButtonIcon(printHtmlOnlyButton, ButtonIconFactory.printHtmlOnlyIcon());
        applyButtonIcon(printPdfButton, ButtonIconFactory.printPdfIcon());
        applyButtonIcon(openHtmlInBrowserButton, ButtonIconFactory.browserIcon());
        applyButtonTooltip(printHtmlButton, "Print HTML with embedded PDF background");
        applyButtonTooltip(printHtmlOnlyButton, "Print strictly the HTML overlay only");
        applyButtonTooltip(printPdfButton, "Print PDF");
        applyButtonTooltip(openHtmlInBrowserButton, "Open HTML overlay in default browser");

        ToggleButton selectToolButton = createToolButton("Select", EditorTool.SELECT, true, ButtonIconFactory.selectToolIcon());
        ToggleButton textFieldToolButton = createToolButton("Text", EditorTool.TEXT_FIELD, false, ButtonIconFactory.textToolIcon());
        ToggleButton labelToolButton = createToolButton("Label", EditorTool.LABEL, false, ButtonIconFactory.labelToolIcon());
        ToggleButton buttonToolButton = createToolButton("Button", EditorTool.BUTTON, false, ButtonIconFactory.buttonToolIcon());
        ToggleButton markerToolButton = createToolButton("Point", EditorTool.MARKER, false, ButtonIconFactory.markerToolIcon());
        ToggleButton tableToolButton = createToolButton("Table", EditorTool.TABLE, false, ButtonIconFactory.tableToolIcon());
        ToggleButton measureToolButton = createToolButton("Measure", EditorTool.MEASURE, false, ButtonIconFactory.measureToolIcon());
        applyButtonTooltip(measureToolButton, "Measure a temporary width and height in millimeters");
        measurementValueLabel.getStyleClass().add("measurement-readout");
        Label themeLabel = new Label("Theme");
        themeLabel.getStyleClass().add("status-meta");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(
                10,
                printHtmlButton,
                printHtmlOnlyButton,
                printPdfButton,
                openHtmlInBrowserButton,
                new Separator(),
                selectToolButton,
                textFieldToolButton,
                labelToolButton,
                buttonToolButton,
                markerToolButton,
                tableToolButton,
                measureToolButton,
                measurementValueLabel,
                spacer,
                themeLabel,
                themeSelector
        );
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("toolbar");
        return toolbar;
    }

    private ToggleButton createToolButton(String label, EditorTool tool, boolean selected, Node icon) {
        ToggleButton button = new ToggleButton(label);
        button.setUserData(tool);
        button.setToggleGroup(toolToggleGroup);
        button.setSelected(selected);
        applyToolbarButtonStyle(button);
        applyButtonIcon(button, icon);
        applyButtonTooltip(button, label + " tool");
        return button;
    }

    private void activateTool(EditorTool tool) {
        for (javafx.scene.control.Toggle toggle : toolToggleGroup.getToggles()) {
            if (toggle.getUserData() == tool) {
                toolToggleGroup.selectToggle(toggle);
                return;
            }
        }
        activeTool = tool;
        statusLabel.setText("Active tool: " + activeTool.name());
    }

    private Node buildInspectorPanel() {
        Label inspectorTitle = new Label("Element inspector");
        inspectorTitle.getStyleClass().add("panel-title");

        Button applyTextButton = new Button("Apply text");
        applyTextButton.setOnAction(event -> applySelectedElementText());
        applyTextButton.getStyleClass().add("action-button-medium");
        applyButtonIcon(applyTextButton, ButtonIconFactory.applyTextIcon());
        applyButtonTooltip(applyTextButton, "Apply text");

        applyTableConfigButton.getStyleClass().add("action-button-medium");
        applyButtonTooltip(applyTableConfigButton, "Apply table config");

        deleteSelectedButton.setOnAction(event -> removeSelectedElement());
        deleteSelectedButton.getStyleClass().add("action-button-medium");
        applyButtonIcon(deleteSelectedButton, ButtonIconFactory.deleteIcon());
        applyButtonTooltip(deleteSelectedButton, "Delete selected element");
        applyButtonTooltip(applyElementIdButton, "Apply element ID");

        VBox panel = new VBox(
                10,
                inspectorTitle,
                new Label("Document status"),
                enableStatusWatermarkCheck,
                showMeasurementGridCheck,
                documentStatusCombo,
                new Separator(),
                new Label("Type"),
                selectedElementTypeLabel,
                new Label("ID"),
                selectedElementIdField,
                applyElementIdButton,
                new Label("Text"),
                selectedElementTextField,
                applyTextButton,
                new Separator(),
                new Label("Table width (mm)"),
                tableWidthMillimetersField,
                new Label("Table column widths (mm)"),
                tableColumnWidthsField,
                new Label("Table detail rows"),
                tableRowsCombo,
                applyTableConfigButton,
                new Separator(),
                new Label("Export DPI"),
                exportDpiCombo,
                deleteSelectedButton
        );
        panel.setPadding(new Insets(12));
        panel.setMinWidth(240);
        panel.setPrefWidth(240);
        panel.getStyleClass().add("side-panel");
        return panel;
    }

    /**
     * Estandariza el tamaño visual de botones del toolbar.
     *
     * @param button botón a estilizar.
     */
    private void applyToolbarButtonStyle(ButtonBase button) {
        button.getStyleClass().add("toolbar-button-large");
    }

    private void applyCompactToolbarButtonStyle(ButtonBase button) {
        button.getStyleClass().add("toolbar-button-compact");
    }

    /**
     * Aplica ícono y espaciado uniforme para botones con texto.
     *
     * @param button botón objetivo.
     * @param icon   gráfico a asociar.
     */
    private void applyButtonIcon(ButtonBase button, Node icon) {
        button.setGraphic(icon);
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setGraphicTextGap(6);
    }

    private void applyButtonTooltip(ButtonBase button, String text) {
        button.setTooltip(new Tooltip(text));
    }

    private Node buildStatusBar() {
        Label zoomLabel = new Label("Zoom");
        zoomLabel.getStyleClass().add("status-meta");
        documentSizeLabel.getStyleClass().add("status-meta");
        zoomValueLabel.getStyleClass().add("status-meta");
        pageLabel.getStyleClass().add("status-page-label");

        previousPageButton.setOnAction(event -> goToPage(currentPageIndex - 1));
        nextPageButton.setOnAction(event -> goToPage(currentPageIndex + 1));
        applyCompactToolbarButtonStyle(previousPageButton);
        applyCompactToolbarButtonStyle(nextPageButton);
        applyButtonIcon(previousPageButton, ButtonIconFactory.previousPageIcon());
        applyButtonIcon(nextPageButton, ButtonIconFactory.nextPageIcon());
        applyButtonTooltip(previousPageButton, "Previous page");
        applyButtonTooltip(nextPageButton, "Next page");

        HBox pageNavigation = new HBox(
                8,
                previousPageButton,
                nextPageButton,
                pageLabel
        );
        pageNavigation.setAlignment(Pos.CENTER);
        pageNavigation.getStyleClass().add("status-page-navigation");

        zoomSlider.setPrefWidth(180);

        Separator statusSeparator = new Separator(Orientation.VERTICAL);
        statusSeparator.setPrefHeight(18);

        Region leftSpacer = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        HBox statusBar = new HBox(
                12,
                statusLabel,
                leftSpacer,
                pageNavigation,
                rightSpacer,
                documentSizeLabel,
                statusSeparator,
                zoomLabel,
                zoomSlider,
                zoomValueLabel
        );
        statusBar.setPadding(new Insets(8, 12, 8, 12));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.getStyleClass().add("status-bar");
        return statusBar;
    }

    private void openPdfFile() {
        if (!confirmCloseApplication()) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select PDF document");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));

        java.io.File selectedFile = chooser.showOpenDialog(ownerStage);
        if (selectedFile == null) {
            return;
        }

        openPdfDocument(selectedFile.toPath());
    }

    private void openPdfDocument(Path pdfPath) {
        try {
            suppressChangeTracking = true;
            PdfDocumentMetadata metadata = pdfService.loadMetadata(pdfPath);
            currentProject = new OverlayProject(pdfPath, metadata);
            autoFitZoomOnNextLoad = true;
            autoFitPendingAttempts = AUTO_FIT_MAX_ATTEMPTS;
            currentHtmlPath = null;
            currentPageIndex = 0;
            hasUnsavedChanges = false;
            deletedElementsHistory.clear();
            clearSelection();
            loadCurrentPage();
            addRecentFile(pdfPath);
            statusLabel.setText("Loaded PDF: " + pdfPath.getFileName());
            LOGGER.log(Level.INFO, "PDF opened: {0}", pdfPath);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unable to open PDF", ex);
            removeRecentFile(pdfPath);
            showError("Cannot open PDF", ex.getMessage());
        } finally {
            updateButtonsState();
            suppressChangeTracking = false;
        }
    }

    private void openHtmlFile() {
        if (!confirmCloseApplication()) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open HTML overlay");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML files", "*.html", "*.htm"));

        java.io.File selectedFile = chooser.showOpenDialog(ownerStage);
        if (selectedFile == null) {
            return;
        }

        openProjectHtml(selectedFile.toPath());
    }

    private void openRecentFile(Path filePath) {
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            removeRecentFile(filePath);
            refreshRecentFilesMenu();
            showError("Recent file not found", filePath.toString());
            return;
        }
        if (!confirmCloseApplication()) {
            return;
        }

        String fileName = filePath.getFileName().toString().toLowerCase(Locale.US);
        if (fileName.endsWith(".pdf")) {
            openPdfDocument(filePath);
            return;
        }
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            openProjectHtml(filePath);
            return;
        }

        removeRecentFile(filePath);
        refreshRecentFilesMenu();
        showError("Unsupported recent file", filePath.toString());
    }

    private void openProjectHtml(Path htmlPath) {
        try {
            suppressChangeTracking = true;
            OverlayProject loadedProject = htmlExportService.loadProjectFromHtml(htmlPath);
            currentProject = loadedProject;
            normalizeElementIdsInProject();
            autoFitZoomOnNextLoad = true;
            autoFitPendingAttempts = AUTO_FIT_MAX_ATTEMPTS;
            currentHtmlPath = htmlPath;
            currentPageIndex = 0;
            hasUnsavedChanges = false;
            deletedElementsHistory.clear();
            clearSelection();
            loadCurrentPage();
            addRecentFile(htmlPath);
            statusLabel.setText("Loaded HTML: " + htmlPath.getFileName());
            LOGGER.log(Level.INFO, "HTML project opened: {0}", htmlPath);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unable to open HTML project", ex);
            removeRecentFile(htmlPath);
            showError("Cannot open HTML", ex.getMessage());
        } finally {
            updateButtonsState();
            suppressChangeTracking = false;
        }
    }

    private void newProject() {
        if (currentProject == null) {
            resetProjectState("Ready for a new document");
            return;
        }
        if (currentHtmlPath == null || hasUnsavedChanges) {
            statusLabel.setText("Save the current project before starting a new one");
            return;
        }
        if (!confirmCloseApplication()) {
            return;
        }
        resetProjectState("Current project closed. Open a PDF or project HTML.");
    }

    private void resetProjectState(String message) {
        suppressChangeTracking = true;
        try {
            currentProject = null;
            currentHtmlPath = null;
            currentPageIndex = 0;
            hasUnsavedChanges = false;
            deletedElementsHistory.clear();
            clearSelection();
            hideMeasurementOverlay();
            elementNodes.clear();
            overlayPane.getChildren().clear();
            installResizeHandles();
            installMeasurementOverlay();
            pageImageView.setImage(null);
            gridCanvas.getGraphicsContext2D().clearRect(0, 0, gridCanvas.getWidth(), gridCanvas.getHeight());
            topRulerCanvas.getGraphicsContext2D().clearRect(0, 0, topRulerCanvas.getWidth(), topRulerCanvas.getHeight());
            leftRulerCanvas.getGraphicsContext2D().clearRect(0, 0, leftRulerCanvas.getWidth(), leftRulerCanvas.getHeight());
            currentPagePixelWidth = DEFAULT_CANVAS_WIDTH;
            currentPagePixelHeight = DEFAULT_CANVAS_HEIGHT;
            overlayPane.setPrefSize(currentPagePixelWidth, currentPagePixelHeight);
            overlayPane.resize(currentPagePixelWidth, currentPagePixelHeight);
            pageStack.setPrefSize(currentPagePixelWidth, currentPagePixelHeight);
            pageLabel.setText("Page -/-");
            documentSizeLabel.setText("Document size: -");
            latestGeneratedHtmlSource = "<!-- Open a PDF or HTML project first -->";
            latestFinalSource = "<!-- Open a PDF or HTML project first -->";
            updateHtmlSourceView();
            updateFinalSourceView();
            updateButtonsState();
            statusLabel.setText(message);
        } finally {
            suppressChangeTracking = false;
        }
    }

    private void refreshRecentFilesMenu() {
        recentFilesMenu.getItems().clear();
        List<Path> recentFiles = loadRecentFiles();
        if (recentFiles.isEmpty()) {
            MenuItem emptyItem = new MenuItem("No recent files");
            emptyItem.setDisable(true);
            recentFilesMenu.getItems().add(emptyItem);
            recentFilesMenu.setDisable(true);
            return;
        }

        recentFilesMenu.setDisable(false);
        for (Path filePath : recentFiles) {
            MenuItem item = new MenuItem(formatRecentFileMenuText(filePath));
            item.setGraphic(createRecentFileIcon(filePath));
            item.setOnAction(event -> openRecentFile(filePath));
            item.setMnemonicParsing(false);
            item.setUserData(filePath);
            recentFilesMenu.getItems().add(item);
        }
        recentFilesMenu.getItems().add(new SeparatorMenuItem());
        MenuItem clearItem = new MenuItem("Clear Recent Files");
        clearItem.setGraphic(ButtonIconFactory.deleteIcon());
        clearItem.setOnAction(event -> {
            saveRecentFiles(List.of());
            refreshRecentFilesMenu();
        });
        recentFilesMenu.getItems().add(clearItem);
    }

    private Node createRecentFileIcon(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase(Locale.US);
        if (fileName.endsWith(".pdf")) {
            return ButtonIconFactory.openPdfIcon();
        }
        return ButtonIconFactory.openHtmlIcon();
    }

    private String formatRecentFileMenuText(Path filePath) {
        Path parent = filePath.getParent();
        if (parent == null) {
            return filePath.getFileName().toString();
        }
        return filePath.getFileName() + " - " + parent;
    }

    private void addRecentFile(Path filePath) {
        Path normalizedPath = normalizeRecentFilePath(filePath);
        List<Path> recentFiles = new ArrayList<>();
        recentFiles.add(normalizedPath);
        for (Path existingPath : loadRecentFiles()) {
            if (!normalizeRecentFilePath(existingPath).equals(normalizedPath)) {
                recentFiles.add(existingPath);
            }
            if (recentFiles.size() >= MAX_RECENT_FILES) {
                break;
            }
        }
        saveRecentFiles(recentFiles);
        refreshRecentFilesMenu();
    }

    private void removeRecentFile(Path filePath) {
        Path normalizedPath = normalizeRecentFilePath(filePath);
        List<Path> recentFiles = loadRecentFiles().stream()
                .filter(existingPath -> !normalizeRecentFilePath(existingPath).equals(normalizedPath))
                .toList();
        saveRecentFiles(recentFiles);
    }

    private List<Path> loadRecentFiles() {
        String rawValue = preferences.get(RECENT_FILES_PREFERENCE_KEY, "");
        if (rawValue.isBlank()) {
            return List.of();
        }

        List<Path> recentFiles = new ArrayList<>();
        for (String token : rawValue.split("\\R")) {
            String pathText = token.strip();
            if (pathText.isBlank()) {
                continue;
            }
            Path filePath = Path.of(pathText);
            if (Files.exists(filePath) && Files.isRegularFile(filePath) && isSupportedRecentFile(filePath)) {
                recentFiles.add(normalizeRecentFilePath(filePath));
            }
            if (recentFiles.size() >= MAX_RECENT_FILES) {
                break;
            }
        }
        saveRecentFiles(recentFiles);
        return recentFiles;
    }

    private void saveRecentFiles(List<Path> recentFiles) {
        String serialized = recentFiles.stream()
                .map(this::normalizeRecentFilePath)
                .distinct()
                .limit(MAX_RECENT_FILES)
                .map(Path::toString)
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("");
        preferences.put(RECENT_FILES_PREFERENCE_KEY, serialized);
    }

    private boolean isSupportedRecentFile(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase(Locale.US);
        return fileName.endsWith(".pdf") || fileName.endsWith(".html") || fileName.endsWith(".htm");
    }

    private Path normalizeRecentFilePath(Path filePath) {
        return filePath.toAbsolutePath().normalize();
    }

    private void loadCurrentPage() {
        if (currentProject == null) {
            return;
        }

        try {
            Image image = pdfService.renderPageAsFxImage(currentProject.getPdfPath(), currentPageIndex, PREVIEW_DPI);
            currentPagePixelWidth = image.getWidth();
            currentPagePixelHeight = image.getHeight();

            pageImageView.setImage(image);
            pageImageView.setFitWidth(currentPagePixelWidth);
            pageImageView.setFitHeight(currentPagePixelHeight);
            gridCanvas.setWidth(currentPagePixelWidth);
            gridCanvas.setHeight(currentPagePixelHeight);

            overlayPane.setPrefSize(currentPagePixelWidth, currentPagePixelHeight);
            overlayPane.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            overlayPane.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            overlayPane.resize(currentPagePixelWidth, currentPagePixelHeight);
            overlayPane.toFront();

            pageStack.setPrefSize(currentPagePixelWidth, currentPagePixelHeight);
            pageStack.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            pageStack.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

            renderOverlayElements();
            redrawMeasurementGuides();
            updatePageLabel();
            updateButtonsState();
            if (autoFitZoomOnNextLoad) {
                scheduleAutoFitZoom();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unable to render page", ex);
            showError("Cannot render page", ex.getMessage());
        }
    }

    private void renderOverlayElements() {
        overlayPane.getChildren().clear();
        elementNodes.clear();
        clearSelection();
        hideMeasurementOverlay();
        installResizeHandles();
        installMeasurementOverlay();

        if (currentProject == null) {
            return;
        }

        OverlayPage page = currentProject.getOverlayPage(currentPageIndex);
        for (OverlayElement element : page.mutableElements()) {
            Region node = createVisualNode(element);
            elementNodes.put(element.getId(), node);
            overlayPane.getChildren().add(node);
        }
        bringResizeHandlesToFront();
        bringMeasurementOverlayToFront();
    }

    private void redrawMeasurementGuides() {
        drawGridOverlay();
        drawRulers();
    }

    private void drawGridOverlay() {
        GraphicsContext graphics = gridCanvas.getGraphicsContext2D();
        graphics.clearRect(0, 0, gridCanvas.getWidth(), gridCanvas.getHeight());

        boolean showGrid = showMeasurementGridCheck.isSelected() && currentProject != null;
        gridCanvas.setVisible(showGrid);
        if (!showGrid) {
            return;
        }

        double pixelsPerCmX = getPixelsPerCentimeterX();
        double pixelsPerCmY = getPixelsPerCentimeterY();
        if (pixelsPerCmX <= 0.0d || pixelsPerCmY <= 0.0d) {
            return;
        }

        drawGridLines(graphics, gridCanvas.getWidth(), pixelsPerCmX, true);
        drawGridLines(graphics, gridCanvas.getHeight(), pixelsPerCmY, false);
    }

    private void drawGridLines(GraphicsContext graphics, double length, double pixelsPerCm, boolean verticalLines) {
        double minorStepPixels = pixelsPerCm * GRID_MINOR_CM;
        double majorStepPixels = pixelsPerCm * GRID_MAJOR_CM;
        double epsilon = 0.01d;
        UiTheme theme = activeTheme == null ? UiTheme.MODENA : activeTheme;

        for (double position = 0.0d; position <= length + epsilon; position += minorStepPixels) {
            boolean majorLine = isMajorMeasurement(position, majorStepPixels);
            graphics.setStroke(majorLine ? theme.getGridMajor() : theme.getGridMinor());
            graphics.setLineWidth(majorLine ? 1.0d : 0.6d);
            if (verticalLines) {
                graphics.strokeLine(position, 0, position, gridCanvas.getHeight());
            } else {
                graphics.strokeLine(0, position, gridCanvas.getWidth(), position);
            }
        }
    }

    private void drawRulers() {
        drawHorizontalRuler();
        drawVerticalRuler();
    }

    private void drawHorizontalRuler() {
        double width = Math.max(0.0d, canvasScrollPane.getViewportBounds().getWidth());
        topRulerCanvas.setWidth(width);
        topRulerCanvas.setVisible(showMeasurementGridCheck.isSelected() && currentProject != null);

        GraphicsContext graphics = topRulerCanvas.getGraphicsContext2D();
        UiTheme theme = activeTheme == null ? UiTheme.MODENA : activeTheme;
        graphics.setFill(theme.getRulerBackground());
        graphics.fillRect(0, 0, width, RULER_SIZE);
        graphics.setStroke(theme.getRulerBorder());
        graphics.strokeRect(0, 0, Math.max(0.0d, width - 0.5d), Math.max(0.0d, RULER_SIZE - 0.5d));

        if (!topRulerCanvas.isVisible()) {
            return;
        }

        double scale = pageStack.getScaleX();
        double pixelsPerCm = getPixelsPerCentimeterX() * scale;
        if (pixelsPerCm <= 0.0d) {
            return;
        }

        double contentWidth = pageStack.getBoundsInParent().getWidth();
        double viewportWidth = canvasScrollPane.getViewportBounds().getWidth();
        double scrollOffset = resolveScrollOffset(canvasScrollPane.getHvalue(), contentWidth, viewportWidth);
        drawRulerTicks(graphics, width, pixelsPerCm, scrollOffset, true);
    }

    private void drawVerticalRuler() {
        double height = Math.max(0.0d, canvasScrollPane.getViewportBounds().getHeight());
        leftRulerCanvas.setHeight(height);
        leftRulerCanvas.setVisible(showMeasurementGridCheck.isSelected() && currentProject != null);

        GraphicsContext graphics = leftRulerCanvas.getGraphicsContext2D();
        UiTheme theme = activeTheme == null ? UiTheme.MODENA : activeTheme;
        graphics.setFill(theme.getRulerBackground());
        graphics.fillRect(0, 0, RULER_SIZE, height);
        graphics.setStroke(theme.getRulerBorder());
        graphics.strokeRect(0, 0, Math.max(0.0d, RULER_SIZE - 0.5d), Math.max(0.0d, height - 0.5d));

        if (!leftRulerCanvas.isVisible()) {
            return;
        }

        double scale = pageStack.getScaleY();
        double pixelsPerCm = getPixelsPerCentimeterY() * scale;
        if (pixelsPerCm <= 0.0d) {
            return;
        }

        double contentHeight = pageStack.getBoundsInParent().getHeight();
        double viewportHeight = canvasScrollPane.getViewportBounds().getHeight();
        double scrollOffset = resolveScrollOffset(canvasScrollPane.getVvalue(), contentHeight, viewportHeight);
        drawRulerTicks(graphics, height, pixelsPerCm, scrollOffset, false);
    }

    private void drawRulerTicks(GraphicsContext graphics, double rulerLength, double pixelsPerCm,
                                double scrollOffset, boolean horizontal) {
        double minorStep = pixelsPerCm * GRID_MINOR_CM;
        double majorStep = pixelsPerCm * GRID_MAJOR_CM;
        double startMeasurement = Math.max(0.0d, Math.floor(scrollOffset / minorStep) * GRID_MINOR_CM);
        double endMeasurement = (scrollOffset + rulerLength) / pixelsPerCm;
        UiTheme theme = activeTheme == null ? UiTheme.MODENA : activeTheme;

        graphics.setFont(Font.font("Segoe UI", 10));
        graphics.setFill(theme.getRulerText());
        graphics.setStroke(theme.getRulerTick());
        graphics.setTextAlign(horizontal ? TextAlignment.CENTER : TextAlignment.LEFT);

        for (double measurementCm = startMeasurement; measurementCm <= endMeasurement + 0.01d;
             measurementCm += GRID_MINOR_CM) {
            double pixelPosition = (measurementCm * pixelsPerCm) - scrollOffset;
            boolean majorTick = isMajorMeasurement(measurementCm, GRID_MAJOR_CM);
            double tickLength = majorTick ? RULER_SIZE - 6.0d : RULER_SIZE - 14.0d;

            if (horizontal) {
                graphics.strokeLine(pixelPosition, RULER_SIZE, pixelPosition, tickLength);
                if (majorTick) {
                    graphics.fillText(formatMeasurementLabel(measurementCm), pixelPosition, 10.5d);
                }
            } else {
                graphics.strokeLine(RULER_SIZE, pixelPosition, tickLength, pixelPosition);
                if (majorTick) {
                    graphics.save();
                    graphics.translate(8.0d, pixelPosition + 9.0d);
                    graphics.rotate(-90.0d);
                    graphics.fillText(formatMeasurementLabel(measurementCm), 0, 0);
                    graphics.restore();
                }
            }
        }
    }

    private double getPixelsPerCentimeterX() {
        PdfPageMetadata pageMetadata = getCurrentPageMetadata();
        if (pageMetadata == null || pageMetadata.widthInches() <= 0.0d) {
            return 0.0d;
        }
        return currentPagePixelWidth / (pageMetadata.widthInches() * 2.54d);
    }

    private double getPixelsPerMillimeterX() {
        return getPixelsPerCentimeterX() / 10.0d;
    }

    private double getPixelsPerCentimeterY() {
        PdfPageMetadata pageMetadata = getCurrentPageMetadata();
        if (pageMetadata == null || pageMetadata.heightInches() <= 0.0d) {
            return 0.0d;
        }
        return currentPagePixelHeight / (pageMetadata.heightInches() * 2.54d);
    }

    private double getPixelsPerMillimeterY() {
        return getPixelsPerCentimeterY() / 10.0d;
    }

    private PdfPageMetadata getCurrentPageMetadata() {
        if (currentProject == null || currentPageIndex < 0 || currentPageIndex >= currentProject.getMetadata().pageCount()) {
            return null;
        }
        return currentProject.getMetadata().getPages().get(currentPageIndex);
    }

    private double getCurrentPageWidthMillimeters() {
        PdfPageMetadata pageMetadata = getCurrentPageMetadata();
        return pageMetadata == null ? 0.0d : pageMetadata.widthMillimeters();
    }

    private double getElementWidthMillimeters(OverlayElement element) {
        return Math.max(1.0d, element.getWidthRatio() * getCurrentPageWidthMillimeters());
    }

    private double resolveScrollOffset(double scrollValue, double contentLength, double viewportLength) {
        double scrollableRange = Math.max(0.0d, contentLength - viewportLength);
        return scrollValue * scrollableRange;
    }

    private boolean isMajorMeasurement(double position, double majorStep) {
        if (majorStep <= 0.0d) {
            return false;
        }
        double roundedSteps = Math.rint(position / majorStep);
        return Math.abs(position - (roundedSteps * majorStep)) < 0.01d;
    }

    private String formatMeasurementLabel(double valueCm) {
        if (Math.abs(valueCm - Math.rint(valueCm)) < 0.01d) {
            return Integer.toString((int) Math.round(valueCm));
        }
        return String.format(Locale.US, "%.1f", valueCm);
    }

    private Region createVisualNode(OverlayElement element) {
        Region node;
        String textValue = resolveDefaultText(element);

        switch (element.getType()) {
            case TEXT_FIELD -> {
                Label label = new Label(textValue);
                label.getStyleClass().add("editor-text");
                label.setAlignment(Pos.CENTER_LEFT);
                label.setPadding(new Insets(2, 4, 2, 4));
                node = label;
            }
            case LABEL -> {
                Label label = new Label(textValue);
                label.getStyleClass().add("editor-label");
                label.setAlignment(Pos.CENTER_LEFT);
                label.setPadding(new Insets(2, 4, 2, 4));
                node = label;
            }
            case BUTTON -> {
                Label label = new Label(textValue);
                label.getStyleClass().add("editor-button");
                label.setAlignment(Pos.CENTER);
                node = label;
            }
            case MARKER -> {
                Region marker = new Region();
                marker.getStyleClass().add("editor-marker");
                node = marker;
            }
            case TABLE -> node = createTablePreviewNode(element);
            default -> throw new IllegalStateException("Unexpected value: " + element.getType());
        }

        node.getStyleClass().add("overlay-node");
        node.setManaged(true);

        double minimumSize = element.getType() == OverlayElementType.MARKER ? MIN_MARKER_SIZE : MIN_RESIZABLE_SIZE;
        double nodeWidth = Math.max(minimumSize, element.getWidthRatio() * currentPagePixelWidth);
        double nodeHeight = Math.max(minimumSize, element.getHeightRatio() * currentPagePixelHeight);

        node.setPrefSize(nodeWidth, nodeHeight);
        node.setMinSize(nodeWidth, nodeHeight);
        node.setMaxSize(nodeWidth, nodeHeight);

        double x = element.getXRatio() * currentPagePixelWidth;
        double y = element.getYRatio() * currentPagePixelHeight;
        node.resizeRelocate(x, y, nodeWidth, nodeHeight);

        attachNodeHandlers(node, element);
        return node;
    }

    private Region createTablePreviewNode(OverlayElement element) {
        int columns = Math.max(1, element.getTableColumnCount());
        int detailRows = element.getTableDataRows();
        List<Double> columnWidthsMillimeters;
        try {
            columnWidthsMillimeters = parseColumnWidths(
                    element.getTableColumnWidths(),
                    columns,
                    getElementWidthMillimeters(element)
            );
        } catch (Exception ex) {
            String fallbackWidths = buildDefaultColumnWidths(columns, getElementWidthMillimeters(element));
            element.setTableColumnWidths(fallbackWidths);
            columnWidthsMillimeters = parseColumnWidths(fallbackWidths, columns, getElementWidthMillimeters(element));
        }

        GridPane tableGrid = new GridPane();
        tableGrid.getStyleClass().add("editor-table-preview");

        updateTablePreviewColumnConstraints(tableGrid, columnWidthsMillimeters);

        int totalRows = detailRows + 1;
        double rowHeightPixels = Math.max(1.0d, element.getHeightRatio() * currentPagePixelHeight / totalRows);
        for (int row = 0; row < totalRows; row++) {
            RowConstraints rowConstraints = new RowConstraints();
            rowConstraints.setMinHeight(rowHeightPixels);
            rowConstraints.setPrefHeight(rowHeightPixels);
            tableGrid.getRowConstraints().add(rowConstraints);
        }

        List<String> headerLabels = parseTableHeaders(element, columns);
        for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
            Label headerCell = new Label(headerLabels.get(columnIndex));
            headerCell.getStyleClass().add("editor-table-header-cell");
            headerCell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            headerCell.setAlignment(Pos.CENTER_LEFT);
            tableGrid.add(headerCell, columnIndex, 0);
        }

        for (int rowIndex = 1; rowIndex <= detailRows; rowIndex++) {
            for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
                Label detailCell = new Label("");
                detailCell.getStyleClass().add("editor-table-detail-cell");
                detailCell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                detailCell.setAlignment(Pos.CENTER_LEFT);
                tableGrid.add(detailCell, columnIndex, rowIndex);
            }
        }

        return tableGrid;
    }

    private Optional<Integer> requestTableColumns() {
        while (true) {
            TextInputDialog dialog = new TextInputDialog(String.valueOf(DEFAULT_TABLE_COLUMNS));
            dialog.setTitle("Insert table");
            dialog.setHeaderText("Table column count");
            dialog.setContentText("Columns (1-" + MAX_TABLE_COLUMNS + "):");
            dialog.initOwner(ownerStage);

            Optional<String> result = dialog.showAndWait();
            if (result.isEmpty()) {
                return Optional.empty();
            }

            try {
                int columnCount = Integer.parseInt(result.get().strip());
                if (columnCount < 1 || columnCount > MAX_TABLE_COLUMNS) {
                    throw new IllegalArgumentException(
                            "Column count must be between 1 and " + MAX_TABLE_COLUMNS
                    );
                }
                return Optional.of(columnCount);
            } catch (Exception ex) {
                showError("Invalid columns value", ex.getMessage());
            }
        }
    }

    private void attachNodeHandlers(Region node, OverlayElement element) {
        DragState dragState = new DragState();

        node.setOnMousePressed(event -> {
            if (activeTool != EditorTool.SELECT) {
                activateTool(EditorTool.SELECT);
                selectElement(element, node);
                statusLabel.setText("Tool switched to SELECT");
                event.consume();
                return;
            }

            selectElement(element, node);
            dragState.dragStartMouseX = event.getSceneX();
            dragState.dragStartMouseY = event.getSceneY();
            dragState.dragStartNodeX = node.getLayoutX();
            dragState.dragStartNodeY = node.getLayoutY();
            dragState.moved = false;
            event.consume();
        });

        node.setOnMouseDragged(event -> {
            if (activeTool != EditorTool.SELECT) {
                return;
            }
            double scale = pageStack.getScaleX();
            double mouseDeltaX = (event.getSceneX() - dragState.dragStartMouseX) / scale;
            double mouseDeltaY = (event.getSceneY() - dragState.dragStartMouseY) / scale;

            double nextX = dragState.dragStartNodeX + mouseDeltaX;
            double nextY = dragState.dragStartNodeY + mouseDeltaY;

            double maxX = Math.max(0, currentPagePixelWidth - node.getPrefWidth());
            double maxY = Math.max(0, currentPagePixelHeight - node.getPrefHeight());

            nextX = clamp(nextX, 0, maxX);
            nextY = clamp(nextY, 0, maxY);

            node.relocate(nextX, nextY);
            updateElementFromNode(element, node);
            positionResizeHandles();
            dragState.moved = true;
            event.consume();
        });

        node.setOnMouseReleased(event -> {
            if (dragState.moved) {
                markProjectDirty();
                statusLabel.setText("Element moved");
            }
            event.consume();
        });

        node.setOnMouseClicked(event -> {
            selectElement(element, node);
            event.consume();
        });
    }

    private void handleResizePressed(MouseEvent event, ResizeHandlePosition position) {
        if (selectedElement == null || selectedNode == null) {
            event.consume();
            return;
        }
        activeResizeState = new ResizeState(
                event.getSceneX(),
                event.getSceneY(),
                selectedNode.getLayoutX(),
                selectedNode.getLayoutY(),
                selectedNode.getPrefWidth(),
                selectedNode.getPrefHeight(),
                selectedElement.getType() == OverlayElementType.TABLE
                        ? getElementWidthMillimeters(selectedElement)
                        : 0.0d,
                selectedElement.getType() == OverlayElementType.TABLE
                        ? selectedElement.getTableColumnWidths()
                        : ""
        );
        event.consume();
    }

    private void handleResizeDragged(MouseEvent event, ResizeHandlePosition position) {
        if (activeResizeState == null || selectedElement == null || selectedNode == null) {
            event.consume();
            return;
        }

        double scale = Math.max(0.0001d, pageStack.getScaleX());
        double deltaX = (event.getSceneX() - activeResizeState.startMouseX()) / scale;
        double deltaY = (event.getSceneY() - activeResizeState.startMouseY()) / scale;

        ResizeBounds nextBounds = calculateResizeBounds(position, deltaX, deltaY, activeResizeState);
        applyNodeBounds(selectedNode, nextBounds.x(), nextBounds.y(), nextBounds.width(), nextBounds.height());
        updateElementFromNode(selectedElement, selectedNode);
        updateTableColumnWidthsAfterResize(activeResizeState);
        updateTableInspectorFields(selectedElement);
        positionResizeHandles();
        event.consume();
    }

    private void handleResizeReleased(MouseEvent event) {
        if (activeResizeState != null) {
            markProjectDirty();
            statusLabel.setText("Element resized");
            activeResizeState = null;
        }
        event.consume();
    }

    private ResizeBounds calculateResizeBounds(ResizeHandlePosition position, double deltaX, double deltaY,
                                               ResizeState resizeState) {
        double x = resizeState.startNodeX();
        double y = resizeState.startNodeY();
        double width = resizeState.startNodeWidth();
        double height = resizeState.startNodeHeight();
        double minimumSize = selectedElement != null && selectedElement.getType() == OverlayElementType.MARKER
                ? MIN_MARKER_SIZE
                : MIN_RESIZABLE_SIZE;

        if (position.resizesLeft()) {
            double proposedX = clamp(x + deltaX, 0.0d, x + width - minimumSize);
            width = width + (x - proposedX);
            x = proposedX;
        }
        if (position.resizesRight()) {
            width = clamp(width + deltaX, minimumSize, currentPagePixelWidth - x);
        }
        if (position.resizesTop()) {
            double proposedY = clamp(y + deltaY, 0.0d, y + height - minimumSize);
            height = height + (y - proposedY);
            y = proposedY;
        }
        if (position.resizesBottom()) {
            height = clamp(height + deltaY, minimumSize, currentPagePixelHeight - y);
        }

        return new ResizeBounds(x, y, width, height);
    }

    private void applyNodeBounds(Region node, double x, double y, double width, double height) {
        node.setPrefSize(width, height);
        node.setMinSize(width, height);
        node.setMaxSize(width, height);
        node.resizeRelocate(x, y, width, height);
    }

    private void updateTableColumnWidthsAfterResize(ResizeState resizeState) {
        if (selectedElement == null || selectedElement.getType() != OverlayElementType.TABLE) {
            return;
        }
        double originalTableWidth = resizeState.startTableWidthMillimeters();
        double resizedTableWidth = getElementWidthMillimeters(selectedElement);
        if (originalTableWidth <= 0.0d || resizedTableWidth <= 0.0d) {
            return;
        }

        int columns = Math.max(1, selectedElement.getTableColumnCount());
        List<Double> originalWidths = parseColumnWidths(
                resizeState.startTableColumnWidths(),
                columns,
                originalTableWidth
        );
        List<Double> resizedWidths = scaleColumnWidths(originalWidths, originalTableWidth, resizedTableWidth);
        String formattedWidths = formatColumnWidths(resizedWidths, resizedTableWidth);
        selectedElement.setTableColumnWidths(formattedWidths);
        updateSelectedTablePreviewColumnConstraints(formattedWidths, columns, resizedTableWidth);
    }

    private void updateSelectedTablePreviewColumnConstraints(String rawWidths, int columns, double tableWidthMillimeters) {
        if (!(selectedNode instanceof GridPane tableGrid)) {
            return;
        }
        List<Double> widths = parseColumnWidths(rawWidths, columns, tableWidthMillimeters);
        updateTablePreviewColumnConstraints(tableGrid, widths);
    }

    private void updateTablePreviewColumnConstraints(GridPane tableGrid, List<Double> columnWidthsMillimeters) {
        tableGrid.getColumnConstraints().clear();
        double pixelsPerMillimeter = getPixelsPerMillimeterX();
        for (double widthMillimeters : columnWidthsMillimeters) {
            ColumnConstraints constraints = new ColumnConstraints();
            double columnWidthPixels = Math.max(1.0d, widthMillimeters * pixelsPerMillimeter);
            constraints.setMinWidth(columnWidthPixels);
            constraints.setPrefWidth(columnWidthPixels);
            constraints.setMaxWidth(columnWidthPixels);
            tableGrid.getColumnConstraints().add(constraints);
        }
        tableGrid.requestLayout();
    }

    private void handleMeasurementMousePressed(MouseEvent event) {
        if (activeTool != EditorTool.MEASURE) {
            return;
        }
        if (currentProject == null) {
            statusLabel.setText("Open a PDF first");
            event.consume();
            return;
        }

        Point2D point = getClampedOverlayPoint(event);
        if (activeMeasurementState == null && isMeasurementOverlayVisible() && isPointInsideMeasurementOverlay(point)) {
            event.consume();
            return;
        }

        activeMeasurementState = new MeasurementState(point.getX(), point.getY());
        clearSelection();
        measurementRectangle.setVisible(true);
        measurementOverlayLabel.setVisible(true);
        updateMeasurementOverlay(point);
        bringMeasurementOverlayToFront();
        statusLabel.setText("Measuring W x H in millimeters");
        event.consume();
    }

    private void handleMeasurementMouseDragged(MouseEvent event) {
        if (activeTool != EditorTool.MEASURE || activeMeasurementState == null) {
            return;
        }
        updateMeasurementOverlay(getClampedOverlayPoint(event));
        event.consume();
    }

    private void handleMeasurementMouseReleased(MouseEvent event) {
        if (activeTool != EditorTool.MEASURE || activeMeasurementState == null) {
            return;
        }
        Point2D point = getClampedOverlayPoint(event);
        updateMeasurementOverlay(point);
        double measuredWidth = Math.abs(point.getX() - activeMeasurementState.startX());
        double measuredHeight = Math.abs(point.getY() - activeMeasurementState.startY());
        activeMeasurementState = null;
        if (measuredWidth < 1.0d && measuredHeight < 1.0d) {
            finishMeasurementMode("Measurement cancelled");
        } else {
            statusLabel.setText("Measurement captured: " + measurementValueLabel.getText());
        }
        event.consume();
    }

    private void handleMeasurementMouseClicked(MouseEvent event) {
        if (activeTool == EditorTool.MEASURE) {
            event.consume();
        }
    }

    private Point2D getClampedOverlayPoint(MouseEvent event) {
        Point2D localPoint = overlayPane.sceneToLocal(event.getSceneX(), event.getSceneY());
        return new Point2D(
                clamp(localPoint.getX(), 0.0d, currentPagePixelWidth),
                clamp(localPoint.getY(), 0.0d, currentPagePixelHeight)
        );
    }

    private boolean isClickOutsideMeasurementOverlay(MouseEvent event) {
        Point2D localPoint = overlayPane.sceneToLocal(event.getSceneX(), event.getSceneY());
        return !isPointInsideMeasurementOverlay(localPoint);
    }

    private boolean isPointInsideMeasurementOverlay(Point2D localPoint) {
        if (!isMeasurementOverlayVisible()) {
            return false;
        }
        return measurementRectangle.getBoundsInParent().contains(localPoint)
                || measurementOverlayLabel.getBoundsInParent().contains(localPoint);
    }

    private void updateMeasurementOverlay(Point2D currentPoint) {
        if (activeMeasurementState == null) {
            return;
        }

        double startX = activeMeasurementState.startX();
        double startY = activeMeasurementState.startY();
        double x = Math.min(startX, currentPoint.getX());
        double y = Math.min(startY, currentPoint.getY());
        double width = Math.abs(currentPoint.getX() - startX);
        double height = Math.abs(currentPoint.getY() - startY);

        measurementRectangle.resizeRelocate(x, y, Math.max(1.0d, width), Math.max(1.0d, height));
        measurementRectangle.setVisible(true);

        String measurementText = formatMeasurementDimensions(width, height);
        measurementValueLabel.setText(measurementText);
        measurementOverlayLabel.setText(measurementText);
        measurementOverlayLabel.autosize();
        positionMeasurementOverlayLabel(x, y, width, height);
        measurementOverlayLabel.setVisible(true);
    }

    private void positionMeasurementOverlayLabel(double x, double y, double width, double height) {
        double labelWidth = Math.max(110.0d, measurementOverlayLabel.prefWidth(-1));
        double labelHeight = Math.max(20.0d, measurementOverlayLabel.prefHeight(-1));
        double labelX = x + width + 6.0d;
        if (labelX + labelWidth > currentPagePixelWidth) {
            labelX = Math.max(0.0d, x - labelWidth - 6.0d);
        }
        double labelY = y + height + 6.0d;
        if (labelY + labelHeight > currentPagePixelHeight) {
            labelY = Math.max(0.0d, y - labelHeight - 6.0d);
        }
        measurementOverlayLabel.resizeRelocate(labelX, labelY, labelWidth, labelHeight);
    }

    private String formatMeasurementDimensions(double widthPixels, double heightPixels) {
        double pixelsPerMillimeterX = getPixelsPerMillimeterX();
        double pixelsPerMillimeterY = getPixelsPerMillimeterY();
        if (pixelsPerMillimeterX <= 0.0d || pixelsPerMillimeterY <= 0.0d) {
            return "W x H: -";
        }

        double widthMillimeters = widthPixels / pixelsPerMillimeterX;
        double heightMillimeters = heightPixels / pixelsPerMillimeterY;
        return "W x H: " + formatWidth(widthMillimeters) + " x " + formatWidth(heightMillimeters) + " mm";
    }

    private void handleCanvasClick(MouseEvent event) {
        if (currentProject == null) {
            statusLabel.setText("Open a PDF first");
            return;
        }
        if (activeTool == EditorTool.MEASURE) {
            return;
        }
        if (activeTool == EditorTool.SELECT) {
            clearSelection();
            return;
        }

        double x = event.getX();
        double y = event.getY();

        Integer requestedTableColumns = null;
        if (activeTool == EditorTool.TABLE) {
            Optional<Integer> optionalColumns = requestTableColumns();
            if (optionalColumns.isEmpty()) {
                statusLabel.setText("Table insertion cancelled");
                return;
            }
            requestedTableColumns = optionalColumns.get();
        }

        OverlayElement newElement = createElementFromTool(activeTool, x, y, requestedTableColumns);
        OverlayPage page = currentProject.getOverlayPage(currentPageIndex);
        page.addElement(newElement);
        markProjectDirty();

        Region node = createVisualNode(newElement);
        overlayPane.getChildren().add(node);
        elementNodes.put(newElement.getId(), node);

        selectElement(newElement, node);
        statusLabel.setText("Element inserted at x=" + (int) x + ", y=" + (int) y);
    }

    private OverlayElement createElementFromTool(EditorTool tool, double x, double y, Integer requestedTableColumns) {
        double widthPx;
        double heightPx;
        OverlayElementType type;

        switch (tool) {
            case TEXT_FIELD -> {
                type = OverlayElementType.TEXT_FIELD;
                widthPx = 180;
                heightPx = 26;
            }
            case LABEL -> {
                type = OverlayElementType.LABEL;
                widthPx = 160;
                heightPx = 24;
            }
            case BUTTON -> {
                type = OverlayElementType.BUTTON;
                widthPx = 120;
                heightPx = 30;
            }
            case MARKER -> {
                type = OverlayElementType.MARKER;
                widthPx = 10;
                heightPx = 10;
            }
            case TABLE -> {
                type = OverlayElementType.TABLE;
                widthPx = 460;
                heightPx = 160;
            }
            default -> throw new IllegalStateException("Unsupported tool: " + tool);
        }

        double maxX = Math.max(0, currentPagePixelWidth - widthPx);
        double maxY = Math.max(0, currentPagePixelHeight - heightPx);

        double safeX = clamp(x, 0, maxX);
        double safeY = clamp(y, 0, maxY);

        double xRatio = safeX / currentPagePixelWidth;
        double yRatio = safeY / currentPagePixelHeight;
        double widthRatio = widthPx / currentPagePixelWidth;
        double heightRatio = heightPx / currentPagePixelHeight;

        String nextId = generateNextElementId(type);
        OverlayElement element = new OverlayElement(type, xRatio, yRatio, widthRatio, heightRatio, defaultTextForType(type, nextId));
        element.setId(nextId);

        if (type == OverlayElementType.TABLE) {
            int tableColumns = requestedTableColumns == null ? DEFAULT_TABLE_COLUMNS : requestedTableColumns;
            element.setTableColumnCount(tableColumns);
            element.setTableDataRows(1);
            element.setTableColumnWidths(buildDefaultColumnWidths(tableColumns, getElementWidthMillimeters(element)));
            element.setText(buildDefaultTableHeaders(tableColumns));
        }
        return element;
    }

    private String defaultTextForType(OverlayElementType type, String elementId) {
        return switch (type) {
            case TEXT_FIELD -> elementId;
            case LABEL -> elementId;
            case BUTTON -> elementId;
            case MARKER -> "";
            case TABLE -> buildDefaultTableHeaders(DEFAULT_TABLE_COLUMNS);
        };
    }

    private String resolveDefaultText(OverlayElement element) {
        if (!element.getText().isBlank()) {
            return element.getText();
        }
        return defaultTextForType(element.getType(), element.getId());
    }

    private void selectElement(OverlayElement element, Region node) {
        if (selectedNode != null) {
            selectedNode.getStyleClass().remove("overlay-selected");
        }

        selectedElement = element;
        selectedNode = node;

        if (!selectedNode.getStyleClass().contains("overlay-selected")) {
            selectedNode.getStyleClass().add("overlay-selected");
        }
        showResizeHandles();

        selectedElementTypeLabel.setText(element.getType().name());
        selectedElementIdField.setText(element.getId());
        selectedElementTextField.setText(element.getText());
        selectedElementIdField.setDisable(false);
        applyElementIdButton.setDisable(false);
        deleteSelectedButton.setDisable(false);
        updateTableInspectorFields(element);
    }

    private void clearSelection() {
        if (selectedNode != null) {
            selectedNode.getStyleClass().remove("overlay-selected");
        }
        selectedElement = null;
        selectedNode = null;
        activeResizeState = null;
        hideResizeHandles();
        selectedElementTypeLabel.setText("None");
        selectedElementIdField.clear();
        selectedElementIdField.setDisable(true);
        applyElementIdButton.setDisable(true);
        selectedElementTextField.setPromptText("Text for selected element");
        selectedElementTextField.clear();
        tableWidthMillimetersField.clear();
        tableWidthMillimetersField.setDisable(true);
        tableColumnWidthsField.clear();
        tableColumnWidthsField.setDisable(true);
        tableRowsCombo.setValue(1);
        tableRowsCombo.setDisable(true);
        applyTableConfigButton.setDisable(true);
        deleteSelectedButton.setDisable(true);
    }

    private void applySelectedElementText() {
        if (selectedElement == null || selectedNode == null) {
            statusLabel.setText("Select an element first");
            return;
        }
        String newText = Optional.ofNullable(selectedElementTextField.getText()).orElse("").strip();
        selectedElement.setText(newText);
        markProjectDirty();

        if (selectedNode instanceof Label labelNode) {
            labelNode.setText(resolveDefaultText(selectedElement));
        } else if (selectedElement.getType() == OverlayElementType.TABLE) {
            refreshVisualNode(selectedElement);
        }

        updateElementFromNode(selectedElement, selectedNode);
        statusLabel.setText("Element text updated");
    }

    private void applySelectedElementId() {
        if (selectedElement == null) {
            statusLabel.setText("Select an element first");
            return;
        }

        String newId = Optional.ofNullable(selectedElementIdField.getText()).orElse("").strip();
        if (newId.isBlank()) {
            showError("Invalid ID", "ID must not be empty");
            return;
        }

        String oldId = selectedElement.getId();
        if (oldId.equals(newId)) {
            statusLabel.setText("Element ID unchanged");
            return;
        }

        if (!isElementIdAvailable(newId, selectedElement)) {
            showError("Duplicate ID", "An element with this ID already exists");
            return;
        }

        selectedElement.setId(newId);
        markProjectDirty();
        Region node = elementNodes.remove(oldId);
        if (node != null) {
            elementNodes.put(newId, node);
        }
        statusLabel.setText("Element ID updated");
    }

    private void applySelectedTableConfiguration() {
        if (selectedElement == null || selectedElement.getType() != OverlayElementType.TABLE) {
            statusLabel.setText("Select a table element first");
            return;
        }
        try {
            double originalTableWidth = getElementWidthMillimeters(selectedElement);
            String originalColumnWidths = selectedElement.getTableColumnWidths();
            String requestedColumnWidths = Optional.ofNullable(tableColumnWidthsField.getText()).orElse("").strip();
            boolean columnWidthsChanged = !requestedColumnWidths.equals(originalColumnWidths);
            boolean tableWidthChanged = false;

            String widthRaw = Optional.ofNullable(tableWidthMillimetersField.getText()).orElse("").strip();
            if (!widthRaw.isBlank()) {
                double widthMillimeters = Double.parseDouble(widthRaw);
                double pageWidthMillimeters = getCurrentPageWidthMillimeters();
                if (pageWidthMillimeters <= 0.0d) {
                    throw new IllegalArgumentException("Current page width is not available");
                }
                if (widthMillimeters <= 0.0d || widthMillimeters > pageWidthMillimeters) {
                    throw new IllegalArgumentException("Table width (mm) must be > 0 and <= page width");
                }
                tableWidthChanged = Math.abs(widthMillimeters - originalTableWidth) > 0.01d;
                selectedElement.setWidthRatio(widthMillimeters / pageWidthMillimeters);
                if (selectedNode != null) {
                    selectedNode.setPrefWidth(selectedElement.getWidthRatio() * currentPagePixelWidth);
                    selectedNode.setMinWidth(selectedNode.getPrefWidth());
                    selectedNode.setMaxWidth(selectedNode.getPrefWidth());
                    updateElementFromNode(selectedElement, selectedNode);
                }
            }

            int columns = selectedElement.getTableColumnCount();
            if (columns <= 0) {
                columns = DEFAULT_TABLE_COLUMNS;
                selectedElement.setTableColumnCount(columns);
            }

            int rows = Optional.ofNullable(tableRowsCombo.getValue()).orElse(1);
            selectedElement.setTableDataRows(rows);

            double currentTableWidth = getElementWidthMillimeters(selectedElement);
            String normalizedWidths;
            if (tableWidthChanged && !columnWidthsChanged) {
                List<Double> currentWidths = parseColumnWidths(originalColumnWidths, columns, originalTableWidth);
                List<Double> scaledWidths = scaleColumnWidths(currentWidths, originalTableWidth, currentTableWidth);
                normalizedWidths = formatColumnWidths(scaledWidths, currentTableWidth);
            } else {
                normalizedWidths = normalizeColumnWidths(
                        requestedColumnWidths,
                        columns,
                        currentTableWidth
                );
            }
            selectedElement.setTableColumnWidths(normalizedWidths);
            tableWidthMillimetersField.setText(formatWidth(currentTableWidth));
            tableColumnWidthsField.setText(normalizedWidths);

            refreshVisualNode(selectedElement);
            markProjectDirty();
            statusLabel.setText("Table configuration updated");
        } catch (Exception ex) {
            showError("Invalid table configuration", ex.getMessage());
        }
    }

    private void updateTableInspectorFields(OverlayElement element) {
        boolean isTable = element.getType() == OverlayElementType.TABLE;
        tableWidthMillimetersField.setDisable(!isTable);
        tableColumnWidthsField.setDisable(!isTable);
        tableRowsCombo.setDisable(!isTable);
        applyTableConfigButton.setDisable(!isTable);

        if (!isTable) {
            selectedElementTextField.setPromptText("Text for selected element");
            tableWidthMillimetersField.clear();
            tableColumnWidthsField.clear();
            tableRowsCombo.setValue(1);
            return;
        }

        selectedElementTextField.setPromptText("Headers separated by |");
        double tableWidthMillimeters = getElementWidthMillimeters(element);
        tableWidthMillimetersField.setText(formatWidth(tableWidthMillimeters));
        int columns = Math.max(1, element.getTableColumnCount());
        String widths = element.getTableColumnWidths();
        if (widths.isBlank()) {
            widths = buildDefaultColumnWidths(columns, tableWidthMillimeters);
            element.setTableColumnWidths(widths);
        }
        tableColumnWidthsField.setText(widths);
        tableRowsCombo.setValue(element.getTableDataRows());
    }

    private void refreshVisualNode(OverlayElement element) {
        Region oldNode = elementNodes.get(element.getId());
        if (oldNode == null) {
            return;
        }

        int childIndex = overlayPane.getChildren().indexOf(oldNode);
        Region newNode = createVisualNode(element);
        if (childIndex >= 0) {
            overlayPane.getChildren().set(childIndex, newNode);
        } else {
            overlayPane.getChildren().add(newNode);
        }
        elementNodes.put(element.getId(), newNode);
        selectElement(element, newNode);
    }

    private void removeSelectedElement() {
        if (selectedElement == null || currentProject == null) {
            return;
        }

        OverlayElement removedCopy = selectedElement.copy();
        int removedPageIndex = currentPageIndex;
        OverlayPage page = currentProject.getOverlayPage(currentPageIndex);
        boolean removed = page.removeElement(selectedElement.getId());
        Region node = elementNodes.remove(selectedElement.getId());
        if (removed && node != null) {
            deletedElementsHistory.push(new DeletedElementSnapshot(removedPageIndex, removedCopy));
            overlayPane.getChildren().remove(node);
            markProjectDirty();
            statusLabel.setText("Element removed");
        }
        clearSelection();
    }

    private void undoLastDeletion() {
        if (currentProject == null) {
            statusLabel.setText("Open a PDF first");
            return;
        }
        if (deletedElementsHistory.isEmpty()) {
            statusLabel.setText("Nothing to undo");
            return;
        }

        DeletedElementSnapshot snapshot = deletedElementsHistory.pop();
        OverlayElement restoredElement = snapshot.element().copy();
        if (!isElementIdAvailable(restoredElement.getId(), null)) {
            restoredElement.setId(generateNextElementId(restoredElement.getType()));
        }

        OverlayPage page = currentProject.getOverlayPage(snapshot.pageIndex());
        page.addElement(restoredElement);

        if (snapshot.pageIndex() == currentPageIndex) {
            Region node = createVisualNode(restoredElement);
            overlayPane.getChildren().add(node);
            elementNodes.put(restoredElement.getId(), node);
            selectElement(restoredElement, node);
        } else {
            clearSelection();
        }
        markProjectDirty();
        statusLabel.setText("Deletion undone");
    }

    private void updateElementFromNode(OverlayElement element, Region node) {
        double xRatio = node.getLayoutX() / currentPagePixelWidth;
        double yRatio = node.getLayoutY() / currentPagePixelHeight;
        double widthRatio = node.getPrefWidth() / currentPagePixelWidth;
        double heightRatio = node.getPrefHeight() / currentPagePixelHeight;

        element.setXRatio(xRatio);
        element.setYRatio(yRatio);
        element.setWidthRatio(widthRatio);
        element.setHeightRatio(heightRatio);
    }

    private void goToPage(int targetIndex) {
        if (currentProject == null) {
            return;
        }
        int pageCount = currentProject.getMetadata().pageCount();
        if (targetIndex < 0 || targetIndex >= pageCount) {
            return;
        }
        currentPageIndex = targetIndex;
        loadCurrentPage();
    }

    private void updatePageLabel() {
        if (currentProject == null) {
            pageLabel.setText("Page -/-");
            documentSizeLabel.setText("Document size: -");
            return;
        }
        int current = currentPageIndex + 1;
        int total = currentProject.getMetadata().pageCount();
        PdfPageMetadata metadata = currentProject.getMetadata().getPage(currentPageIndex);
        pageLabel.setText("Page " + current + "/" + total + " (" + formatInches(metadata) + ")");
        documentSizeLabel.setText(buildDocumentSizeInfo(currentProject.getMetadata(), metadata));
    }

    private String formatInches(PdfPageMetadata metadata) {
        return String.format("%.2f x %.2f in", metadata.widthInches(), metadata.heightInches());
    }

    private String buildDocumentSizeInfo(PdfDocumentMetadata documentMetadata, PdfPageMetadata currentPageMetadata) {
        return String.format(
                Locale.US,
                "Document: %d pages | Current page: %.2f x %.2f in (%.0f x %.0f pt)",
                documentMetadata.pageCount(),
                currentPageMetadata.widthInches(),
                currentPageMetadata.heightInches(),
                (double) currentPageMetadata.widthPoints(),
                (double) currentPageMetadata.heightPoints()
        );
    }

    private String formatZoomPercentage(double zoomPercent) {
        return String.format(Locale.US, "%.0f%%", zoomPercent);
    }

    private void scheduleAutoFitZoom() {
        Platform.runLater(this::applyAutoFitZoomIfPossible);
    }

    private void applyAutoFitZoomIfPossible() {
        if (!autoFitZoomOnNextLoad) {
            return;
        }

        if (applyFitZoomToViewport()) {
            autoFitZoomOnNextLoad = false;
            autoFitPendingAttempts = 0;
            return;
        }

        if (autoFitPendingAttempts > 0) {
            autoFitPendingAttempts--;
            Platform.runLater(this::applyAutoFitZoomIfPossible);
            return;
        }

        autoFitZoomOnNextLoad = false;
    }

    private boolean applyFitZoomToViewport() {
        if (currentPagePixelWidth <= 0 || currentPagePixelHeight <= 0) {
            return false;
        }

        Bounds viewportBounds = canvasScrollPane.getViewportBounds();
        double viewportWidth = viewportBounds == null ? 0.0d : viewportBounds.getWidth();
        double viewportHeight = viewportBounds == null ? 0.0d : viewportBounds.getHeight();
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            return false;
        }

        double fitScaleX = viewportWidth / currentPagePixelWidth;
        double fitScaleY = viewportHeight / currentPagePixelHeight;
        double fitScale = Math.min(fitScaleX, fitScaleY);
        if (!Double.isFinite(fitScale) || fitScale <= 0) {
            return false;
        }

        double fitPercent = fitScale * 100.0d;
        zoomSlider.setValue(clamp(fitPercent, zoomSlider.getMin(), zoomSlider.getMax()));
        return true;
    }

    private void normalizeElementIdsInProject() {
        if (currentProject == null) {
            return;
        }

        Map<OverlayElementType, Integer> counters = new HashMap<>();
        for (PdfPageMetadata pageMetadata : currentProject.getMetadata().getPages()) {
            OverlayPage page = currentProject.getOverlayPage(pageMetadata.pageIndex());
            for (OverlayElement element : page.mutableElements()) {
                String prefix = idPrefixForType(element.getType());
                int next = counters.getOrDefault(element.getType(), 0) + 1;
                String expectedId = prefix + next;
                element.setId(expectedId);
                counters.put(element.getType(), next);
            }
        }
    }

    private boolean isElementIdAvailable(String candidateId, OverlayElement excludedElement) {
        if (currentProject == null) {
            return true;
        }
        for (PdfPageMetadata pageMetadata : currentProject.getMetadata().getPages()) {
            OverlayPage page = currentProject.getOverlayPage(pageMetadata.pageIndex());
            for (OverlayElement element : page.mutableElements()) {
                if (excludedElement != null && element == excludedElement) {
                    continue;
                }
                if (element.getId().equalsIgnoreCase(candidateId)) {
                    return false;
                }
            }
        }
        return true;
    }

    private String generateNextElementId(OverlayElementType type) {
        String prefix = idPrefixForType(type);
        int maxSuffix = 0;

        if (currentProject != null) {
            for (PdfPageMetadata pageMetadata : currentProject.getMetadata().getPages()) {
                OverlayPage page = currentProject.getOverlayPage(pageMetadata.pageIndex());
                for (OverlayElement element : page.mutableElements()) {
                    int suffix = extractSuffix(element.getId(), prefix);
                    if (suffix > maxSuffix) {
                        maxSuffix = suffix;
                    }
                }
            }
        }
        return prefix + (maxSuffix + 1);
    }

    private String idPrefixForType(OverlayElementType type) {
        return switch (type) {
            case TEXT_FIELD -> "textbox";
            case LABEL -> "label";
            case MARKER -> "point";
            case TABLE -> "table";
            case BUTTON -> "button";
        };
    }

    private int extractSuffix(String idValue, String prefix) {
        if (idValue == null) {
            return -1;
        }
        String normalizedId = idValue.strip().toLowerCase(Locale.ROOT);
        if (!normalizedId.startsWith(prefix)) {
            return -1;
        }
        String suffixPart = normalizedId.substring(prefix.length());
        if (suffixPart.isBlank()) {
            return -1;
        }
        try {
            return Integer.parseInt(suffixPart);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private String buildDefaultTableHeaders(int columns) {
        List<String> headers = new ArrayList<>();
        for (int index = 1; index <= columns; index++) {
            headers.add("Column " + index);
        }
        return String.join("|", headers);
    }

    private String buildDefaultColumnWidths(int columns, double tableWidthMillimeters) {
        List<Double> widths = new ArrayList<>();
        double safeTableWidth = Math.max(1.0d, tableWidthMillimeters);
        double base = safeTableWidth / columns;
        double runningTotal = 0.0d;
        for (int index = 0; index < columns; index++) {
            double width = index == columns - 1 ? safeTableWidth - runningTotal : base;
            runningTotal += width;
            widths.add(width);
        }
        return formatColumnWidths(widths, safeTableWidth);
    }

    private String normalizeColumnWidths(String rawValue, int expectedColumns, double tableWidthMillimeters) {
        List<Double> widths = parseColumnWidths(rawValue, expectedColumns, tableWidthMillimeters);
        double totalWidth = widths.stream().mapToDouble(Double::doubleValue).sum();
        double maxWidth = Math.max(1.0d, tableWidthMillimeters);
        if (totalWidth > maxWidth + 0.01d) {
            throw new IllegalArgumentException("Column widths (mm) must not exceed table width");
        }
        return formatColumnWidths(widths);
    }

    private String formatColumnWidths(List<Double> widths) {
        List<String> tokens = new ArrayList<>();
        for (double width : widths) {
            tokens.add(formatWidth(width));
        }
        return String.join(",", tokens);
    }

    private String formatColumnWidths(List<Double> widths, double tableWidthMillimeters) {
        return formatColumnWidths(fitColumnWidthsToTableWidth(widths, tableWidthMillimeters));
    }

    private List<Double> scaleColumnWidths(List<Double> sourceWidths, double sourceTableWidthMillimeters,
                                           double targetTableWidthMillimeters) {
        double safeSourceWidth = Math.max(MIN_TABLE_COLUMN_WIDTH_MM, sourceTableWidthMillimeters);
        double safeTargetWidth = Math.max(MIN_TABLE_COLUMN_WIDTH_MM, targetTableWidthMillimeters);
        double ratio = safeTargetWidth / safeSourceWidth;
        List<Double> scaledWidths = new ArrayList<>();
        for (double sourceWidth : sourceWidths) {
            scaledWidths.add(Math.max(MIN_TABLE_COLUMN_WIDTH_MM, sourceWidth * ratio));
        }
        return fitColumnWidthsToTableWidth(scaledWidths, safeTargetWidth);
    }

    private List<Double> fitColumnWidthsToTableWidth(List<Double> widths, double tableWidthMillimeters) {
        if (widths.isEmpty()) {
            throw new IllegalArgumentException("Column widths count must match columns");
        }

        int columnCount = widths.size();
        double targetWidth = roundWidth(Math.max(MIN_TABLE_COLUMN_WIDTH_MM, tableWidthMillimeters));
        double minimumWidth = Math.min(MIN_TABLE_COLUMN_WIDTH_MM, targetWidth / columnCount);
        double sourceTotal = widths.stream()
                .mapToDouble(width -> Math.max(minimumWidth, width))
                .sum();

        if (sourceTotal <= 0.0d) {
            return parseColumnWidths(buildDefaultColumnWidths(columnCount, targetWidth), columnCount, targetWidth);
        }

        List<Double> fittedWidths = new ArrayList<>();
        double runningTotal = 0.0d;
        for (int index = 0; index < columnCount; index++) {
            if (index == columnCount - 1) {
                fittedWidths.add(Math.max(minimumWidth, roundWidth(targetWidth - runningTotal)));
                continue;
            }

            double proportionalWidth = widths.get(index) * targetWidth / sourceTotal;
            double remainingMinimum = (columnCount - index - 1) * minimumWidth;
            double maximumAllowed = Math.max(minimumWidth, targetWidth - runningTotal - remainingMinimum);
            double fittedWidth = clamp(roundWidth(proportionalWidth), minimumWidth, maximumAllowed);
            fittedWidths.add(fittedWidth);
            runningTotal += fittedWidth;
        }
        return fittedWidths;
    }

    private String formatWidth(double value) {
        return String.format(Locale.US, "%.2f", roundWidth(value))
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }

    private double roundWidth(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    private List<Double> parseColumnWidths(String rawValue, int expectedColumns, double tableWidthMillimeters) {
        if (expectedColumns <= 0) {
            throw new IllegalArgumentException("Table must contain at least one column");
        }

        String source = rawValue == null ? "" : rawValue.strip();
        if (source.isBlank()) {
            source = buildDefaultColumnWidths(expectedColumns, tableWidthMillimeters);
        }

        String[] tokens = source.split(",");
        if (tokens.length != expectedColumns) {
            throw new IllegalArgumentException(
                    "Column widths count must match columns (" + expectedColumns + ")"
            );
        }

        List<Double> values = new ArrayList<>();
        for (String token : tokens) {
            double width = Double.parseDouble(token.strip());
            if (width <= 0.0d) {
                throw new IllegalArgumentException("Each column width must be > 0");
            }
            values.add(width);
        }
        return values;
    }

    private List<String> parseTableHeaders(OverlayElement element, int columns) {
        String source = Optional.ofNullable(element.getText()).orElse("").strip();
        if (source.isBlank()) {
            source = buildDefaultTableHeaders(columns);
            element.setText(source);
        }

        String[] tokens = source.split("\\|", -1);
        List<String> headers = new ArrayList<>();
        for (int index = 0; index < columns; index++) {
            String header = index < tokens.length ? tokens[index].strip() : "";
            if (header.isBlank()) {
                header = "Column " + (index + 1);
            }
            headers.add(header);
        }
        return headers;
    }

    private void updateButtonsState() {
        boolean hasProject = currentProject != null;
        boolean canStartNewProject = hasProject && currentHtmlPath != null && !hasUnsavedChanges;
        newProjectMenuItem.setDisable(!canStartNewProject);
        printHtmlButton.setDisable(!hasProject);
        printHtmlOnlyButton.setDisable(!hasProject);
        printPdfButton.setDisable(!hasProject);
        openHtmlInBrowserButton.setDisable(!hasProject);
        previousPageButton.setDisable(!hasProject || currentPageIndex <= 0);
        nextPageButton.setDisable(!hasProject || currentProject != null
                && currentPageIndex >= currentProject.getMetadata().pageCount() - 1);
        deleteSelectedButton.setDisable(selectedElement == null);
        enableStatusWatermarkCheck.setDisable(!hasProject);
        if (hasProject) {
            enableStatusWatermarkCheck.setSelected(currentProject.isStatusWatermarkEnabled());
            documentStatusCombo.setValue(currentProject.getDocumentStatus());
            documentStatusCombo.setDisable(!currentProject.isStatusWatermarkEnabled());
        } else {
            enableStatusWatermarkCheck.setSelected(false);
            documentStatusCombo.setValue(DocumentStatus.DRAFT);
            documentStatusCombo.setDisable(true);
        }
    }

    private boolean saveProjectHtml() {
        if (currentProject == null) {
            statusLabel.setText("Open a PDF first");
            return false;
        }

        if (currentHtmlPath == null) {
            return saveProjectHtmlAs();
        }

        return saveProjectHtmlTo(currentHtmlPath, false);
    }

    private boolean saveProjectHtmlAs() {
        if (currentProject == null) {
            statusLabel.setText("Open a PDF first");
            return false;
        }

        Path targetHtmlPath = resolveProjectHtmlPath("Save HTML project as", "-project.html");
        if (targetHtmlPath == null) {
            statusLabel.setText("Save cancelled");
            return false;
        }

        return saveProjectHtmlTo(targetHtmlPath, true);
    }

    private boolean saveProjectHtmlTo(Path targetHtmlPath, boolean showConfirmation) {
        try {
            int selectedDpi = getSelectedExportDpi();
            Path htmlPath = htmlExportService.exportProjectAsHtml(
                    currentProject,
                    targetHtmlPath,
                    selectedDpi,
                    true,
                    ExportOptions.defaultOptions()
            );
            currentHtmlPath = htmlPath;
            hasUnsavedChanges = false;
            addRecentFile(htmlPath);
            updateButtonsState();
            statusLabel.setText("Project saved: " + htmlPath.getFileName());
            if (showConfirmation) {
                Alert info = new Alert(Alert.AlertType.INFORMATION, "Project saved at:\n" + htmlPath, ButtonType.OK);
                info.setHeaderText("Project saved");
                info.initOwner(ownerStage);
                info.showAndWait();
            }
            return true;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error saving HTML project", ex);
            showError("Cannot save HTML project", ex.getMessage());
            return false;
        }
    }

    private void installCloseHandler() {
        ownerStage.setOnCloseRequest(event -> {
            if (bypassCloseConfirmation) {
                bypassCloseConfirmation = false;
                return;
            }
            if (!confirmCloseApplication()) {
                event.consume();
            }
        });
    }

    private void requestApplicationClose() {
        if (!confirmCloseApplication()) {
            return;
        }
        bypassCloseConfirmation = true;
        ownerStage.close();
    }

    private boolean confirmCloseApplication() {
        if (!hasUnsavedChanges || currentProject == null) {
            return true;
        }

        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.YES);
        ButtonType discardButton = new ButtonType("Don't Save", ButtonBar.ButtonData.NO);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved changes");
        alert.setHeaderText("There are unsaved changes");
        alert.setContentText("Do you want to save the project before closing?");
        alert.getButtonTypes().setAll(saveButton, discardButton, ButtonType.CANCEL);
        alert.initOwner(ownerStage);

        Optional<ButtonType> selection = alert.showAndWait();
        if (selection.isEmpty() || selection.get() == ButtonType.CANCEL) {
            return false;
        }
        if (selection.get() == saveButton) {
            return saveProjectHtml();
        }
        return true;
    }

    private void markProjectDirty() {
        if (suppressChangeTracking || currentProject == null) {
            return;
        }
        hasUnsavedChanges = true;
        updateButtonsState();
        refreshActiveSourcePreview();
    }

    private void refreshActiveSourcePreview() {
        Tab selectedTab = workspaceTabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == htmlSourceTab) {
            refreshHtmlSourcePreview();
        } else if (selectedTab == finalSourceTab) {
            refreshFinalSourcePreview();
        }
    }

    private void exportErpNextFragment() {
        if (currentProject == null) {
            statusLabel.setText("Open a PDF first");
            return;
        }

        Optional<SaveExportSelection> exportSelection = showExportOptionsDialog();
        if (exportSelection.isEmpty()) {
            statusLabel.setText("Export cancelled");
            return;
        }

        Path targetHtmlPath = resolveExportHtmlPath("Export ERPNext HTML as", "-erpnext-fragment.html");
        if (targetHtmlPath == null) {
            return;
        }

        try {
            int selectedDpi = getSelectedExportDpi();
            String fragmentHtml = htmlExportService.buildEmbedHtmlFragment(
                    currentProject,
                    selectedDpi,
                    false,
                    exportSelection.get().exportOptions()
            );
            Files.writeString(targetHtmlPath, fragmentHtml);
            statusLabel.setText("ERPNext export completed: " + targetHtmlPath.getFileName());
            Alert info = new Alert(Alert.AlertType.INFORMATION, "ERPNext fragment generated at:\n" + targetHtmlPath, ButtonType.OK);
            info.setHeaderText("ERPNext export completed");
            info.initOwner(ownerStage);
            info.showAndWait();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error exporting ERPNext fragment", ex);
            showError("Cannot export ERPNext fragment", ex.getMessage());
        }
    }

    private void printPdfDocument() {
        if (currentProject == null) {
            statusLabel.setText("Open a PDF first");
            return;
        }

        try {
            boolean printed = printService.printPdf(currentProject.getPdfPath());
            statusLabel.setText(printed ? "PDF sent to printer" : "PDF print cancelled");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error printing PDF", ex);
            showError("Cannot print PDF", ex.getMessage());
        }
    }

    private void printHtmlLayer() {
        if (currentProject == null) {
            statusLabel.setText("Open a PDF first");
            return;
        }

        try {
            Path htmlPath = createTemporaryStandaloneHtml("pdf-overlay-print-with-background-", true);

            printService.printHtml(
                    htmlPath,
                    ownerStage,
                    printed -> statusLabel.setText(printed
                            ? "HTML with PDF background sent to printer"
                            : "HTML print cancelled"),
                    errorMessage -> {
                        statusLabel.setText("HTML print failed");
                        showError("Cannot print HTML", errorMessage);
                    }
            );
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error printing HTML", ex);
            showError("Cannot print HTML", ex.getMessage());
        }
    }

    private void printHtmlOnlyLayer() {
        if (currentProject == null) {
            statusLabel.setText("Open a PDF first");
            return;
        }

        try {
            Path htmlPath = createTemporaryStandaloneHtml("pdf-overlay-print-html-only-", false);

            printService.printHtml(
                    htmlPath,
                    ownerStage,
                    printed -> statusLabel.setText(printed
                            ? "HTML overlay sent to printer"
                            : "HTML overlay print cancelled"),
                    errorMessage -> {
                        statusLabel.setText("HTML overlay print failed");
                        showError("Cannot print HTML overlay", errorMessage);
                    }
            );
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error printing HTML overlay", ex);
            showError("Cannot print HTML overlay", ex.getMessage());
        }
    }

    private void openHtmlOnlyInBrowser() {
        if (currentProject == null) {
            statusLabel.setText("Open a PDF first");
            return;
        }

        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            showError("Cannot open browser", "Desktop browser integration is not available in this environment.");
            return;
        }

        try {
            Path htmlPath = createTemporaryStandaloneHtml("pdf-overlay-browser-html-only-", false);
            Desktop.getDesktop().browse(htmlPath.toUri());
            statusLabel.setText("HTML overlay opened in default browser");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error opening HTML overlay in browser", ex);
            showError("Cannot open browser", ex.getMessage());
        }
    }

    private Path createTemporaryStandaloneHtml(String filePrefix, boolean includePdfBackground) throws IOException {
        Path tempHtmlPath = Files.createTempFile(filePrefix, ".html");
        String browserHtml = htmlExportService.buildStandaloneBrowserHtml(
                currentProject,
                getSelectedExportDpi(),
                includePdfBackground,
                ExportOptions.defaultOptions()
        );
        Files.writeString(tempHtmlPath, browserHtml, StandardCharsets.UTF_8);
        return tempHtmlPath;
    }

    private int getSelectedExportDpi() {
        return exportDpiCombo.getValue() == null ? 300 : exportDpiCombo.getValue();
    }

    private void refreshHtmlSourcePreview() {
        if (currentProject == null) {
            latestGeneratedHtmlSource = "<!-- Open a PDF or HTML project first -->";
            updateHtmlSourceView();
            return;
        }
        try {
            latestGeneratedHtmlSource = htmlExportService.buildHtmlContent(
                    currentProject,
                    getSelectedExportDpi(),
                    false,
                    ExportOptions.defaultOptions()
            );
            updateHtmlSourceView();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error generating HTML source preview", ex);
            latestGeneratedHtmlSource = "<!-- Error generating HTML source: " + ex.getMessage() + " -->";
            updateHtmlSourceView();
        }
    }

    private void refreshFinalSourcePreview() {
        if (currentProject == null) {
            latestFinalSource = "<!-- Open a PDF or HTML project first -->";
            updateFinalSourceView();
            return;
        }
        try {
            latestFinalSource = htmlExportService.buildEmbedHtmlFragment(
                    currentProject,
                    getSelectedExportDpi(),
                    false,
                    ExportOptions.defaultOptions()
            );
            updateFinalSourceView();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error generating final source preview", ex);
            latestFinalSource = "<!-- Error generating final source: " + ex.getMessage() + " -->";
            updateFinalSourceView();
        }
    }

    /**
     * Actualiza el contenido mostrado en el visor de código con resaltado HTML/CSS.
     */
    private void updateHtmlSourceView() {
        String source = latestGeneratedHtmlSource == null ? "" : latestGeneratedHtmlSource;
        String selectedBlock = Optional.ofNullable(sourceBlockSelector.getValue()).orElse(SOURCE_BLOCK_FULL);
        String blockContent = extractHtmlBlock(source, selectedBlock);
        String highlightedHtml = buildHighlightedSourceDocument(blockContent, selectedBlock);
        htmlSourceWebView.getEngine().loadContent(highlightedHtml, "text/html");
    }

    private void updateFinalSourceView() {
        String source = latestFinalSource == null ? "" : latestFinalSource;
        String plainHtml = buildPlainSourceDocument(source);
        finalSourceWebView.getEngine().loadContent(plainHtml, "text/html");
    }

    /**
     * Extrae una sección del documento HTML según el bloque seleccionado.
     *
     * @param htmlSource     código HTML completo.
     * @param selectedBlock  bloque seleccionado en UI.
     * @return contenido textual del bloque solicitado.
     */
    private String extractHtmlBlock(String htmlSource, String selectedBlock) {
        return switch (selectedBlock) {
            case SOURCE_BLOCK_HEAD -> extractFirstGroup(HTML_HEAD_PATTERN, htmlSource, "<!-- HEAD not found -->");
            case SOURCE_BLOCK_STYLE -> extractAllMatches(HTML_STYLE_PATTERN, htmlSource, "<!-- STYLE not found -->");
            case SOURCE_BLOCK_BODY -> extractFirstGroup(HTML_BODY_PATTERN, htmlSource, "<!-- BODY not found -->");
            case SOURCE_BLOCK_METADATA -> extractFullMatch(
                    OVERLAY_METADATA_PATTERN,
                    htmlSource,
                    "<!-- Metadata block not found -->"
            );
            default -> htmlSource;
        };
    }

    /**
     * Obtiene el primer grupo capturado por un patrón.
     *
     * @param pattern      patrón de búsqueda.
     * @param content      contenido fuente.
     * @param fallbackText texto por defecto cuando no hay coincidencias.
     * @return bloque capturado o texto fallback.
     */
    private String extractFirstGroup(Pattern pattern, String content, String fallbackText) {
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find() || matcher.groupCount() < 1) {
            return fallbackText;
        }
        return matcher.group(1).trim();
    }

    /**
     * Concatena todas las coincidencias completas de un patrón.
     *
     * @param pattern      patrón de búsqueda.
     * @param content      contenido fuente.
     * @param fallbackText texto por defecto cuando no hay coincidencias.
     * @return texto concatenado con salto de línea entre coincidencias.
     */
    private String extractAllMatches(Pattern pattern, String content, String fallbackText) {
        Matcher matcher = pattern.matcher(content);
        List<String> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.group().trim());
        }
        if (matches.isEmpty()) {
            return fallbackText;
        }
        return String.join("\n\n", matches);
    }

    /**
     * Obtiene la primera coincidencia completa de un patrón.
     *
     * @param pattern      patrón de búsqueda.
     * @param content      contenido fuente.
     * @param fallbackText texto por defecto cuando no hay coincidencias.
     * @return coincidencia completa o fallback.
     */
    private String extractFullMatch(Pattern pattern, String content, String fallbackText) {
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            return fallbackText;
        }
        return matcher.group().trim();
    }

    /**
     * Construye el documento HTML mostrado dentro del WebView para presentar código coloreado.
     *
     * @param sourceCode    bloque de código a renderizar.
     * @param selectedBlock nombre del bloque activo.
     * @return documento HTML listo para cargar en el WebView.
     */
    private String buildHighlightedSourceDocument(String sourceCode, String selectedBlock) {
        String escaped = escapeHtml(sourceCode);
        String highlighted = applyBasicSyntaxHighlight(escaped);
        String title = "Block: " + escapeHtml(selectedBlock);
        String bodyBackground = activeTheme != null && activeTheme.isDark() ? "#151a20" : "#ffffff";
        String bodyColor = activeTheme != null && activeTheme.isDark() ? "#e7edf5" : "#212529";
        String headerBackground = activeTheme != null && activeTheme.isDark() ? "#202833" : "#f3f5f7";
        String headerBorder = activeTheme != null && activeTheme.isDark() ? "#4a5667" : "#dde2e7";
        String headerColor = activeTheme != null && activeTheme.isDark() ? "#b8c7d9" : "#445061";
        String tagColor = activeTheme != null && activeTheme.isDark() ? "#8fc1ff" : "#0033b3";
        String attrColor = activeTheme != null && activeTheme.isDark() ? "#ffcf88" : "#7a3e00";
        String valueColor = activeTheme != null && activeTheme.isDark() ? "#9fe6a8" : "#067d17";
        String commentColor = activeTheme != null && activeTheme.isDark() ? "#8b98aa" : "#6f7b8a";
        String jinjaColor = activeTheme != null && activeTheme.isDark() ? "#d4a6ff" : "#8a2be2";
        return """
                <!doctype html>
                <html>
                <head>
                  <meta charset="UTF-8">
                  <style>
                    :root {
                      color-scheme: light;
                    }
                    body {
                      margin: 0;
                      background: %s;
                      color: %s;
                      font-family: Consolas, Menlo, Monaco, monospace;
                    }
                    .header {
                      position: sticky;
                      top: 0;
                      z-index: 1;
                      padding: 8px 12px;
                      background: %s;
                      border-bottom: 1px solid %s;
                      color: %s;
                      font-size: 12px;
                      font-weight: 600;
                    }
                    pre {
                      margin: 0;
                      padding: 12px;
                      line-height: 1.35;
                      font-size: 12px;
                      white-space: pre;
                      overflow: auto;
                    }
                    .tok-tag { color: %s; }
                    .tok-attr { color: %s; }
                    .tok-val { color: %s; }
                    .tok-comment { color: %s; font-style: italic; }
                    .tok-jinja { color: %s; font-weight: 600; }
                  </style>
                </head>
                <body>
                  <div class="header">%s</div>
                  <pre>%s</pre>
                </body>
                </html>
                """.formatted(
                bodyBackground,
                bodyColor,
                headerBackground,
                headerBorder,
                headerColor,
                tagColor,
                attrColor,
                valueColor,
                commentColor,
                jinjaColor,
                title,
                highlighted
        );
    }

    private String buildPlainSourceDocument(String sourceCode) {
        String escaped = escapeHtml(sourceCode);
        String bodyBackground = activeTheme != null && activeTheme.isDark() ? "#151a20" : "#ffffff";
        String bodyColor = activeTheme != null && activeTheme.isDark() ? "#e7edf5" : "#212529";
        return """
                <!doctype html>
                <html>
                <head>
                  <meta charset="UTF-8">
                  <style>
                    :root {
                      color-scheme: light;
                    }
                    body {
                      margin: 0;
                      background: %s;
                      color: %s;
                      font-family: Consolas, Menlo, Monaco, monospace;
                    }
                    pre {
                      margin: 0;
                      padding: 12px;
                      line-height: 1.35;
                      font-size: 12px;
                      white-space: pre-wrap;
                      overflow-wrap: normal;
                      word-break: normal;
                    }
                  </style>
                </head>
                <body>
                  <pre>%s</pre>
                </body>
                </html>
                """.formatted(
                bodyBackground,
                bodyColor,
                escaped
        );
    }

    private void applyTheme(UiTheme theme) {
        if (theme == null) {
            return;
        }

        activeTheme = theme;
        Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());
        root.getStyleClass().removeAll(
                UiTheme.MODENA.getRootStyleClass(),
                UiTheme.CASPIAN.getRootStyleClass(),
                UiTheme.METAL.getRootStyleClass(),
                UiTheme.NIMBUS.getRootStyleClass(),
                UiTheme.WINDOWS.getRootStyleClass(),
                UiTheme.WINDOWS_CLASSIC.getRootStyleClass(),
                UiTheme.MOTIF.getRootStyleClass(),
                UiTheme.DARK.getRootStyleClass()
        );
        root.getStyleClass().add(theme.getRootStyleClass());
        redrawMeasurementGuides();
        if (workspaceTabPane.getSelectionModel().getSelectedItem() == htmlSourceTab) {
            updateHtmlSourceView();
        } else if (workspaceTabPane.getSelectionModel().getSelectedItem() == finalSourceTab) {
            updateFinalSourceView();
        }
    }

    /**
     * Escapa caracteres especiales para representar el código fuente en HTML.
     *
     * @param rawSource texto original.
     * @return texto escapado para `pre`.
     */
    private String escapeHtml(String rawSource) {
        return rawSource
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * Aplica resaltado básico para etiquetas HTML, atributos, valores, comentarios y bloques Jinja.
     *
     * @param escapedSource código escapado.
     * @return código escapado con etiquetas `span` para colores.
     */
    private String applyBasicSyntaxHighlight(String escapedSource) {
        String highlighted = escapedSource;
        highlighted = highlighted.replaceAll("(?s)&lt;!--.*?--&gt;", "<span class=\"tok-comment\">$0</span>");
        highlighted = highlighted.replaceAll("\\{\\%.*?\\%\\}", "<span class=\"tok-jinja\">$0</span>");
        highlighted = highlighted.replaceAll("\\{\\{.*?\\}\\}", "<span class=\"tok-jinja\">$0</span>");
        highlighted = highlighted.replaceAll("(&lt;/?)([a-zA-Z][a-zA-Z0-9:_-]*)", "$1<span class=\"tok-tag\">$2</span>");
        highlighted = highlighted.replaceAll(
                "([a-zA-Z_:][-a-zA-Z0-9_:.]*)(\\s*=\\s*)(\"[^\"]*\"|'[^']*')",
                "<span class=\"tok-attr\">$1</span>$2<span class=\"tok-val\">$3</span>"
        );
        return highlighted;
    }

    private Optional<SaveExportSelection> showExportOptionsDialog() {
        Dialog<SaveExportSelection> dialog = new Dialog<>();
        dialog.setTitle("Export options");
        dialog.setHeaderText("Select HTML export options");
        dialog.initOwner(ownerStage);

        ButtonType exportButtonType = new ButtonType("Export", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(exportButtonType, ButtonType.CANCEL);

        CheckBox exportFontCheck = new CheckBox("General: export font");
        exportFontCheck.setSelected(true);
        CheckBox exportTableColorsCheck = new CheckBox("Tables: export colors");
        exportTableColorsCheck.setSelected(false);
        CheckBox exportTableBordersCheck = new CheckBox("Tables: export borders");
        exportTableBordersCheck.setSelected(true);
        CheckBox exportTextBordersCheck = new CheckBox("Text fields: export borders");
        exportTextBordersCheck.setSelected(true);

        VBox content = new VBox(
                8,
                exportFontCheck,
                exportTableColorsCheck,
                exportTableBordersCheck,
                exportTextBordersCheck
        );
        content.setPadding(new Insets(8, 4, 4, 4));
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == exportButtonType) {
                return new SaveExportSelection(
                        new ExportOptions(
                                exportFontCheck.isSelected(),
                                exportTableColorsCheck.isSelected(),
                                exportTableBordersCheck.isSelected(),
                                exportTextBordersCheck.isSelected()
                        )
                );
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private Path resolveProjectHtmlPath(String dialogTitle, String defaultSuffix) {
        Path initialPath = currentHtmlPath;
        if (initialPath == null && currentProject != null) {
            initialPath = currentProject.getPdfPath();
        }
        return resolveHtmlPath(dialogTitle, defaultSuffix, initialPath);
    }

    private Path resolveExportHtmlPath(String dialogTitle, String defaultSuffix) {
        Path initialPath = currentProject == null ? null : currentProject.getPdfPath();
        return resolveHtmlPath(dialogTitle, defaultSuffix, initialPath);
    }

    private Path resolveHtmlPath(String dialogTitle, String defaultSuffix, Path initialPath) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(dialogTitle);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML files", "*.html"));

        if (initialPath != null) {
            Path parent = initialPath.toAbsolutePath().getParent();
            if (parent != null && Files.exists(parent)) {
                chooser.setInitialDirectory(parent.toFile());
            }

            String initialFileName = initialPath.getFileName().toString();
            if (initialFileName.toLowerCase(Locale.US).endsWith(".html")
                    || initialFileName.toLowerCase(Locale.US).endsWith(".htm")) {
                chooser.setInitialFileName(initialFileName);
            } else {
                chooser.setInitialFileName(stripExtension(initialFileName) + defaultSuffix);
            }
        }

        java.io.File selectedFile = chooser.showSaveDialog(ownerStage);
        if (selectedFile == null) {
            return null;
        }

        Path targetPath = selectedFile.toPath();
        String fileName = targetPath.getFileName().toString().toLowerCase();
        if (!fileName.endsWith(".html") && !fileName.endsWith(".htm")) {
            targetPath = targetPath.resolveSibling(targetPath.getFileName() + ".html");
        }
        return targetPath;
    }

    private String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0) {
            return fileName;
        }
        return fileName.substring(0, dotIndex);
    }

    private void showError(String title, String details) {
        Alert alert = new Alert(Alert.AlertType.ERROR, details, ButtonType.OK);
        alert.setHeaderText(title);
        alert.initOwner(ownerStage);
        alert.showAndWait();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Estado temporal para operaciones de arrastre de elementos.
     */
    private static final class DragState {
        private double dragStartMouseX;
        private double dragStartMouseY;
        private double dragStartNodeX;
        private double dragStartNodeY;
        private boolean moved;
    }

    /**
     * Estado temporal para operaciones de resize.
     */
    private record ResizeState(double startMouseX,
                               double startMouseY,
                               double startNodeX,
                               double startNodeY,
                               double startNodeWidth,
                               double startNodeHeight,
                               double startTableWidthMillimeters,
                               String startTableColumnWidths) {
    }

    private record ResizeBounds(double x, double y, double width, double height) {
    }

    private record MeasurementState(double startX, double startY) {
    }

    private enum ResizeAnchor {
        START,
        CENTER,
        END
    }

    private enum ResizeHandlePosition {
        NORTH_WEST(ResizeAnchor.START, ResizeAnchor.START, Cursor.NW_RESIZE),
        NORTH(ResizeAnchor.CENTER, ResizeAnchor.START, Cursor.N_RESIZE),
        NORTH_EAST(ResizeAnchor.END, ResizeAnchor.START, Cursor.NE_RESIZE),
        EAST(ResizeAnchor.END, ResizeAnchor.CENTER, Cursor.E_RESIZE),
        SOUTH_EAST(ResizeAnchor.END, ResizeAnchor.END, Cursor.SE_RESIZE),
        SOUTH(ResizeAnchor.CENTER, ResizeAnchor.END, Cursor.S_RESIZE),
        SOUTH_WEST(ResizeAnchor.START, ResizeAnchor.END, Cursor.SW_RESIZE),
        WEST(ResizeAnchor.START, ResizeAnchor.CENTER, Cursor.W_RESIZE);

        private final ResizeAnchor horizontalAnchor;
        private final ResizeAnchor verticalAnchor;
        private final Cursor cursor;

        ResizeHandlePosition(ResizeAnchor horizontalAnchor, ResizeAnchor verticalAnchor, Cursor cursor) {
            this.horizontalAnchor = horizontalAnchor;
            this.verticalAnchor = verticalAnchor;
            this.cursor = cursor;
        }

        private ResizeAnchor horizontalAnchor() {
            return horizontalAnchor;
        }

        private ResizeAnchor verticalAnchor() {
            return verticalAnchor;
        }

        private Cursor cursor() {
            return cursor;
        }

        private boolean resizesLeft() {
            return horizontalAnchor == ResizeAnchor.START;
        }

        private boolean resizesRight() {
            return horizontalAnchor == ResizeAnchor.END;
        }

        private boolean resizesTop() {
            return verticalAnchor == ResizeAnchor.START;
        }

        private boolean resizesBottom() {
            return verticalAnchor == ResizeAnchor.END;
        }
    }

    /**
     * Snapshot mínimo para restaurar un elemento eliminado.
     */
    private record DeletedElementSnapshot(int pageIndex, OverlayElement element) {
    }

    /**
     * Selección de opciones específicas para guardado de HTML.
     */
    private record SaveExportSelection(ExportOptions exportOptions) {
    }
}
