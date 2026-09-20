# Roadmap

## Corto plazo
- [x] **Buscar y reemplazar** (en fichero) y **buscar en el proyecto**.
- [x] **Guardar como / renombrar / borrar**.
- [x] **Temas y tamaño de fuente**; **persistir pestañas**.
- [x] **Terminal con sesión persistente** (no PTY).
- [x] **Node-lite** (`require` de módulos locales en el runner JS).
- [x] **Ir a definición** (LSP-lite).
- [x] **Hover, referencias y renombrar** (LSP-lite).
- [x] **Formateo de documento y snippets**.
- [ ] **LSP real** por proceso (hover semántico, referencias exactas, renombrar en proyecto).
- [ ] **PTY real** (control de trabajos, apps de pantalla completa).
- [x] **Temas claros globales** (oscuro/claro/sistema) y **tests unitarios**.
- [x] **i18n (ES/EN)** + selector de idioma (quedan algunos textos por migrar).
- [x] **PTY real** (control de trabajos, apps de pantalla completa) — experimental, con fallback.
- [x] **Depurador de JavaScript** (puntos de parada + traza).
- [x] **Depurador de Python** (sys.settrace en Chaquopy).
- [ ] **Node.js real** (npm / módulos nativos).
- [ ] **Build on-device** (Gradle/Kotlin).
- [ ] **i18n**: terminar de migrar los textos restantes (paleta/diálogos).
- [ ] **Release firmado + subir a GitHub** (repo, CI, capturas).

## Medio plazo
- [ ] **Node.js embebido** (motor JS con `require`/npm) para Run de Node real.
- [ ] **Terminal interactivo** (PTY) en vez de comandos sueltos.
- [ ] **Debugger on-device** (breakpoints) para Python/JS.
- [ ] **Temas** (claro/oscuro y paletas) y tamaño de fuente configurable.
- [ ] **Git con diff** visual y resolución de conflictos básica.

## Largo plazo
- [ ] **Build on-device** (Gradle/Kotlin) para proyectos Android.
- [ ] **Extensiones/plugins** (API pública).
- [ ] **Sincronización** opcional de ajustes y snippets.
- [ ] Soporte **tablet** con layout de tres paneles (#5) y teclados externos.

## Ideas
- [ ] Resaltado semántico con árbol de sintaxis real.
- [ ] Multi-cursor y edición en columna.
- [ ] Integración con **llama.cpp** local con descubrimiento automático en la LAN.
