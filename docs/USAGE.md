# Guía de Uso

## Abrir Documento

Usar `File > Open > Open PDF...` para iniciar un proyecto desde un PDF base.
La aplicación renderiza la página actual, activa reglas, grilla y overlay.

Usar `File > Open > Open Project HTML...` para continuar editando un archivo
guardado por la aplicación. No se soporta abrir HTML externo sin metadata
interna del proyecto.

## Herramientas

| Herramienta | Uso |
| --- | --- |
| `Select` | Seleccionar, mover y redimensionar controles. |
| `Text` | Insertar campos de texto editables en el HTML final. |
| `Label` | Insertar texto fijo. |
| `Button` | Insertar elemento visual tipo botón. |
| `Point` | Insertar marcador circular. |
| `Table` | Insertar tabla HTML real con columnas y filas. |
| `Measure` | Medir temporalmente `W x H` en milímetros. |

## Medición en Milímetros

La herramienta `Measure` permite medir tablas, columnas o espacios sin crear
elementos persistentes.

1. Activar `Measure` en la barra superior.
2. Arrastrar sobre el área deseada.
3. Leer el valor `W x H` junto al botón o en la etiqueta del recuadro.
4. Usar ese valor para configurar ancho de tabla, columnas o controles.

El recuadro temporal se elimina al hacer click posterior o al presionar una
tecla. El último valor queda visible junto al botón.

## Tablas

Al insertar una tabla se define la cantidad de columnas. El inspector permite
configurar:

- `Table width (mm)`.
- `Table column widths (mm)`.
- `Table detail rows`.
- Encabezados separados por `|` en el campo `Text`.

Al redimensionar una tabla con mouse:

- Se recalcula el ancho total en mm.
- Se escalan proporcionalmente las columnas.
- Se actualizan los inputs del inspector.
- Se refresca la vista de columnas en tiempo real.

## Guardar Proyecto

`Save Project` guarda en la ruta actual. Si el proyecto aún no tiene ruta,
abre el flujo de `Save Project As...`.

`Save Project As...` pide una nueva ruta y guarda un HTML editable por esta
aplicación. Ese archivo conserva metadata del proyecto y los datos necesarios
para continuar editando.

Guardar proyecto no muestra opciones de exportación.

## Exportar ERPNext

`Export ERPNext...` genera el fragmento que debe insertarse en Print Format.
La salida no incluye imagen original ni metadata editable.

El fragmento exportado empieza así:

```html
<style>
/* CSS generado por esta aplicación */
</style>
<div class="preprinted-page">
    <!-- páginas y controles exportados -->
</div>
```

## Imprimir y Navegador

- `Print PDF`: imprime el PDF/imagen original.
- `Print HTML`: imprime HTML con PDF embebido como fondo.
- `Print HTML Only`: imprime estrictamente solo el HTML.
- `Open HTML`: abre en navegador el HTML estricto, sin imagen original.

## Navegación

Los botones `Prev`, `Next` y el indicador de página están en la barra inferior.
El zoom se controla desde la barra inferior o con atajos.

## Atajos

| Atajo | Acción |
| --- | --- |
| `Ctrl/Cmd + Q` | Salir. |
| `Ctrl/Cmd + +` | Aumentar zoom. |
| `Ctrl/Cmd + -` | Reducir zoom. |
| `Ctrl/Cmd + rueda mouse` | Zoom interactivo. |
| `Ctrl/Cmd + Z` | Deshacer último borrado. |
| `DEL` | Borrar elemento seleccionado. |
| Cualquier tecla | Limpia medición temporal visible. |
