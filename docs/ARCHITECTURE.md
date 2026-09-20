# Arquitectura

KeyIDE es una app Android nativa en **Kotlin** con **Views + Material 3** (sin Compose,
para minimizar dependencias y tamaño). Una única actividad orquesta paneles reutilizables.

## Vista general

```
MainActivity  (orquesta TODO)
├── CodeEditorView         editor + gutter (números y diagnósticos)
│   ├── GutterView
│   ├── Syntax             resaltado regex + keywords por lenguaje
│   └── Diagnostic         severidad/mensaje por línea
├── Bottom sheet (herramientas)
│   ├── FilesPanel         explorador (interno o SAF)
│   ├── TerminalPanel      shell on-device
│   ├── GitPanel           JGit
│   ├── AiPanel            copiloto (OpenAI-compatible / Anthropic)
│   └── SettingsPanel      proveedor, endpoint, modelo, API key
├── splitPreview (FrameLayout)  editor | PreviewPanel (WebView)
├── CommandPalette         comandos (Ctrl+K)
├── JsRunner               ejecuta JS en el WebView (V8)
└── PythonBridge           ejecuta Python (Chaquopy)
```

## Flujo de datos

- **Documentos**: la actividad mantiene `docs: List<Doc>`. Cada `Doc` apunta a un
  `File` (proyecto interno) o a un `Uri` (documento SAF) y guarda su buffer en memoria.
  Al cambiar de pestaña se hace `stashCurrent()` (vuelca el texto al `Doc`).
- **Guardar**: escribe a disco (`File.writeText`) o vía `ContentResolver.openOutputStream`
  para SAF.
- **Explorador**: dos modos con pila de navegación — `internalStack: List<File>` y
  `safStack: List<DocumentFile>`. Se listan hijos y se navega empujando/sacando de la pila.
- **Ejecución**: `Run` inspecciona la extensión y deriva a `JsRunner` (WebView), a
  `PythonBridge` (Chaquopy) o al `TerminalPanel` (shell).

## Decisiones técnicas

| Tema | Decisión | Motivo |
|------|----------|--------|
| UI | Views + Material 3 | Tamaño y estabilidad; sin Compose. |
| Python | Chaquopy (build-time) | No hay intérprete en Android; pip no existe en runtime. |
| JS | WebView V8 | Cero dependencias. |
| Git | JGit 5.13 | Java 8, compatible Android (JGit 6.x usa API Java 11 no disponible). |
| Ficheros | SAF + `DocumentFile` | Acceso a carpetas reales sin permisos peligrosos. |
| Autocompletado | LSP-lite in-process | Sin servidor; suficiente en móvil. |

## Estructura de carpetas

```
app/src/main/java/com/keyide/app/
├── MainActivity.kt
├── editor/    CodeEditorView · GutterView · Syntax · Diagnostic
├── files/     Entry · FilesPanel · WorkspaceRepo
├── terminal/  TerminalRunner · TerminalPanel
├── git/       GitClient · GitPanel
├── ai/        AiClient · AiPanel
├── run/       JsRunner · PythonBridge
├── preview/   PreviewPanel
├── settings/  SettingsPanel
├── palette/   CommandPalette
├── data/      Settings · AiProviders
└── util/      Markdown
app/src/main/python/runner.py     helper de ejecución Python
```
