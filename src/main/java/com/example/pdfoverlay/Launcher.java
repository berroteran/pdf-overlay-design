package com.example.pdfoverlay;

import javafx.application.Application;

/**
 * Lanzador independiente para evitar problemas de arranque JavaFX en algunos IDE.
 */
public final class Launcher {

    private Launcher() {
    }

    /**
     * Punto de entrada del proceso.
     *
     * @param args argumentos de línea de comandos.
     */
    public static void main(String[] args) {
        Application.launch(PdfOverlayApplication.class, args);
    }
}

