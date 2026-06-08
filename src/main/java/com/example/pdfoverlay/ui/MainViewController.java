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
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    private static final double ZOOM_STEP_PERCENT = 10.0d;
    private static final int AUTO_FIT_MAX_ATTEMPTS = 16;
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
    private final WebView htmlSourceWebView;
    private final ComboBox<String> sourceBlockSelector;

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
    private final TextField tableWidthPercentField;
    private final TextField tableColumnWidthsField;
    private final ComboBox<Integer> tableRowsCombo;
    private final Button applyTableConfigButton;
    private final ComboBox<Integer> exportDpiCombo;
    private final Slider zoomSlider;

    private final Button openHtmlButton;
    private final Button exportButton;
    private final Button exportErpNextButton;
    private final Button printHtmlButton;
    private final Button printPdfButton;
    private final Button previousPageButton;
    private final Button nextPageButton;
    private final Button deleteSelectedButton;

    private final Map<String, Region> elementNodes;
    private final Deque<DeletedElementSnapshot> deletedElementsHistory;

    private EditorTool activeTool;
    private OverlayProject currentProject;
    private Path currentHtmlPath;
    private int currentPageIndex;
    private OverlayElement selectedElement;
    private Region selectedNode;
    private boolean autoFitZoomOnNextLoad;
    private int autoFitPendingAttempts;
    private String latestGeneratedHtmlSource;

    private double currentPagePixelWidth;
    private double currentPagePixelHeight;

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
        this.htmlSourceWebView = new WebView();
        this.sourceBlockSelector = new ComboBox<>();

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
        this.tableWidthPercentField = new TextField();
        this.tableColumnWidthsField = new TextField();
        this.tableRowsCombo = new ComboBox<>();
        this.applyTableConfigButton = new Button("Apply table config");
        this.exportDpiCombo = new ComboBox<>();
        this.zoomSlider = new Slider(0.0, 300.0, 100.0);

        this.openHtmlButton = new Button("Open HTML");
        this.exportButton = new Button("Save Project As...");
        this.exportErpNextButton = new Button("Export ERPNext...");
        this.printHtmlButton = new Button("Print HTML");
        this.printPdfButton = new Button("Print PDF");
        this.previousPageButton = new Button("< Prev");
        this.nextPageButton = new Button("Next >");
        this.deleteSelectedButton = new Button("Delete selected");

        this.elementNodes = new HashMap<>();
        this.deletedElementsHistory = new ArrayDeque<>();
        this.selectedElementIdField.setDisable(true);
        this.applyElementIdButton.setDisable(true);

        this.activeTool = EditorTool.SELECT;
        this.currentPageIndex = 0;
        this.currentHtmlPath = null;
        this.currentPagePixelWidth = DEFAULT_CANVAS_WIDTH;
        this.currentPagePixelHeight = DEFAULT_CANVAS_HEIGHT;
        this.autoFitZoomOnNextLoad = false;
        this.autoFitPendingAttempts = 0;
        this.latestGeneratedHtmlSource = "<!-- Open a PDF or HTML project first -->";

        configureCanvas();
        configureActions();
        zoomValueLabel.setText(formatZoomPercentage(zoomSlider.getValue()));

        root.setTop(buildTopArea());
        root.setCenter(buildWorkspacePane());
        root.setRight(buildInspectorPanel());
        root.setBottom(buildStatusBar());
        root.setFocusTraversable(true);

        updateButtonsState();
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

    private void configureActions() {
        root.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.DELETE) {
                removeSelectedElement();
                event.consume();
                return;
            }

            if (event.isShortcutDown()) {
                switch (event.getCode()) {
                    case Q -> {
                        Platform.exit();
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
            statusLabel.setText(newValue
                    ? "Status watermark enabled: " + currentProject.getDocumentStatus().getWatermarkText()
                    : "Status watermark disabled");
            if (workspaceTabPane.getSelectionModel().getSelectedItem() == htmlSourceTab) {
                refreshHtmlSourcePreview();
            }
        });
        documentStatusCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (currentProject == null || newValue == null) {
                return;
            }
            currentProject.setDocumentStatus(newValue);
            statusLabel.setText("Document status: " + newValue.getWatermarkText());
            if (workspaceTabPane.getSelectionModel().getSelectedItem() == htmlSourceTab) {
                refreshHtmlSourcePreview();
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
        tableWidthPercentField.setPromptText("Example: 55");
        tableColumnWidthsField.setPromptText("Example: 20,30,25,25");
        tableRowsCombo.getItems().addAll(1, 4);
        tableRowsCombo.setValue(1);
        applyTableConfigButton.setOnAction(event -> applySelectedTableConfiguration());

        workspaceTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == htmlSourceTab) {
                refreshHtmlSourcePreview();
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
                statusLabel.setText("Active tool: " + activeTool.name());
            }
        });

        canvasScrollPane.addEventFilter(ScrollEvent.SCROLL, this::handleZoomScroll);
        canvasScrollPane.hvalueProperty().addListener((obs, oldValue, newValue) -> redrawMeasurementGuides());
        canvasScrollPane.vvalueProperty().addListener((obs, oldValue, newValue) -> redrawMeasurementGuides());
        canvasScrollPane.viewportBoundsProperty().addListener((obs, oldValue, newValue) -> redrawMeasurementGuides());
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

        workspaceTabPane.getTabs().setAll(graphicTab, htmlSourceTab);
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
        MenuItem openPdfMenuItem = new MenuItem("Open PDF...");
        openPdfMenuItem.setOnAction(event -> openPdfFile());

        MenuItem openProjectMenuItem = new MenuItem("Open Project HTML...");
        openProjectMenuItem.setOnAction(event -> openHtmlFile());

        MenuItem saveProjectMenuItem = new MenuItem("Save Project As...");
        saveProjectMenuItem.setOnAction(event -> saveProjectHtml());

        MenuItem exportErpNextMenuItem = new MenuItem("Export ERPNext...");
        exportErpNextMenuItem.setOnAction(event -> exportErpNextFragment());

        MenuItem printHtmlMenuItem = new MenuItem("Print HTML...");
        printHtmlMenuItem.setOnAction(event -> printHtmlLayer());

        MenuItem printPdfMenuItem = new MenuItem("Print PDF...");
        printPdfMenuItem.setOnAction(event -> printPdfDocument());

        MenuItem exitMenuItem = new MenuItem("Exit");
        exitMenuItem.setOnAction(event -> Platform.exit());

        Menu fileMenu = new Menu("File");
        fileMenu.getItems().addAll(
                openPdfMenuItem,
                openProjectMenuItem,
                new SeparatorMenuItem(),
                saveProjectMenuItem,
                exportErpNextMenuItem,
                new SeparatorMenuItem(),
                printHtmlMenuItem,
                printPdfMenuItem,
                new SeparatorMenuItem(),
                exitMenuItem
        );

        MenuBar menuBar = new MenuBar(fileMenu);
        return menuBar;
    }

    private Node buildTopToolbar() {
        Button openButton = new Button("Open PDF");
        openButton.setOnAction(event -> openPdfFile());
        applyToolbarButtonStyle(openButton);
        applyButtonIcon(openButton, ButtonIconFactory.openPdfIcon());

        openHtmlButton.setOnAction(event -> openHtmlFile());
        exportButton.setOnAction(event -> saveProjectHtml());
        exportErpNextButton.setOnAction(event -> exportErpNextFragment());
        printHtmlButton.setOnAction(event -> printHtmlLayer());
        printPdfButton.setOnAction(event -> printPdfDocument());
        applyToolbarButtonStyle(openHtmlButton);
        applyToolbarButtonStyle(exportButton);
        applyToolbarButtonStyle(exportErpNextButton);
        applyToolbarButtonStyle(printHtmlButton);
        applyToolbarButtonStyle(printPdfButton);
        applyButtonIcon(openHtmlButton, ButtonIconFactory.openHtmlIcon());
        applyButtonIcon(exportButton, ButtonIconFactory.saveHtmlIcon());
        applyButtonIcon(exportErpNextButton, ButtonIconFactory.saveHtmlIcon());
        applyButtonIcon(printHtmlButton, ButtonIconFactory.printHtmlIcon());
        applyButtonIcon(printPdfButton, ButtonIconFactory.printPdfIcon());

        previousPageButton.setOnAction(event -> goToPage(currentPageIndex - 1));
        nextPageButton.setOnAction(event -> goToPage(currentPageIndex + 1));
        applyToolbarButtonStyle(previousPageButton);
        applyToolbarButtonStyle(nextPageButton);
        applyButtonIcon(previousPageButton, ButtonIconFactory.previousPageIcon());
        applyButtonIcon(nextPageButton, ButtonIconFactory.nextPageIcon());

        ToggleButton selectToolButton = createToolButton("Select", EditorTool.SELECT, true, ButtonIconFactory.selectToolIcon());
        ToggleButton textFieldToolButton = createToolButton("Text", EditorTool.TEXT_FIELD, false, ButtonIconFactory.textToolIcon());
        ToggleButton labelToolButton = createToolButton("Label", EditorTool.LABEL, false, ButtonIconFactory.labelToolIcon());
        ToggleButton buttonToolButton = createToolButton("Button", EditorTool.BUTTON, false, ButtonIconFactory.buttonToolIcon());
        ToggleButton markerToolButton = createToolButton("Point", EditorTool.MARKER, false, ButtonIconFactory.markerToolIcon());
        ToggleButton tableToolButton = createToolButton("Table", EditorTool.TABLE, false, ButtonIconFactory.tableToolIcon());

        HBox toolbar = new HBox(
                10,
                openButton,
                openHtmlButton,
                exportButton,
                exportErpNextButton,
                printHtmlButton,
                printPdfButton,
                new Separator(),
                previousPageButton,
                nextPageButton,
                pageLabel,
                new Separator(),
                selectToolButton,
                textFieldToolButton,
                labelToolButton,
                buttonToolButton,
                markerToolButton,
                tableToolButton
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

        applyTableConfigButton.getStyleClass().add("action-button-medium");

        deleteSelectedButton.setOnAction(event -> removeSelectedElement());
        deleteSelectedButton.getStyleClass().add("action-button-medium");
        applyButtonIcon(deleteSelectedButton, ButtonIconFactory.deleteIcon());

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
                new Label("Table width (%)"),
                tableWidthPercentField,
                new Label("Table column widths (%)"),
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

    private Node buildStatusBar() {
        Label zoomLabel = new Label("Zoom");
        zoomLabel.getStyleClass().add("status-meta");
        documentSizeLabel.getStyleClass().add("status-meta");
        zoomValueLabel.getStyleClass().add("status-meta");

        zoomSlider.setPrefWidth(180);

        Separator statusSeparator = new Separator(Orientation.VERTICAL);
        statusSeparator.setPrefHeight(18);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox statusBar = new HBox(
                12,
                statusLabel,
                spacer,
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
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select PDF document");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));

        java.io.File selectedFile = chooser.showOpenDialog(ownerStage);
        if (selectedFile == null) {
            return;
        }

        try {
            Path pdfPath = selectedFile.toPath();
            PdfDocumentMetadata metadata = pdfService.loadMetadata(pdfPath);
            currentProject = new OverlayProject(pdfPath, metadata);
            autoFitZoomOnNextLoad = true;
            autoFitPendingAttempts = AUTO_FIT_MAX_ATTEMPTS;
            currentHtmlPath = null;
            currentPageIndex = 0;
            clearSelection();
            loadCurrentPage();
            statusLabel.setText("Loaded PDF: " + pdfPath.getFileName());
            LOGGER.log(Level.INFO, "PDF opened: {0}", pdfPath);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unable to open PDF", ex);
            showError("Cannot open PDF", ex.getMessage());
        } finally {
            updateButtonsState();
        }
    }

    private void openHtmlFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open HTML overlay");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML files", "*.html", "*.htm"));

        java.io.File selectedFile = chooser.showOpenDialog(ownerStage);
        if (selectedFile == null) {
            return;
        }

        try {
            Path htmlPath = selectedFile.toPath();
            OverlayProject loadedProject = htmlExportService.loadProjectFromHtml(htmlPath);
            currentProject = loadedProject;
            normalizeElementIdsInProject();
            autoFitZoomOnNextLoad = true;
            autoFitPendingAttempts = AUTO_FIT_MAX_ATTEMPTS;
            currentHtmlPath = htmlPath;
            currentPageIndex = 0;
            clearSelection();
            loadCurrentPage();
            statusLabel.setText("Loaded HTML: " + htmlPath.getFileName());
            LOGGER.log(Level.INFO, "HTML project opened: {0}", htmlPath);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unable to open HTML project", ex);
            showError("Cannot open HTML", ex.getMessage());
        } finally {
            updateButtonsState();
        }
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

        if (currentProject == null) {
            return;
        }

        OverlayPage page = currentProject.getOverlayPage(currentPageIndex);
        for (OverlayElement element : page.mutableElements()) {
            Region node = createVisualNode(element);
            elementNodes.put(element.getId(), node);
            overlayPane.getChildren().add(node);
        }
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

        for (double position = 0.0d; position <= length + epsilon; position += minorStepPixels) {
            boolean majorLine = isMajorMeasurement(position, majorStepPixels);
            graphics.setStroke(majorLine ? Color.rgb(42, 91, 135, 0.45) : Color.rgb(42, 91, 135, 0.18));
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
        graphics.setFill(Color.rgb(245, 248, 252));
        graphics.fillRect(0, 0, width, RULER_SIZE);
        graphics.setStroke(Color.rgb(175, 188, 201));
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
        graphics.setFill(Color.rgb(245, 248, 252));
        graphics.fillRect(0, 0, RULER_SIZE, height);
        graphics.setStroke(Color.rgb(175, 188, 201));
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

        graphics.setFont(Font.font("Segoe UI", 10));
        graphics.setFill(Color.rgb(36, 60, 82));
        graphics.setStroke(Color.rgb(63, 92, 122));
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

    private double getPixelsPerCentimeterY() {
        PdfPageMetadata pageMetadata = getCurrentPageMetadata();
        if (pageMetadata == null || pageMetadata.heightInches() <= 0.0d) {
            return 0.0d;
        }
        return currentPagePixelHeight / (pageMetadata.heightInches() * 2.54d);
    }

    private PdfPageMetadata getCurrentPageMetadata() {
        if (currentProject == null || currentPageIndex < 0 || currentPageIndex >= currentProject.getMetadata().pageCount()) {
            return null;
        }
        return currentProject.getMetadata().getPages().get(currentPageIndex);
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

        double nodeWidth = Math.max(8, element.getWidthRatio() * currentPagePixelWidth);
        double nodeHeight = Math.max(8, element.getHeightRatio() * currentPagePixelHeight);

        if (element.getType() == OverlayElementType.MARKER) {
            nodeWidth = 10;
            nodeHeight = 10;
        }

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
        List<Double> columnWidths;
        try {
            columnWidths = parseColumnWidths(element.getTableColumnWidths(), columns);
        } catch (Exception ex) {
            String fallbackWidths = buildDefaultColumnWidths(columns);
            element.setTableColumnWidths(fallbackWidths);
            columnWidths = parseColumnWidths(fallbackWidths, columns);
        }

        GridPane tableGrid = new GridPane();
        tableGrid.getStyleClass().add("editor-table-preview");

        for (double widthPercent : columnWidths) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(widthPercent);
            tableGrid.getColumnConstraints().add(constraints);
        }

        int totalRows = detailRows + 1;
        for (int row = 0; row < totalRows; row++) {
            RowConstraints rowConstraints = new RowConstraints();
            rowConstraints.setPercentHeight(100.0d / totalRows);
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
                Label detailCell = new Label(rowIndex == 1 ? "..." : "");
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
            event.consume();
        });

        node.setOnMouseClicked(event -> {
            selectElement(element, node);
            event.consume();
        });
    }

    private void handleCanvasClick(MouseEvent event) {
        if (currentProject == null) {
            statusLabel.setText("Open a PDF first");
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
            element.setTableColumnWidths(buildDefaultColumnWidths(tableColumns));
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
        selectedElementTypeLabel.setText("None");
        selectedElementIdField.clear();
        selectedElementIdField.setDisable(true);
        applyElementIdButton.setDisable(true);
        selectedElementTextField.setPromptText("Text for selected element");
        selectedElementTextField.clear();
        tableWidthPercentField.clear();
        tableWidthPercentField.setDisable(true);
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
            String widthRaw = Optional.ofNullable(tableWidthPercentField.getText()).orElse("").strip();
            if (!widthRaw.isBlank()) {
                double widthPercent = Double.parseDouble(widthRaw);
                if (widthPercent <= 0.0d || widthPercent > 100.0d) {
                    throw new IllegalArgumentException("Table width (%) must be > 0 and <= 100");
                }
                selectedElement.setWidthRatio(widthPercent / 100.0d);
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

            String normalizedWidths = normalizeColumnWidths(tableColumnWidthsField.getText(), columns);
            selectedElement.setTableColumnWidths(normalizedWidths);
            tableColumnWidthsField.setText(normalizedWidths);

            refreshVisualNode(selectedElement);
            statusLabel.setText("Table configuration updated");
        } catch (Exception ex) {
            showError("Invalid table configuration", ex.getMessage());
        }
    }

    private void updateTableInspectorFields(OverlayElement element) {
        boolean isTable = element.getType() == OverlayElementType.TABLE;
        tableWidthPercentField.setDisable(!isTable);
        tableColumnWidthsField.setDisable(!isTable);
        tableRowsCombo.setDisable(!isTable);
        applyTableConfigButton.setDisable(!isTable);

        if (!isTable) {
            selectedElementTextField.setPromptText("Text for selected element");
            tableWidthPercentField.clear();
            tableColumnWidthsField.clear();
            tableRowsCombo.setValue(1);
            return;
        }

        selectedElementTextField.setPromptText("Headers separated by |");
        tableWidthPercentField.setText(formatWidth(selectedElement.getWidthRatio() * 100.0d));
        int columns = Math.max(1, element.getTableColumnCount());
        String widths = element.getTableColumnWidths();
        if (widths.isBlank()) {
            widths = buildDefaultColumnWidths(columns);
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

    private String buildDefaultColumnWidths(int columns) {
        List<Double> widths = new ArrayList<>();
        double base = 100.0d / columns;
        double runningTotal = 0.0d;
        for (int index = 0; index < columns; index++) {
            double width = index == columns - 1 ? 100.0d - runningTotal : base;
            runningTotal += width;
            widths.add(width);
        }
        return formatColumnWidths(widths);
    }

    private String normalizeColumnWidths(String rawValue, int expectedColumns) {
        List<Double> normalized = parseColumnWidths(rawValue, expectedColumns);
        return formatColumnWidths(normalized);
    }

    private String formatColumnWidths(List<Double> widths) {
        List<String> tokens = new ArrayList<>();
        for (double width : widths) {
            tokens.add(formatWidth(width));
        }
        return String.join(",", tokens);
    }

    private String formatWidth(double value) {
        return String.format(Locale.US, "%.2f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }

    private List<Double> parseColumnWidths(String rawValue, int expectedColumns) {
        if (expectedColumns <= 0) {
            throw new IllegalArgumentException("Table must contain at least one column");
        }

        String source = rawValue == null ? "" : rawValue.strip();
        if (source.isBlank()) {
            source = buildDefaultColumnWidths(expectedColumns);
        }

        String[] tokens = source.split(",");
        if (tokens.length != expectedColumns) {
            throw new IllegalArgumentException(
                    "Column widths count must match columns (" + expectedColumns + ")"
            );
        }

        List<Double> values = new ArrayList<>();
        double total = 0.0d;
        for (String token : tokens) {
            double width = Double.parseDouble(token.strip());
            if (width <= 0.0d) {
                throw new IllegalArgumentException("Each column width must be > 0");
            }
            values.add(width);
            total += width;
        }
        if (total <= 0.0d) {
            throw new IllegalArgumentException("Invalid total column width");
        }

        List<Double> normalized = new ArrayList<>();
        for (double value : values) {
            normalized.add((value / total) * 100.0d);
        }
        return normalized;
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
        exportButton.setDisable(!hasProject);
        exportErpNextButton.setDisable(!hasProject);
        printHtmlButton.setDisable(!hasProject);
        printPdfButton.setDisable(!hasProject);
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

    private void saveProjectHtml() {
        if (currentProject == null) {
            statusLabel.setText("Open a PDF first");
            return;
        }

        Optional<SaveExportSelection> exportSelection = showExportOptionsDialog();
        if (exportSelection.isEmpty()) {
            statusLabel.setText("Save cancelled");
            return;
        }

        Path targetHtmlPath = resolveHtmlPath("Save HTML project as", "-project.html");
        if (targetHtmlPath == null) {
            return;
        }

        try {
            int selectedDpi = exportDpiCombo.getValue() == null ? 300 : exportDpiCombo.getValue();
            Path htmlPath = htmlExportService.exportProjectAsHtml(
                    currentProject,
                    targetHtmlPath,
                    selectedDpi,
                    exportSelection.get().includePdfBackground(),
                    exportSelection.get().exportOptions()
            );
            currentHtmlPath = htmlPath;
            statusLabel.setText("Project saved: " + htmlPath.getFileName());
            Alert info = new Alert(Alert.AlertType.INFORMATION, "HTML generated at:\n" + htmlPath, ButtonType.OK);
            info.setHeaderText("Project saved");
            info.initOwner(ownerStage);
            info.showAndWait();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error saving HTML project", ex);
            showError("Cannot save HTML project", ex.getMessage());
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

        Path targetHtmlPath = resolveHtmlPath("Export ERPNext HTML as", "-erpnext-fragment.html");
        if (targetHtmlPath == null) {
            return;
        }

        try {
            int selectedDpi = exportDpiCombo.getValue() == null ? 300 : exportDpiCombo.getValue();
            String fragmentHtml = htmlExportService.buildEmbedHtmlFragment(
                    currentProject,
                    selectedDpi,
                    exportSelection.get().includePdfBackground(),
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
            int selectedDpi = exportDpiCombo.getValue() == null ? 300 : exportDpiCombo.getValue();
            Path tempHtmlPath = Files.createTempFile("pdf-overlay-print-", ".html");
            Path htmlPath = htmlExportService.exportProjectAsHtml(
                    currentProject,
                    tempHtmlPath,
                    selectedDpi,
                    true,
                    ExportOptions.defaultOptions()
            );

            printService.printHtml(
                    htmlPath,
                    ownerStage,
                    printed -> statusLabel.setText(printed ? "HTML sent to printer" : "HTML print cancelled"),
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

    private void refreshHtmlSourcePreview() {
        if (currentProject == null) {
            latestGeneratedHtmlSource = "<!-- Open a PDF or HTML project first -->";
            updateHtmlSourceView();
            return;
        }
        try {
            int selectedDpi = exportDpiCombo.getValue() == null ? 300 : exportDpiCombo.getValue();
            latestGeneratedHtmlSource = htmlExportService.buildHtmlContent(
                    currentProject,
                    selectedDpi,
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
                      background: #ffffff;
                      color: #212529;
                      font-family: Consolas, Menlo, Monaco, monospace;
                    }
                    .header {
                      position: sticky;
                      top: 0;
                      z-index: 1;
                      padding: 8px 12px;
                      background: #f3f5f7;
                      border-bottom: 1px solid #dde2e7;
                      color: #445061;
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
                    .tok-tag { color: #0033b3; }
                    .tok-attr { color: #7a3e00; }
                    .tok-val { color: #067d17; }
                    .tok-comment { color: #6f7b8a; font-style: italic; }
                    .tok-jinja { color: #8a2be2; font-weight: 600; }
                  </style>
                </head>
                <body>
                  <div class="header">%s</div>
                  <pre>%s</pre>
                </body>
                </html>
                """.formatted(title, highlighted);
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

        CheckBox includePdfBackgroundCheck = new CheckBox("General: embed PDF background image");
        includePdfBackgroundCheck.setSelected(false);
        CheckBox exportFontCheck = new CheckBox("General: export font");
        exportFontCheck.setSelected(true);
        CheckBox exportTableColorsCheck = new CheckBox("Tables: export colors");
        exportTableColorsCheck.setSelected(true);
        CheckBox exportTableBordersCheck = new CheckBox("Tables: export borders");
        exportTableBordersCheck.setSelected(true);
        CheckBox exportTextBordersCheck = new CheckBox("Text fields: export borders");
        exportTextBordersCheck.setSelected(true);

        VBox content = new VBox(
                8,
                includePdfBackgroundCheck,
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
                        includePdfBackgroundCheck.isSelected(),
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

    private Path resolveHtmlPath(String dialogTitle, String defaultSuffix) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(dialogTitle);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML files", "*.html"));

        if (currentHtmlPath != null) {
            Path parent = currentHtmlPath.toAbsolutePath().getParent();
            if (parent != null && Files.exists(parent)) {
                chooser.setInitialDirectory(parent.toFile());
            }
            chooser.setInitialFileName(currentHtmlPath.getFileName().toString());
        } else if (currentProject != null) {
            Path pdfPath = currentProject.getPdfPath();
            Path parent = pdfPath.toAbsolutePath().getParent();
            if (parent != null && Files.exists(parent)) {
                chooser.setInitialDirectory(parent.toFile());
            }
            chooser.setInitialFileName(stripExtension(pdfPath.getFileName().toString()) + defaultSuffix);
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
    }

    /**
     * Snapshot mínimo para restaurar un elemento eliminado.
     */
    private record DeletedElementSnapshot(int pageIndex, OverlayElement element) {
    }

    /**
     * Selección de opciones específicas para guardado de HTML.
     */
    private record SaveExportSelection(boolean includePdfBackground, ExportOptions exportOptions) {
    }
}
