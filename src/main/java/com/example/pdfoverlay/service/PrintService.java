package com.example.pdfoverlay.service;

import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.print.PrinterJob;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Window;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;

import java.awt.print.PrinterException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio de impresión para PDF y HTML.
 */
public final class PrintService {
    private static final Logger LOGGER = Logger.getLogger(PrintService.class.getName());

    /**
     * Imprime un PDF respetando tamaño original de páginas.
     *
     * @param pdfPath ruta del documento PDF.
     * @return true cuando se completa la impresión.
     * @throws Exception cuando ocurre error de impresión o lectura.
     */
    public boolean printPdf(Path pdfPath) throws Exception {
        validatePath(pdfPath, "pdfPath");

        java.awt.print.PrinterJob printerJob = java.awt.print.PrinterJob.getPrinterJob();
        if (!printerJob.printDialog()) {
            return false;
        }

        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            printerJob.setPageable(new PDFPageable(document));
            printerJob.print();
            LOGGER.log(Level.INFO, "PDF printed: {0}", pdfPath);
            return true;
        } catch (PrinterException ex) {
            LOGGER.log(Level.SEVERE, "Error printing PDF", ex);
            throw ex;
        }
    }

    /**
     * Imprime un archivo HTML cargándolo en un WebView temporal.
     *
     * @param htmlPath       ruta del HTML.
     * @param ownerWindow    ventana propietaria para el diálogo de impresión.
     * @param resultConsumer callback de resultado (true impreso, false cancelado/fallido).
     * @param errorConsumer  callback de mensaje de error.
     */
    public void printHtml(Path htmlPath, Window ownerWindow,
                          Consumer<Boolean> resultConsumer, Consumer<String> errorConsumer) {
        Objects.requireNonNull(ownerWindow, "ownerWindow is required");
        Objects.requireNonNull(resultConsumer, "resultConsumer is required");
        Objects.requireNonNull(errorConsumer, "errorConsumer is required");
        validatePath(htmlPath, "htmlPath");

        WebView webView = new WebView();
        webView.setPrefSize(1280, 1800);
        WebEngine webEngine = webView.getEngine();

        ChangeListener<Worker.State> listener = new ChangeListener<>() {
            @Override
            public void changed(javafx.beans.value.ObservableValue<? extends Worker.State> observableValue,
                                Worker.State oldState,
                                Worker.State newState) {
                if (newState == Worker.State.SUCCEEDED) {
                    observableValue.removeListener(this);
                    executeHtmlPrint(webView, ownerWindow, htmlPath, resultConsumer, errorConsumer);
                } else if (newState == Worker.State.FAILED || newState == Worker.State.CANCELLED) {
                    observableValue.removeListener(this);
                    Throwable error = webEngine.getLoadWorker().getException();
                    String message = error == null ? "Cannot load HTML for printing" : error.getMessage();
                    LOGGER.log(Level.SEVERE, "Error loading HTML for print: {0}", message);
                    errorConsumer.accept(message);
                    resultConsumer.accept(false);
                }
            }
        };

        webEngine.getLoadWorker().stateProperty().addListener(listener);
        webEngine.load(htmlPath.toUri().toString());
    }

    private void executeHtmlPrint(WebView webView, Window ownerWindow, Path htmlPath,
                                  Consumer<Boolean> resultConsumer, Consumer<String> errorConsumer) {
        PrinterJob printerJob = PrinterJob.createPrinterJob();
        if (printerJob == null) {
            String message = "No printer configured in the system";
            LOGGER.warning(message);
            errorConsumer.accept(message);
            resultConsumer.accept(false);
            return;
        }

        if (!printerJob.showPrintDialog(ownerWindow)) {
            resultConsumer.accept(false);
            return;
        }

        boolean printSuccess = printerJob.printPage(webView);
        if (printSuccess) {
            printerJob.endJob();
            LOGGER.log(Level.INFO, "HTML printed: {0}", htmlPath);
        } else {
            errorConsumer.accept("Printer failed while printing HTML");
        }
        resultConsumer.accept(printSuccess);
    }

    private void validatePath(Path path, String fieldName) {
        Objects.requireNonNull(path, fieldName + " is required");
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Invalid path for " + fieldName + ": " + path);
        }
    }
}

