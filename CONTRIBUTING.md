# Guía de Colaboración

Gracias por contribuir a **PDF Overlay Designer**.

## Autoría del proyecto

- **Omar Berroteran**

## Licencia de contribuciones

Al contribuir a este repositorio, aceptas que tus aportes se publiquen bajo
la licencia **Apache License 2.0** del proyecto.

## Requisitos de entorno

- JDK 21
- Maven 3.9+
- Git

## Flujo recomendado

1. Crea una rama desde `master`:
   - `feature/<descripcion-corta>`
   - `fix/<descripcion-corta>`
2. Implementa cambios pequeños y acotados por tarea.
3. Ejecuta validación local:
   - `mvn test`
   - `mvn -DskipTests package`
4. Realiza commits claros y pequeños (un objetivo por commit).
5. Abre Pull Request con:
   - objetivo del cambio
   - alcance técnico
   - evidencia de pruebas.

## Convenciones del proyecto

- Código en inglés:
  - nombres de clases, métodos, variables, enums y campos.
- Documentación técnica en español:
  - comentarios y JavaDoc.
- Mantener diseño por capas:
  - UI/controlador
  - servicios
  - modelo.
- Evitar dependencias innecesarias.
- Priorizar mantenibilidad y simplicidad operativa.

## Reglas de commits

- Commit por tarea.
- Usar título con formato:
  - `[Component]: Imperative summary`
- Mantener el título en 50 caracteres o menos.
- Dejar una línea en blanco entre título y cuerpo.
- Explicar qué cambia y por qué.
- Cerrar commits asistidos con:
  - `Assisted-by: Codex (GPT-5)`

Ejemplo:

```text
[UI]: Add overlay measurement tool

Add a temporary measurement tool for drawing an overlay box and
reading its width and height in millimeters while editing forms.

Assisted-by: Codex (GPT-5)
```

## Documentación

- Actualizar README cuando cambie una función visible.
- Actualizar `docs/USAGE.md` cuando cambie el flujo de usuario.
- Actualizar `docs/EXPORT_AND_SAVE.md` cuando cambie guardado, exportación o impresión.
- Actualizar `docs/ARCHITECTURE.md` cuando cambien capas, servicios o responsabilidades.
- Si una regla evita errores previos, documentarla explícitamente.

## Pull Requests

- No mezclar refactors grandes con cambios funcionales.
- Si un cambio afecta UI + exportación + tests, dividir en PRs o commits separados cuando sea posible.
- Incluir impacto en:
  - UX/funcionalidad
  - compatibilidad de exportación HTML
  - impresión y flujo ERPNext/Jinja.

## Reporte de bugs

Al reportar un bug, incluir:

- versión/tag del proyecto
- sistema operativo
- pasos para reproducir
- resultado esperado vs actual
- logs o capturas relevantes.
