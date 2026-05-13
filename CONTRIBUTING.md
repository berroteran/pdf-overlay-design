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
- Mensajes sugeridos por prefijo:
  - `feat:`
  - `fix:`
  - `docs:`
  - `build:`
  - `ci:`
  - `test:`
  - `chore:`

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
