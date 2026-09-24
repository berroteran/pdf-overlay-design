# Instrucciones de desarrollo y agentes

Estas reglas aplican a todo el repositorio y son la referencia central de
convenciones y forma de trabajo para agentes y colaboradores. Las guías de
contribución y arquitectura deben enlazarlas, sin mantener copias divergentes.

## Stack y alcance

- Revisar `pom.xml`, el código afectado y sus consumidores antes de implementar.
  El stack actual es Java 21, Maven, JavaFX 21, PDFBox 3 y JUnit 5.
- Mantener compatibilidad con las versiones configuradas. No introducir
  frameworks, dependencias o actualizaciones sin una necesidad concreta.
- Aplicar estas reglas a código nuevo y a refactorizaciones dentro del alcance
  de la tarea. La estructura objetivo no implica que ya esté implementada.
- Evolucionar el código existente de forma incremental, preservando contratos,
  archivos de proyecto y comportamiento. No realizar reorganizaciones masivas
  como efecto secundario de una corrección pequeña.

## Idiomas y nombres

- Escribir en inglés los nombres de paquetes, clases, interfaces, métodos,
  variables, parámetros, campos, constantes, enums, claves de recursos y tests.
- Usar nombres representativos del dominio y de la intención; evitar
  abreviaturas ambiguas y nombres genéricos como `data`, `manager` o `helper`
  cuando exista un concepto más preciso.
- Usar las convenciones Java: paquetes en minúsculas, tipos en `PascalCase`,
  métodos y variables en `camelCase`, constantes en `UPPER_SNAKE_CASE`.
- Escribir comentarios, JavaDoc y documentación explicativa en español.
  Conservar identificadores técnicos, comandos y nombres de APIs en su forma
  original. Los comentarios deben explicar decisiones, contratos o restricciones,
  sin repetir lo que ya expresa el código.
- Mantener estables los identificadores de formatos persistidos y contratos
  externos; un cambio de nombre requiere evaluar compatibilidad y migración.

## Organización por dominio y componente

Organizar primero por responsabilidad funcional y después por capas internas
cuando aporten claridad. Evitar paquetes globales que acumulen servicios,
controladores o utilidades de dominios sin relación.

Estructura de referencia bajo `com.example.pdfoverlay`:

| Paquete | Responsabilidad |
| --- | --- |
| `core` | Núcleo independiente de la interfaz: modelo, reglas y casos de uso. Separar componentes como `project`, `overlay` y `export` según la necesidad real. |
| `core.<component>.application` | Casos de uso y contratos de entrada/salida del componente. |
| `core.<component>.domain` | Entidades, objetos de valor e invariantes del componente. |
| `infrastructure` | Adaptadores de archivos, PDFBox, plantillas y otras integraciones, agrupados por componente. |
| `security` | Políticas transversales de autorización y protección, cuando existan. |
| `auth` | Autenticación e identidad; `auth.session` gestiona el ciclo de vida de sesiones, si se incorpora esa capacidad. |
| `ui` | Presentación JavaFX, organizada por pantalla o funcionalidad. |
| `ui.components` | Controles visuales reutilizables y sus contratos de interacción. |
| `ui.theme` | Selección y aplicación de temas visuales. |
| `ui.i18n` | Selección de idioma y resolución de mensajes. |
| `cli` | Comandos, argumentos, entrada/salida y códigos de salida de la consola. |
| `bootstrap` | Puntos de entrada y composición de dependencias de cada interfaz. |

Crear únicamente los paquetes que tengan una responsabilidad implementada.
No añadir autenticación, sesiones, repositorios, DTOs o interfaces vacías solo
para completar este esquema. Seguridad, autenticación y sesiones deben tener
responsabilidades diferenciadas cuando sean necesarias.

## Desacople, responsabilidad única y reutilización

- Mantener la dirección de dependencias hacia el núcleo: UI, CLI y adaptadores
  consumen los casos de uso y contratos de `core`; `core` no importa esos paquetes.
- El núcleo no debe depender de JavaFX, controles, propiedades observables de
  JavaFX, diálogos, ventanas, `WebView`, argumentos de consola ni `System.exit`.
  Las implementaciones PDFBox y de acceso a archivos quedan en adaptadores.
- Usar tipos de dominio y contratos neutrales para resultados, errores y datos.
  Convertir imágenes o modelos a tipos JavaFX en el adaptador visual.
- Los controladores y comandos traducen entradas, invocan casos de uso y
  presentan resultados. Las reglas de negocio, validaciones compartidas,
  persistencia y exportación pertenecen a sus componentes correspondientes.
- UI, CLI y futuros servicios deben reutilizar los mismos casos de uso sin
  requerir la inicialización del entorno gráfico. Una operación que dependa de
  impresión gráfica debe quedar explícitamente en su adaptador de UI.
- Inyectar dependencias por constructor y componerlas en el punto de entrada.
  Evitar singletons con estado mutable y localizadores globales de servicios.
  La inyección manual es suficiente mientras no exista una necesidad mayor.
- Separar controlador, servicio de aplicación, dominio, repositorio/adaptador y
  DTO cuando corresponda. Usar interfaces en límites que necesiten sustitución
  o inversión de dependencias; no crear una interfaz por cada clase por rutina.
- Extraer componentes con una responsabilidad coherente. Reutilizar lógica
  compartida comprobada; evitar duplicación y abstracciones especulativas.
- Los controles de `ui.components` no deben conocer una pantalla concreta ni
  acceder directamente a persistencia: reciben datos y exponen eventos.

## UI: temas e idiomas

- La UI debe admitir inglés (`en`) y español (`es`), con selección de idioma y
  preferencia persistente. Definir un idioma de respaldo determinista.
- Centralizar textos visibles en recursos `ResourceBundle`: menús, botones,
  títulos, ayuda, validaciones, errores y etiquetas de accesibilidad. No añadir
  literales traducibles en controladores ni concatenar fragmentos traducidos.
- Mantener las mismas claves en ambos idiomas y usar parámetros para mensajes.
  Formatear fechas y números de presentación con el `Locale` seleccionado.
- No traducir automáticamente el contenido del documento del usuario. El idioma
  visual no debe alterar identificadores, datos guardados ni sintaxis HTML/CSS
  o Jinja; los formatos técnicos deben usar una configuración regional estable.
- Centralizar colores, tipografías y estados visuales en CSS y recursos de tema.
  Evitar estilos y colores dispersos en controladores; los dibujos programáticos
  deben consumir los valores del tema activo.
- Conservar los temas existentes y permitir agregar otros sin cambiar reglas
  de negocio. Persistir la preferencia y prever respaldo ante valores inválidos.
- Cambiar tema o idioma sin perder el documento ni su estado de edición.
  Verificar legibilidad, contraste, foco de teclado y textos largos en ambos
  idiomas y en los temas afectados.
- Ejecutar tareas costosas de PDF, archivos y exportación fuera del hilo de UI.
  Actualizar controles en el hilo JavaFX y gestionar errores y cancelación.

## CLI: convenciones Unix/Linux

Cuando se implemente una CLI, mantenerla en `cli` y aplicar convenciones POSIX
de argumentos y extensiones GNU habituales donde correspondan:

- Nombres de comandos y opciones en inglés, predecibles y en minúsculas;
  subcomandos por operación y opciones largas como `--output`, con alias cortos
  solo cuando sean útiles y no ambiguos.
- Ofrecer `--help` y `--version`; documentar sintaxis, argumentos obligatorios,
  valores predeterminados, ejemplos, códigos de salida y efectos sobre archivos.
- Admitir `--` para terminar las opciones y tratar correctamente rutas con
  espacios o nombres que comiencen con guion. Usar `-` para stdin/stdout en las
  operaciones donde el formato permita trabajar con flujos.
- Enviar resultados a stdout y diagnósticos, progreso y logs a stderr. No
  contaminar una salida estructurada o binaria con mensajes informativos.
- Definir códigos estables: `0` para éxito, `1` para fallo de ejecución y `2`
  para uso o argumentos inválidos. Traducir errores al código correspondiente
  en el punto de entrada; los casos de uso no terminan el proceso.
- Funcionar sin interfaz gráfica y sin preguntas interactivas por defecto.
  Rechazar sobrescrituras por defecto y exigir una opción explícita como
  `--force` cuando corresponda; documentar el comportamiento.
- Permitir composición con redirecciones y pipes cuando aplique, resultados
  deterministas y cancelación limpia. Liberar recursos y evitar archivos
  parcialmente escritos ante un fallo.
- Validar argumentos antes de producir efectos. No construir comandos de shell
  concatenando entradas del usuario ni asumir separadores de rutas de un SO.
- Probar parsing, ayuda, stdout/stderr, códigos de salida y errores sin iniciar
  JavaFX. No declarar conformidad formal con POSIX solo por seguir esta guía.

## Calidad y seguridad

- Validar entradas en los límites e invariantes en el dominio. Evitar estados
  inválidos y favorecer inmutabilidad cuando simplifique el diseño.
- Manejar excepciones con contexto; no silenciarlas ni usar capturas genéricas
  para aparentar éxito. Traducirlas a mensajes de UI o CLI en sus adaptadores.
- Usar logging útil y coherente con el proyecto, sin exponer credenciales ni
  contenido sensible de documentos. Evitar registrar repetidamente el mismo error.
- Cerrar documentos, streams y recursos con `try-with-resources` cuando aplique.
  Considerar tamaño de archivos, memoria, cancelación y escrituras seguras.
- Tratar archivos y HTML externos como entradas no confiables. Escapar según
  el contexto de salida y delimitar explícitamente HTML/Jinja permitido sin
  romper los contratos de exportación ni habilitar ejecución accidental.

## Forma de trabajo y verificación

1. Leer estas instrucciones, `CONTRIBUTING.md` y la documentación del componente.
   Inspeccionar el estado de Git y preservar cambios existentes de otras tareas.
2. Confirmar stack, comportamiento actual, consumidores y límites del cambio.
   Identificar acoplamientos y riesgos antes de elegir una implementación.
3. Implementar el cambio mínimo completo en el componente correcto, con nombres
   claros y separación de responsabilidades. Mantener decisiones de presentación
   fuera del núcleo y evitar mezclar refactorizaciones ajenas a la tarea.
4. Añadir o ajustar pruebas JUnit cuando protejan comportamiento, invariantes o
   regresiones reales. Probar el núcleo sin JavaFX y los adaptadores por separado.
   Para i18n/temas, comprobar claves y comportamiento visual; para CLI, verificar
   también su contrato de proceso.
5. Ejecutar verificaciones proporcionales al cambio. Para código Java usar
   `mvn test` y, cuando afecte empaquetado o integración, `mvn -DskipTests package`.
   Una prueba focalizada no equivale a la suite completa; documentar límites.
   Para cambios solo documentales, revisar enlaces, coherencia y
   `git diff --check`, sin exigir pruebas de ejecución innecesarias.
6. Actualizar las guías de uso, arquitectura o exportación afectadas. Distinguir
   siempre capacidades implementadas, arquitectura objetivo y trabajo pendiente.
7. Revisar el diff final y reportar en español qué cambió, por qué, qué se verificó
   y cualquier limitación material. Mantener cada entrega dentro de su alcance.

## Contratos específicos del proyecto

- Preservar la diferencia entre guardar un proyecto editable y exportar un
  fragmento ERPNext; seguir `docs/EXPORT_AND_SAVE.md`.
- Conservar medidas exportadas en milímetros, tablas HTML reales, metadata
  editable solo donde corresponda y ausencia del fondo original en exportación.
- Mantener la compatibilidad con proyectos guardados y plantillas ERPNext/Jinja.
  Proteger esos contratos con pruebas cuando se modifique su implementación.
