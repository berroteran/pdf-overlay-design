package com.example.pdfoverlay;

import com.example.pdfoverlay.ui.MainViewController;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Punto de entrada principal de la aplicación.
 */
public class PdfOverlayApplication extends Application {
    private static final String APP_ICON_PATH = "/icons/app-icon.png";

    /**
     * Inicializa la ventana principal y carga la UI.
     *
     * @param stage escenario principal de JavaFX.
     */
    @Override
    public void start(Stage stage) {
        Scene splashScene = createSplashScene();
        stage.setScene(splashScene);
        stage.setTitle("PDF Overlay Designer");
        stage.getIcons().add(loadAppIcon());
        stage.show();
        stage.centerOnScreen();

        PauseTransition splashDelay = new PauseTransition(Duration.seconds(1.7));
        splashDelay.setOnFinished(event -> showMainEditor(stage));
        splashDelay.play();
    }

    /**
     * Reemplaza splash por la vista principal del editor.
     *
     * @param stage escenario principal.
     */
    private void showMainEditor(Stage stage) {
        MainViewController controller = new MainViewController(stage);
        Scene scene = new Scene(controller.getRoot(), 1380, 900);
        scene.getStylesheets().add(PdfOverlayApplication.class.getResource("/styles/app.css").toExternalForm());

        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(720);
        stage.centerOnScreen();
    }

    /**
     * Construye la escena de splash inicial.
     *
     * @return escena de splash.
     */
    private Scene createSplashScene() {
        VBox content = new VBox(16);
        content.setAlignment(Pos.CENTER);
        content.getStyleClass().add("splash-content");

        Label titleLabel = new Label("PDF Overlay Designer");
        titleLabel.getStyleClass().add("splash-title");

        Label subtitleLabel = new Label("Inicializando editor de formatos pre-impresos...");
        subtitleLabel.getStyleClass().add("splash-subtitle");

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.getStyleClass().add("splash-progress");
        progressIndicator.setPrefSize(72, 72);

        content.getChildren().addAll(titleLabel, subtitleLabel, progressIndicator);

        StackPane root = new StackPane(content);
        root.getStyleClass().add("splash-root");

        FadeTransition pulse = new FadeTransition(Duration.seconds(1.1), subtitleLabel);
        pulse.setFromValue(0.45);
        pulse.setToValue(1.0);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(FadeTransition.INDEFINITE);
        pulse.play();

        Scene scene = new Scene(root, 760, 440);
        scene.getStylesheets().add(PdfOverlayApplication.class.getResource("/styles/app.css").toExternalForm());
        return scene;
    }

    /**
     * Carga el ícono principal de la aplicación.
     *
     * @return imagen de ícono para la ventana.
     */
    private Image loadAppIcon() {
        return new Image(
                java.util.Objects.requireNonNull(
                        PdfOverlayApplication.class.getResourceAsStream(APP_ICON_PATH),
                        "Application icon not found at " + APP_ICON_PATH
                )
        );
    }

    /**
     * Lanza la aplicación JavaFX.
     *
     * @param args argumentos de línea de comandos.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
