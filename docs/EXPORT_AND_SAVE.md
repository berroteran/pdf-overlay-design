# Guardado, Exportación e Impresión

## Regla Principal

Guardar proyecto y exportar son operaciones distintas.

- Guardar conserva el proyecto editable.
- Exportar genera salida para uso externo.
- Imprimir usa salidas temporales y no modifica el proyecto.

## Flujo

```mermaid
flowchart TD
    A[Proyecto abierto] --> B{Acción}

    B --> C[Save Project]
    C --> D{currentHtmlPath existe}
    D -->|Sí| E[Guardar directo\ncon metadata editable]
    D -->|No| F[Save Project As]
    F --> G[Elegir ruta]
    G --> E
    E --> O[New Project habilitado\nsi no hay cambios pendientes]

    B --> H[Export ERPNext]
    H --> I[Mostrar Export options]
    I --> J[Elegir ruta de export]
    J --> K[Generar fragmento\nsin metadata ni imagen original]

    B --> L[Print / Open HTML]
    L --> M[Generar HTML temporal]
    M --> N[WebView / navegador / impresora]
```

## Save Project

`Save Project` escribe el proyecto en la ruta actual. Si todavía no existe
ruta, delega a `Save Project As...`.

Características:

- Incluye metadata interna `PDF_OVERLAY_METADATA_BEGIN/END`.
- Conserva datos necesarios para `Open Project HTML`.
- Usa opciones completas por defecto.
- No pregunta opciones de exportación.
- Actualiza `currentHtmlPath`.
- Limpia el estado de cambios pendientes.

## Save Project As

`Save Project As...` siempre pide ruta y guarda un nuevo archivo de proyecto.

Características:

- Misma salida editable que `Save Project`.
- Cambia `currentHtmlPath` al nuevo archivo.
- Puede usarse para renombrar o duplicar proyecto.

## New Project

`New Project` cierra el proyecto actual y deja el editor sin documento cargado.
Solo se habilita cuando el proyecto tiene archivo guardado y no hay cambios
pendientes.

No crea un archivo nuevo por sí mismo. El siguiente paso normal es abrir otro
PDF o un HTML de proyecto desde `Open` o `Recent Files`.

## Recent Files

El historial guarda hasta 10 rutas persistentes entre sesiones.

- Incluye PDF abiertos.
- Incluye HTML de proyecto abiertos o guardados.
- Elimina rutas inexistentes al refrescar el menú.
- Abrir un reciente aplica la misma protección de cambios pendientes que
  `Open PDF` y `Open Project HTML`.

## Export ERPNext

`Export ERPNext...` genera salida para Print Format.

Características:

- Muestra `Export options`.
- No incluye metadata editable.
- No incluye imagen/PDF original.
- No actualiza `currentHtmlPath`.
- Parte del PDF/proyecto fuente para sugerir nombre de archivo.

Formato inicial del fragmento:

```html
<style>
/* CSS generado por esta aplicación */
</style>
<div class="preprinted-page">
    <!-- páginas y controles exportados -->
</div>
```

## Plantilla Completa

Para render standalone o previsualización con plantilla, los estilos propios
de la aplicación van dentro de `.print-format`, no en `<head>`.

```html
<!DOCTYPE html>
<html>
<head>
</head>
<body>
    <div class="print-format-gutter">
        <div class="print-format">
            <style>{{ print_style }}</style>
            {{ body }}
        </div>
    </div>
</body>
</html>
```

Las llamadas Jinja para incluir CSS externo no pertenecen a esta plantilla y no
deben generarse.

## Impresión

```mermaid
flowchart LR
    A[Print PDF] --> B[PDF original]
    B --> C[PrintService\nPDFBox Printable]

    D[Print HTML] --> E[HTML temporal\ncon PDF embebido]
    E --> F[PrintService\nJavaFX WebView]

    G[Print HTML Only] --> H[HTML temporal\nsin PDF]
    H --> F

    I[Open HTML] --> J[HTML temporal\nsin PDF]
    J --> K[Navegador por defecto]
```

## Reglas de Exportación Física

- Usar `mm` para medidas exportadas.
- No usar porcentajes para layout final.
- Mantener ancho de tablas y columnas en valores directos.
- No incluir imagen original en export ERPNext.
- Mantener estilos generados dentro del cuerpo imprimible.
