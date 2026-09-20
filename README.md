# KeyIDE 🧠

**IDE de código ligero para Android** — editor con pestañas, vista previa web, **ejecución
real de JavaScript y Python**, terminal, **Git** y copiloto de IA.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)](#)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](#)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Release](https://img.shields.io/github/v/release/aurenox-global/keyide?label=release)](https://github.com/aurenox-global/keyide/releases/latest)

> **Creado por Andrés Mag** · © 2026 · MIT

🌐 **Web del proyecto:** https://aurenox-global.github.io/keyide/ ·
⬇️ **[Descargar APK](https://github.com/aurenox-global/keyide/releases/latest)**

---

## ✨ Qué es

KeyIDE nace de analizar lo que **falta** en los mejores editores de Android (Acode,
Squircle IDE, DroidEdit, Spck, AIDE, CxxDroid, Pydroid) y en los entornos en la nube
(Codespaces, Replit). Sus carencias repetidas —multi-archivo real, autocompletado,
diagnósticos, terminal, run on-device, Git e IA— son justo lo que KeyIDE cubre.

Interfaz **mobile-first**: el editor manda y las herramientas viven en *bottom sheets*
deslizables; con pantalla ancha se activa el **split editor | vista previa**.

## Características

| Área | Qué incluye |
|------|-------------|
| **Editor** | Números de línea, gutter con diagnósticos, resaltado de sintaxis, auto-indentación, auto-cierre de paréntesis/comillas, ajuste de línea, ir a línea. |
| **Autocompletado** | LSP-lite: palabras clave/builtins del lenguaje + todas las palabras del documento. |
| **Multi-archivo** | Pestañas con buffer independiente e indicador de cambios (`●`). |
| **Run real** | JavaScript (motor V8 del WebView, con `require` de módulos locales) y **Python embebido** (Chaquopy). |
| **Vista previa** | WebView para HTML/CSS/JS/SVG y render de Markdown; en split lateral o pantalla completa. |
| **Terminal** | Sesión de shell **persistente** (`cd` y entorno se conservan). |
| **Git** | Nativo con **JGit**: estado, init, commit, log, pull, push y clone. |
| **IA** | DeepSeek, OpenAI/ChatGPT, Anthropic/Claude, Qwen, Kimi, GLM y **modelo local llama.cpp**. |
| **Ficheros** | Explorador con acceso a **carpetas reales del dispositivo (SAF)**: abrir, crear ficheros y carpetas. |
| **Búsqueda** | Buscar y reemplazar en el documento y en el proyecto; **ir a definición**. |
| **Apariencia** | Temas (One Dark, Monokai, Claro), tamaño de fuente y **persistencia de sesión**. |

## 📱 Capturas

_(añadir capturas en `docs/screenshots/`)_

## 🚀 Instalar

Descarga el APK desde [Releases](https://github.com/aurenox-global/keyide/releases/latest).

## 🛠 Compilar

```bash
git clone https://github.com/aurenox-global/keyide.git
cd keyide
echo "sdk.dir=/ruta/a/Android/Sdk" > local.properties
./gradlew assembleDebug
# APK -> app/build/outputs/apk/debug/app-debug.apk
```

Requisitos: JDK 17 + Android SDK 35. Detalle en [`docs/BUILDING.md`](docs/BUILDING.md).

### Dependencias Python (pip)

Se empaquetan **al compilar** (Chaquopy), no en tiempo de ejecución. Se declaran con el
bloque de nivel de proyecto `chaquopy` (el `python { }` anidado **no** compila en Kotlin DSL):

```kotlin
chaquopy {
    defaultConfig {
        pip { install("requests") }
    }
}
```

## 🧱 Stack

Kotlin · Android Views + Material 3 · AGP 8.7.3 · Gradle 8.13 · compileSdk 35 · minSdk 26.
Chaquopy (Python) · JGit (Git) · DocumentFile (SAF) · AndroidX.

## 📚 Documentación

- 🌐 [Web del proyecto](https://aurenox-global.github.io/keyide/)
- [`CHANGELOG.md`](CHANGELOG.md) — historial por versión.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) · [`docs/FEATURES.md`](docs/FEATURES.md) ·
  [`docs/BUILDING.md`](docs/BUILDING.md) · [`docs/ROADMAP.md`](docs/ROADMAP.md)

## ⌨️ Atajos

`Ctrl+K` paleta · `Ctrl+S` guardar · `Ctrl+F` buscar · `Ctrl+B` ir a definición ·
`Ctrl+O` abrir carpeta · `Ctrl+P` vista previa · `Ctrl+W` cerrar pestaña · `Ctrl+Z` deshacer.

## 📄 Licencia

MIT © 2026 **Andrés Mag**. Ver [`LICENSE`](LICENSE).
