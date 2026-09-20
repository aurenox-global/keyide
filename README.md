# KeyIDE 🧠

**IDE de código ligero para Android** — editor con pestañas, vista previa web, ejecución
real de JavaScript y Python, terminal integrado, Git nativo y copiloto de IA.

> **Creado por Andrés Mag** · © 2026 Andrés Mag

---

## ✨ Qué es

KeyIDE nace de analizar lo que **falta** en los mejores editores de Android (Acode,
Squircle IDE, DroidEdit, Spck, AIDE, CxxDroid, Pydroid) y en los entornos en la nube
(Codespaces, Replit). Sus carencias repetidas —multi-archivo real, autocompletado,
diagnósticos, terminal, build/run on-device, git y IA— son justo lo que KeyIDE cubre.

Interfaz **mobile-first**: el editor manda y las herramientas viven en *bottom sheets*
deslizables; con teclado/pantalla ancha se activa el **split editor | vista previa**.

## Características

| Área | Qué incluye |
|------|-------------|
| **Editor** | Números de línea, gutter con diagnósticos, resaltado de sintaxis (JS/TS/Kotlin/Java/Python/HTML/CSS/JSON/MD/sh/XML), auto-indentación, auto-cierre de paréntesis/comillas, ajuste de línea, ir a línea. |
| **Multi-archivo** | Pestañas con buffer independiente e indicador de cambios (`●`). |
| **Autocompletado** | LSP-lite: palabras clave/builtins del lenguaje + todas las palabras del documento. |
| **Run real** | JavaScript (motor V8 del WebView) y **Python embebido** (Chaquopy) — salida en el terminal. |
| **Vista previa** | WebView para HTML/CSS/JS/SVG y render de Markdown. |
| **Split** | Editor y vista previa lado a lado (auto en tablet/horizontal). |
| **Terminal** | Shell on-device (`/system/bin/sh`). |
| **Git** | Nativo con **JGit** (sin binario externo): estado, init, commit, log, pull, push, clone. |
| **IA** | Copiloto multi-proveedor: DeepSeek, OpenAI/ChatGPT, Anthropic/Claude, Qwen, Kimi, GLM y **modelo local llama.cpp/ollama**. |
| **Ficheros** | Explorador con acceso a **carpetas reales del dispositivo (SAF)**: abrir, crear ficheros y carpetas, navegar. |
| **Paleta** | Comandos con `Ctrl+K`; atajos `Ctrl+S/P/W/O`. |

## 📱 Capturas

_(añadir capturas en `docs/screenshots/`)_

## 🚀 Compilar

```bash
./gradlew assembleDebug
# APK -> app/build/outputs/apk/debug/app-debug.apk
```

Requisitos: JDK 17, Android SDK 35. Ver [`docs/BUILDING.md`](docs/BUILDING.md).

## 🧱 Stack

Kotlin · Android Views + Material 3 · AGP 8.7.3 · Gradle 8.13 · compileSdk 35 · minSdk 26.
Librerías: Chaquopy (Python), JGit (Git), DocumentFile (SAF), AndroidX.

## 📚 Documentación

- [`CHANGELOG.md`](CHANGELOG.md) — historial de cambios por versión.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — arquitectura y estructura del código.
- [`docs/FEATURES.md`](docs/FEATURES.md) — detalle funcional.
- [`docs/BUILDING.md`](docs/BUILDING.md) — compilación y dependencias Python (pip).
- [`docs/ROADMAP.md`](docs/ROADMAP.md) — lo que queda por hacer.

## 📄 Licencia

MIT © 2026 **Andrés Mag**. Ver [`LICENSE`](LICENSE).
