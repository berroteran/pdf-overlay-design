package com.example.pdfoverlay.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Estado de overlay para una página específica.
 */
public final class OverlayPage {
    private final int pageIndex;
    private final List<OverlayElement> elements;

    /**
     * Crea el contenedor de elementos para una página.
     *
     * @param pageIndex índice de página base 0.
     */
    public OverlayPage(int pageIndex) {
        if (pageIndex < 0) {
            throw new IllegalArgumentException("pageIndex must be >= 0");
        }
        this.pageIndex = pageIndex;
        this.elements = new ArrayList<>();
    }

    /**
     * @return índice base 0 de la página.
     */
    public int getPageIndex() {
        return pageIndex;
    }

    /**
     * @return copia de elementos de la página.
     */
    public List<OverlayElement> getElements() {
        return elements.stream().map(OverlayElement::copy).toList();
    }

    /**
     * Agrega un nuevo elemento a la página.
     *
     * @param element elemento a insertar.
     */
    public void addElement(OverlayElement element) {
        elements.add(Objects.requireNonNull(element, "element is required"));
    }

    /**
     * Elimina un elemento por id.
     *
     * @param elementId identificador del elemento.
     * @return true si se eliminó.
     */
    public boolean removeElement(String elementId) {
        return elements.removeIf(item -> item.getId().equals(elementId));
    }

    /**
     * Busca un elemento por id.
     *
     * @param elementId identificador del elemento.
     * @return elemento encontrado si existe.
     */
    public Optional<OverlayElement> findElementById(String elementId) {
        return elements.stream().filter(item -> item.getId().equals(elementId)).findFirst();
    }

    /**
     * Permite acceso mutable controlado para la UI/editor.
     *
     * @return lista interna mutable.
     */
    public List<OverlayElement> mutableElements() {
        return elements;
    }
}

