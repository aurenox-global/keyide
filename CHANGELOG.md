# Changelog

Todos los cambios relevantes de KeyIDE. Formato basado en
[Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y
[Versionado semántico](https://semver.org/lang/es/).

## [0.13.0] — 2026-09-20

### Añadido
- **i18n (ES/EN)**: textos de la interfaz movidos a **recursos** (`values/strings.xml` +
  `values-en/strings.xml`). Los paneles (explorador, terminal, Git, copiloto, ajustes,
  búsqueda/reemplazo, títulos de hoja) ya usan `getString(...)`.
- **Selector de idioma**: **Sistema / Español / English** (vía
  `AppCompatDelegate.setApplicationLocales`), desde la paleta de comandos
  (*Ver: Idioma (Sistema / ES / EN)…*).

### Notas
- Quedan textos por migrar (títulos de comandos de la paleta y diálogos largos) para
  completar la i18n; se hará en la siguiente pasada.

## [0.12.0] — 2026-09-20

### Añadido
- **Tema global de la app**: **Oscuro / Claro / Sistema**. Nueva paleta global (`ui/Ui.kt`) +
  estilo `Theme.KeyIDE.Light`; **todos los paneles** (editor, explorador, terminal, Git, IA,
  ajustes, búsqueda, paleta, previsualización) migrados de colores fijos a la paleta.
  Selector en la paleta de comandos: *Ver: Tema de la app (oscuro/claro)…*.
- **Tests unitarios** (JUnit) de la lógica pura: Markdown, Snippets, proveedores de IA y
  temas del editor (`app/src/test/java/com/keyide/app/CoreTest.kt`).

### Notas
- Queda pendiente la **i18n (ES/EN)**: es una pasada mecánica de strings (próximo paso).

## [0.11.0] — 2026-09-20

### Añadido
- **LSP-lite ampliado**: **hover** (información del símbolo con su definición), **buscar
  referencias** (precarga la búsqueda en el proyecto con la palabra del cursor) y
  **renombrar símbolo** (todas las apariciones del identificador en el documento).
- **Formateo de documento**: re-indentado por anidamiento de `{} [] ()` para
  JavaScript/TypeScript/Java/Kotlin/JSON/CSS (paleta: *Edición: Formatear documento*).
- **Snippets por lenguaje**: plantillas (function, for, try/catch, def, class, HTML5,
  regla CSS, shebang…) que se insertan en el cursor (paleta: *Insertar snippet…*).

### Corregido
- **Git ahora opera sobre la carpeta activa del explorador**, no sobre el proyecto interno
  fijo. Avisa cuando la carpeta abierta es SAF (JGit necesita un filesystem real).
- **El explorador muestra el nombre de la carpeta**: en la barra de ruta (ahora en su propia
  fila, ya no se corta) y en el título del panel (*Explorador · <carpeta>*).

## [0.10.0] — 2026-09-20

### Añadido
- **Node-lite en el runner de JS**: mini **CommonJS** implementado en JS sobre V8.
  `require()` de módulos **locales** (rutas relativas `./x`, `../y`, con `.js` o `index.js`),
  `module.exports`, `process` (`argv`, `env`, `cwd()`, `platform`, `version`) y `__filename`.
  No hay npm ni módulos nativos: solo ficheros del proyecto.
- **Ir a definición (LSP-lite)**: con `Ctrl+B` o desde la paleta, busca la definición del
  nombre bajo el cursor en el documento y, si no está, en el proyecto (interno o SAF), y
  salta a esa línea.

### Cambiado
- El terminal muestra cuántos módulos locales carga al ejecutar JS.

### Publicación
- **Web del proyecto** (`docs/index.html`, lista para GitHub Pages) con toda la documentación.
- **Capturas reales** en `docs/screenshots/` (editor, split+preview, teclas, explorador SAF,
  terminal, Git, copiloto), integradas en la web y en el README.
- **README** para GitHub con badges, tabla de funciones y enlaces.
- **Release firmado**: `keystore/keyide-release.jks` (local, *gitignored*) y firma configurada
  vía `keystore.properties`.
- Script `keyide-publish.sh` para republicar código + release con un comando.

## [0.9.0] — 2026-09-20

### Añadido
- **Temas del editor**: *One Dark*, *Monokai* y *Claro (GitHub)*, con paleta aplicada al
  código, al margen y al resaltado de sintaxis.
- **Tamaño de fuente** configurable (10–24sp).
- **Persistencia de sesión**: al reabrir la app se restauran las **pestañas abiertas** y la
  pestaña activa.
- **Terminal con sesión persistente** (`ShellSession` sobre `/system/bin/sh`): el proceso
  queda vivo, así que `cd`, variables de entorno y estado se **mantienen entre comandos**.
  Botón *Limpiar* y aviso de sesión terminada.

### Cambiado
- El terminal ya no lanza cada comando por separado (se eliminó `TerminalRunner`).

## [0.8.0] — 2026-09-20

### Añadido
- **Buscar y reemplazar** en el documento: resalta todas las coincidencias, navegación
  anterior/siguiente, **Reemplazar** y **Reemplazar todo**, con opción *sensible a
  mayúsculas* (`Aa`).
- **Buscar en el proyecto**: recorre la ubicación actual (interna o carpeta SAF) y lista
  `fichero:línea` con vista previa; al pulsar, abre el fichero **en esa línea**.
- **Operaciones de fichero**: **Guardar como…**, **Renombrar…** y **Borrar…** del documento
  actual, tanto en el proyecto interno como en documentos SAF.
- **Deshacer / Rehacer**: botones `↶ ↷` en la barra de símbolos, en la paleta y con `Ctrl+Z`.
- Icono de **búsqueda** en el toolbar y atajo **`Ctrl+F`**.

### Cambiado
- La barra de símbolos incorpora `↶ ↷ 🔍` al principio.

## [0.7.1] — 2026-09-20

### Corregido
- **Barra inferior con el teclado abierto**: ahora la barra (teclas + navegación) queda
  **pegada justo encima del teclado** y ya **no se estira**. El inset inferior se aplica
  como *padding del contenedor* (desplaza toda la columna) en lugar de rellenar la propia
  barra, que era lo que la engordaba.

## [0.7.0] — 2026-09-20

### Añadido
- **Explorador con SAF**: elegir cualquier **carpeta real del dispositivo**
  (`ACTION_OPEN_DOCUMENT_TREE`), con permiso persistente. Navegar por subcarpetas,
  **abrir** ficheros y **crear** ficheros/carpetas en esa ubicación. Aviso en el título
  de si el documento es `interno` o `SAF`.
- **Acerca de** (menú y paleta): créditos, versión y licencia — *Creado por Andrés Mag*.
- **Documentación del proyecto**: `README.md`, `CHANGELOG.md`, `docs/ARCHITECTURE.md`,
  `docs/FEATURES.md`, `docs/BUILDING.md`, `docs/ROADMAP.md` y `LICENSE`.
- Atajo **Ctrl+O** para abrir carpeta; entradas nuevas en la paleta.

### Cambiado
- **UI del explorador** rediseñada: barra de ruta con botones `Abrir carpeta`, `+ Fichero`,
  `+ Carpeta` y `⬆` para subir de nivel. Listado tipo gestor de archivos (carpetas primero).
- Modelo de documento unificado: una pestaña puede apuntar a un fichero interno **o** a un
  documento SAF (lectura/escritura vía `ContentResolver`).
- `About` usa `BuildConfig` para la versión (se activó `buildConfig`).

### Notas
- APK ~39 MB (incluye runtime de Python para 3 ABIs).

## [0.6.0] — 2026-09-20

### Añadido
- **Dependencias Python empaquetadas (pip)**: se instalan en *build-time* y viajan dentro
  del APK. Probado con `requests` (+ urllib3, certifi, idna, charset-normalizer).
- Fichero de ejemplo **`prueba_api.py`** (versión de Python + `requests.get`).
- Documentación interna del DSL de Chaquopy (bloque `chaquopy { }` de nivel de proyecto).

## [0.5.0] — 2026-09-20

### Añadido
- **Git nativo con JGit** (sin binario externo): Estado, Init, Commit, Log, Pull, Push y
  Clone. Campos de mensaje, URL remota, usuario y token. Autor/committer configurables.

## [0.4.0] — 2026-09-20

### Añadido
- **Multi-archivo real con pestañas** (buffer independiente, indicador `●`, cerrar con `✕`
  o `Ctrl+W`).
- **Split editor | vista previa** (layout #3): toggle manual y activación automática en
  tablet/horizontal.

## [0.3.0] — 2026-09-20

### Añadido
- **Python embebido (Chaquopy)**: *Run* sobre `.py` ejecuta de verdad y muestra la salida.
- **JavaScript real** mediante el motor V8 del WebView (console → terminal).
- **Autocompletado LSP-lite** (keywords/builtins + palabras del documento).
- **Auto-cierre** de paréntesis y comillas con *skip over*.

## [0.2.0] — 2026-09-20

### Añadido
- **Vista previa** (WebView) para HTML/CSS/JS/SVG y render de Markdown.
- **Ajustes del copiloto**: proveedores DeepSeek, OpenAI/ChatGPT, Anthropic/Claude, Qwen,
  Kimi, GLM, **modelo local llama.cpp/ollama** y personalizado, con test de conexión.
- Soporte **Anthropic** nativo (`/messages`) además de OpenAI-compatible.
- Auto-indentación, ajuste de línea, ir a línea; atajos `Ctrl+K/S/P`.

### Corregido
- **UI solapada con la barra de estado**: edge-to-edge + manejo de insets
  (`setDecorFitsSystemWindows(false)` + `WindowInsetsCompat`).

## [0.1.0] — 2026-09-20

### Añadido
- Esqueleto inicial: editor propio (números de línea, gutter, resaltado regex), explorador
  de proyecto, terminal integrado, panel Git, copiloto IA, paleta de comandos y bottom nav
  de 5 secciones (Ficheros · Teclas · Run · Git · AI).
- 5 wireframes ASCII previos para elegir el diseño (`android-ide/wireframes/`).
