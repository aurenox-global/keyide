# Funciones

## Editor
- Números de línea y **gutter** con marcas de diagnóstico (error/warning/info).
- **Resaltado de sintaxis** por regex para JS/TS, Kotlin, Java, Python, HTML, CSS, JSON,
  Markdown, shell y XML (se degrada a texto plano en otros).
- **Auto-indentación** al pulsar Enter (mantiene sangría; añade una extra tras `{`).
- **Auto-cierre** de `( [ { " ' \`` con *skip over* cuando el cierre ya existe.
- **Autocompletado LSP-lite**: desplegable con palabras clave/builtins del lenguaje y todas
  las palabras del documento (a partir de 2 caracteres).
- **Ajuste de línea** conmutable; **ir a línea**; scroll horizontal cuando no ajusta.

## Multi-archivo
- **Pestañas** con buffer independiente y `●` de cambios sin guardar.
- Cerrar con `✕` o `Ctrl+W`.

## Vista previa y split
- **WebView** para HTML/CSS/JS/SVG (rutas relativas resueltas con `baseURL`).
- **Markdown** renderizado a HTML con estilos propios.
- **Split** editor | vista previa: botón en el toolbar, paleta, o automático en
  tablet/horizontal. En móvil vertical la vista previa se abre a pantalla completa.

## Ejecución (Run)
- **JavaScript** → motor V8 del WebView; `console.log/warn/error` van al terminal.
  Incluye un **mini CommonJS**: `require()` de módulos **locales** (`./x`, `../y`, `.js` o
  `index.js`), `module.exports`, `process` (`argv`, `env`, `cwd()`), `__filename`.
  (Sin npm ni módulos nativos.)
- **Python** → intérprete embebido (Chaquopy); stdout/stderr al terminal.
- **Shell** (`.sh`) y comandos libres en el **terminal integrado**.

## Git (JGit)
- Estado (rama, remoto, cambios `M`/`??`), Init, Commit, Log, Pull, Push y Clone.
- Credenciales (usuario/token) y autor/committer configurables.

## Copiloto IA
- Proveedores: **DeepSeek, OpenAI/ChatGPT, Anthropic/Claude, Qwen, Kimi, GLM,
  modelo local llama.cpp/ollama** y personalizado.
- Formato OpenAI-compatible (`/chat/completions`) y Anthropic (`/messages`).
- El fichero actual se envía como contexto; botón **Probar** para verificar la conexión.

## Ficheros (SAF)
- Abrir cualquier **carpeta real** del dispositivo con permiso persistente.
- Navegar por subcarpetas, **abrir**, **crear ficheros** y **crear carpetas**.
- Modo interno (proyecto de ejemplo) como alternativa sin permisos.

## Buscar y reemplazar
- **En el documento**: resaltado de todas las coincidencias, ir a anterior/siguiente,
  reemplazar una o **todas**, y modo sensible a mayúsculas.
- **En el proyecto**: recorre la ubicación actual (interna o SAF) y lista `fichero:línea`
  con vista previa; al tocar, abre el fichero en esa línea.
- Atajo **`Ctrl+F`**; icono de lupa en el toolbar.

## Operaciones de fichero
- **Nuevo fichero**, **Guardar como…**, **Renombrar…** y **Borrar…** (interno y SAF).
- **Deshacer / Rehacer** (`↶ ↷` en la barra de símbolos, paleta o `Ctrl+Z`).

## Terminal
- **Sesión persistente** sobre `/system/bin/sh`: el proceso se mantiene vivo, así que
  `cd`, variables y estado **se conservan** entre comandos (a diferencia de ejecutar cada
  comando por separado).
- Botón *Limpiar*; la salida se vuelca en tiempo real.
- Nota: no es un PTY (sin control de trabajos ni apps de pantalla completa).

## Apariencia y sesión
- **Tema de la app**: Oscuro, Claro o Sistema (afecta a todos los paneles).
- **Temas del editor**: One Dark, Monokai y Claro (GitHub).
- **Tamaño de fuente** 10–24sp.
- **Persistencia**: se restauran las pestañas abiertas y la activa al reabrir.

## Navegación de código
- **Ir a definición** (LSP-lite): `Ctrl+B` o paleta. Busca el nombre bajo el cursor en el
  documento y, si no, en el proyecto (interno o SAF), y salta a esa línea.
- **Información del símbolo (hover)**: muestra la definición del nombre bajo el cursor.
- **Buscar referencias**: abre la búsqueda de proyecto con la palabra del cursor.
- **Renombrar símbolo**: renombra todas las apariciones del identificador en el documento.
- **Buscar en el proyecto** (ver más arriba) para localizar cualquier texto.

## Formateo y snippets
- **Formatear documento**: re-indenta por llaves (JS/TS/Java/Kotlin/JSON/CSS).
- **Snippets** por lenguaje insertables en el cursor.

## Comandos y atajos
- **Paleta** (`Ctrl+K`): todas las acciones del IDE.
- `Ctrl+S` guardar · `Ctrl+P` vista previa · `Ctrl+W` cerrar pestaña · `Ctrl+O` abrir carpeta · `Ctrl+F` buscar.
- Barra de símbolos para el pulgar (`{ } ( ) [ ] ; " ' = + - …`).
