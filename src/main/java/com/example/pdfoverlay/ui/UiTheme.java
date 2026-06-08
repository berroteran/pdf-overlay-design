package com.example.pdfoverlay.ui;

import javafx.application.Application;
import javafx.scene.paint.Color;

/**
 * Temas disponibles para la interfaz JavaFX.
 * Los temas Swing se representan como variantes visuales inspiradas
 * en sus look and feel clásicos, manteniendo compatibilidad nativa con JavaFX.
 */
public enum UiTheme {
    MODENA(
            "JavaFX Modena",
            Application.STYLESHEET_MODENA,
            "theme-modena",
            "#f4f7fb",
            "#bcc9d6",
            "#274866",
            "#355c84",
            "#2f5f90",
            "#2f5f90",
            0.44,
            0.20,
            false
    ),
    CASPIAN(
            "JavaFX Caspian",
            Application.STYLESHEET_CASPIAN,
            "theme-caspian",
            "#f0f3f7",
            "#9fb0c1",
            "#1f3951",
            "#3d6288",
            "#375f88",
            "#375f88",
            0.42,
            0.18,
            false
    ),
    METAL(
            "Swing Metal",
            Application.STYLESHEET_MODENA,
            "theme-metal",
            "#edf0f5",
            "#9aa4b0",
            "#243241",
            "#556270",
            "#4e657d",
            "#4e657d",
            0.44,
            0.20,
            false
    ),
    NIMBUS(
            "Swing Nimbus",
            Application.STYLESHEET_MODENA,
            "theme-nimbus",
            "#edf4fa",
            "#90a9c2",
            "#103250",
            "#41698f",
            "#3e78a8",
            "#3e78a8",
            0.46,
            0.22,
            false
    ),
    WINDOWS(
            "Swing Windows",
            Application.STYLESHEET_MODENA,
            "theme-windows",
            "#f2f4f7",
            "#a8b6c4",
            "#1d3f63",
            "#39668f",
            "#3d6f9f",
            "#3d6f9f",
            0.44,
            0.20,
            false
    ),
    WINDOWS_CLASSIC(
            "Swing Windows Classic",
            Application.STYLESHEET_MODENA,
            "theme-windows-classic",
            "#e7edf3",
            "#75889c",
            "#11273b",
            "#234d74",
            "#315c84",
            "#315c84",
            0.48,
            0.22,
            false
    ),
    MOTIF(
            "Swing CDE/Motif",
            Application.STYLESHEET_CASPIAN,
            "theme-motif",
            "#eceadf",
            "#9d9888",
            "#353126",
            "#5f5848",
            "#70695a",
            "#70695a",
            0.42,
            0.18,
            false
    ),
    DARK(
            "Dark",
            Application.STYLESHEET_MODENA,
            "theme-dark",
            "#1d232b",
            "#566171",
            "#eef4fb",
            "#9ab4d0",
            "#75a7dc",
            "#75a7dc",
            0.50,
            0.24,
            true
    );

    private final String displayName;
    private final String userAgentStylesheet;
    private final String rootStyleClass;
    private final Color rulerBackground;
    private final Color rulerBorder;
    private final Color rulerText;
    private final Color rulerTick;
    private final Color gridMajor;
    private final Color gridMinor;
    private final boolean dark;

    UiTheme(String displayName,
            String userAgentStylesheet,
            String rootStyleClass,
            String rulerBackground,
            String rulerBorder,
            String rulerText,
            String rulerTick,
            String gridMajorBase,
            String gridMinorBase,
            double gridMajorOpacity,
            double gridMinorOpacity,
            boolean dark) {
        this.displayName = displayName;
        this.userAgentStylesheet = userAgentStylesheet;
        this.rootStyleClass = rootStyleClass;
        this.rulerBackground = Color.web(rulerBackground);
        this.rulerBorder = Color.web(rulerBorder);
        this.rulerText = Color.web(rulerText);
        this.rulerTick = Color.web(rulerTick);
        this.gridMajor = withOpacity(gridMajorBase, gridMajorOpacity);
        this.gridMinor = withOpacity(gridMinorBase, gridMinorOpacity);
        this.dark = dark;
    }

    public String getUserAgentStylesheet() {
        return userAgentStylesheet;
    }

    public String getRootStyleClass() {
        return rootStyleClass;
    }

    public Color getRulerBackground() {
        return rulerBackground;
    }

    public Color getRulerBorder() {
        return rulerBorder;
    }

    public Color getRulerText() {
        return rulerText;
    }

    public Color getRulerTick() {
        return rulerTick;
    }

    public Color getGridMajor() {
        return gridMajor;
    }

    public Color getGridMinor() {
        return gridMinor;
    }

    public boolean isDark() {
        return dark;
    }

    @Override
    public String toString() {
        return displayName;
    }

    private static Color withOpacity(String color, double opacity) {
        Color baseColor = Color.web(color);
        return Color.color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), opacity);
    }
}
