package com.example.pdfoverlay.service;

import com.example.pdfoverlay.model.PdfDocumentMetadata;
import com.example.pdfoverlay.model.PdfPageMetadata;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio para lectura y render de documentos PDF.
 */
public final class PdfService {
    private static final Logger LOGGER = Logger.getLogger(PdfService.class.getName());

    /**
     * Carga metadatos de páginas del PDF.
     *
     * @param pdfPath ruta del archivo PDF.
     * @return metadatos de documento y páginas.
     * @throws IOException cuando no se puede leer el archivo.
     */
    public PdfDocumentMetadata loadMetadata(Path pdfPath) throws IOException {
        validatePdfPath(pdfPath);

        List<PdfPageMetadata> pages = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                PDPage page = document.getPage(pageIndex);
                float width = page.getMediaBox().getWidth();
                float height = page.getMediaBox().getHeight();
                pages.add(new PdfPageMetadata(pageIndex, width, height));
            }
        }
        LOGGER.log(Level.INFO, "PDF metadata loaded: {0}", pdfPath);
        return new PdfDocumentMetadata(pdfPath, pages);
    }

    /**
     * Renderiza una página PDF como imagen JavaFX para vista previa.
     *
     * @param pdfPath   ruta del archivo PDF.
     * @param pageIndex índice de página base 0.
     * @param dpi       resolución de render.
     * @return imagen JavaFX de la página.
     * @throws IOException cuando falla el render.
     */
    public Image renderPageAsFxImage(Path pdfPath, int pageIndex, float dpi) throws IOException {
        BufferedImage bufferedImage = renderPageAsBufferedImage(pdfPath, pageIndex, dpi);
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }

    /**
     * Renderiza una página PDF y la guarda como PNG.
     *
     * @param pdfPath    ruta del PDF.
     * @param pageIndex  índice de página base 0.
     * @param dpi        resolución de render.
     * @param outputPath ruta de salida PNG.
     * @throws IOException cuando falla escritura o lectura.
     */
    public void renderPageToPng(Path pdfPath, int pageIndex, float dpi, Path outputPath) throws IOException {
        Objects.requireNonNull(outputPath, "outputPath is required");
        BufferedImage bufferedImage = renderPageAsBufferedImage(pdfPath, pageIndex, dpi);
        Files.createDirectories(outputPath.getParent());
        if (!ImageIO.write(bufferedImage, "png", outputPath.toFile())) {
            throw new IOException("Unable to write PNG image: " + outputPath);
        }
        LOGGER.log(Level.INFO, "Page rendered to PNG: {0}", outputPath);
    }

    /**
     * Renderiza una página PDF y devuelve bytes PNG en memoria.
     *
     * @param pdfPath   ruta del PDF.
     * @param pageIndex índice de página base 0.
     * @param dpi       resolución de render.
     * @return bytes codificados como imagen PNG.
     * @throws IOException cuando falla el render o codificación.
     */
    public byte[] renderPageToPngBytes(Path pdfPath, int pageIndex, float dpi) throws IOException {
        BufferedImage bufferedImage = renderPageAsBufferedImage(pdfPath, pageIndex, dpi);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (!ImageIO.write(bufferedImage, "png", outputStream)) {
                throw new IOException("Unable to encode PNG bytes for page " + pageIndex);
            }
            return outputStream.toByteArray();
        }
    }

    private BufferedImage renderPageAsBufferedImage(Path pdfPath, int pageIndex, float dpi) throws IOException {
        validatePdfPath(pdfPath);
        if (dpi <= 0) {
            throw new IllegalArgumentException("dpi must be > 0");
        }
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            if (pageIndex < 0 || pageIndex >= document.getNumberOfPages()) {
                throw new IllegalArgumentException("Invalid page index: " + pageIndex);
            }
            PDFRenderer renderer = new PDFRenderer(document);
            return renderer.renderImageWithDPI(pageIndex, dpi, ImageType.RGB);
        }
    }

    private void validatePdfPath(Path pdfPath) {
        Objects.requireNonNull(pdfPath, "pdfPath is required");
        if (!Files.exists(pdfPath)) {
            throw new IllegalArgumentException("PDF does not exist: " + pdfPath);
        }
        if (!Files.isRegularFile(pdfPath)) {
            throw new IllegalArgumentException("Invalid PDF file path: " + pdfPath);
        }
    }
}
