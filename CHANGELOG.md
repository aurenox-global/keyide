# Changelog

Todos los cambios relevantes de KeyIDE. Formato basado en
[Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y
[Versionado semántico](https://semver.org/lang/es/).

## [0.19.2] — 2026-09-21

### Corregido
- **Barra inferior con el teclado (medición robusta)**: en vez de fiarnos de los *insets*
  (que en algunas versiones se suman al redimensionado y mandaban la barra demasiado arriba),
  ahora se mide la **altura real que tapa el teclado** con `getWindowVisibleDisplayFrame`
  (overlap = altura de la vista − frame visible). Funciona igual si el sistema redimensiona la
  ventana o si no → **sin doble ajuste**; la barra queda justo encima del teclado.
- **Interruptor de seguridad**: paleta → *Ver: Ocultar barra al escribir (ON/OFF)*. Si se
  activa, la barra de navegación se **oculta** mientras escribes (máximo espacio de código).
  Por defecto OFF.

## [0.19.1] — 2026-09-21

### Corregido
- **Barra inferior con el teclado abierto (doble ajuste)**: en **Android < 15** el sistema ya
  encoge la ventana (`adjustResize`) **y** además sumábamos el inset del teclado → las barras
  se iban **demasiado arriba** y tapaban el editor. Ahora: en **API ≥ 30** usamos los *insets*
  del teclado (el sistema no redimensiona); en **API < 30** confiamos en el redimensionado
  nativo (sin doble ajuste). Resultado: la barra queda **pegada encima del teclado** sin
  invadir el código.
- Con el teclado abierto se **oculta la barra de símbolos** (es redundante) para dar más
  espacio al código; se restaura al cerrarlo.

## [0.19.0] — 2026-09-21

### Añadido
- **Compilar / Build (on-device)**: panel nuevo que **detecta el tipo de proyecto** y lanza
  acciones reales en el dispositivo:
  - **Comprobar sintaxis Python** (`py_compile` sobre todos los `.py`).
  - **Comprobar sintaxis JavaScript** (con Node: `vm.Script` sobre los `.js`, excluyendo
    `node_modules`).
  - **npm**: `npm install` y `npm run <script>` — **npm 6.14.18 embebido** en `assets/npm`,
    ejecutado por el Node embebido (con `--prefix`, caché en el dispositivo).
  - **Ejecutar `build.sh`** en el terminal (persistente/PTY).
  - **Gradle/Kotlin/Java**: aviso honesto — requiere un **JDK**, no disponible on-device.
- Se amplió el protocolo del servicio Node (comandos JSON `{script,args}`) para poder
  ejecutar scripts con argumentos (`process.argv`).

### Web
- **Sitio rediseñado: dinámico y llamativo** (`docs/index.html`): fondo animado con red de
  partículas, blobs, **barra de progreso** de scroll, **terminal con animación de tecleo**,
  contadores animados, tarjetas con glow, **carrusel de capturas** en marcos de móvil,
  **timeline del changelog** y *reveal on scroll*. Todo vanilla (sin dependencias).

### Notas
- El panel no compila proyectos Android/Gradle en el móvil (imposible sin JVM); para eso
  ofrece el terminal (Termux) o compilar en el PC.

## [0.18.0] — 2026-09-20

### Añadido
- **Node.js REAL embebido (`nodejs-mobile`)**: `libnode.so` (~43 MB por ABI) + puente JNI
  `libkeyidenode.so` que arranca `node::Start` en un hilo de 8 MB y mantiene un **servicio
  Node persistente**: el bootstrap lee rutas de scripts por `stdin` y las ejecuta con
  `require` (limpiando la caché). Salida capturada por tubería + `libc++_shared.so`.
  → Con esto, el *Run* de JS puede **`require` de `node_modules` reales (npm)**.
- **Motor JS seleccionable**: *Ver: Motor JS (V8 / Node)…* (V8 por defecto). **Fallback
  automático a V8** si Node no arranca.
- Script `scripts/fetch-nodejs-mobile.sh` para restaurar los binarios (van en `.gitignore`
  por tamaño).

### Notas
- **El APK pasa a ~148 MB** (3 ABIs con `libnode`). El plugin Gradle de janeasystems ya no
  existe → la integración es manual (JNI + `libnode.so`).
- Compila y empaqueta correctamente; **no probado en dispositivo** (verificar `Run` con Node).

## [0.17.0] — 2026-09-20

### Cambiado
- **i18n cerrada al 100%**: migrados a recursos los **36 títulos de la paleta de comandos** y
  los **diálogos** (Acerca de, Ir a línea, Nuevo fichero/carpeta, Guardar como, Renombrar,
  Borrar, Insertar snippet, tema de editor/app e idioma) junto con sus botones.
  Completo en **ES** (`values/`) y **EN** (`values-en/`).

## [0.16.0] — 2026-09-20

### Añadido
- **Depurador de Python**: los mismos **puntos de parada** (toque en el margen) y **traza**,
  implementados con `sys.settrace` en el intérprete embebido (`runner.execute_debug`).
  La ejecución se detiene en la línea marcada de tu código (también dentro de funciones).

### Notas
- JS y Python comparten la traza y los puntos de parada (paleta: *Depurar: …*).

## [0.15.0] — 2026-09-20

### Añadido
- **Depurador de JavaScript (ligero)**: **puntos de parada** tocando el **margen** del editor
  (punto rojo) y **traza** de las líneas ejecutadas. Al llegar a un punto de parada, la
  ejecución se detiene y lo indica con la línea. Paleta: *Depurar: ejecutar con puntos de
  parada / activar traza / limpiar puntos*. Sin puntos ni traza, la ejecución es normal.
- **Web del proyecto actualizada** con las novedades, features y changelog.

### Notas
- El depurador JS solo puede parar en líneas "completas" (no en continuaciones de expresión).

## [0.14.0] — 2026-09-20

### Añadido
- **Terminal con PTY REAL (experimental)**: código nativo (**NDK/JNI**, `libkeyidepty`)
  que abre un pseudo-terminal (`posix_openpt` + `grantpt`/`unlockpt` + `fork` + `sh -i`).
  Permite apps interactivas: control de trabajos, colores, `vim`/`htop`…
  El tamaño se fija con `ioctl(TIOCSWINSZ)` (40×100) y se limpian las secuencias ANSI.
- **Fallback automático**: si el PTY no está disponible, usa el shell persistente actual,
  así el terminal nunca se queda sin funcionar. El título indica `PTY` o `Sesión persistente`.

### Notas
- El PTY lleva **fallback**, pero **no se ha podido probar en un dispositivo real**
  (solo compila y empaqueta). Verificar en el móvil.

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
