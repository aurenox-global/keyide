package com.keyide.app.files

import android.content.Context
import java.io.File

/** Nodo plano del árbol de proyecto (con profundidad para la indentación). */
data class FileNode(val file: File, val depth: Int, val isDir: Boolean)

/**
 * Espacio de trabajo mínimo viable: un proyecto de ejemplo creado en el
 * almacenamiento interno de la app la primera vez que se abre.
 * (Paso siguiente: SAF/SAF-tree para abrir carpetas reales del dispositivo.)
 */
object WorkspaceRepo {

    fun root(context: Context): File = File(context.filesDir, "projects/demo")

    fun ensure(context: Context) {
        val r = root(context)
        if (!r.exists()) r.mkdirs()
        val src = File(r, "src"); if (!src.exists()) src.mkdirs()
        write(File(r, "main.js"), SAMPLE_MAIN_JS)
        write(File(r, "index.html"), SAMPLE_INDEX_HTML)
        write(File(r, "styles.css"), SAMPLE_CSS)
        write(File(src, "utils.js"), SAMPLE_UTILS_JS)
        write(File(r, "prueba_api.py"), SAMPLE_PY)
        write(File(r, "README.md"), SAMPLE_README)
    }

    private fun write(f: File, content: String) {
        if (!f.exists()) f.writeText(content)
    }

    /** Aplana el árbol respetando las carpetas colapsadas. */
    fun flatten(root: File, collapsed: Set<String>): List<FileNode> {
        val out = ArrayList<FileNode>()
        fun walk(dir: File, depth: Int) {
            val children = dir.listFiles() ?: return
            children
                .filter { !it.name.startsWith(".") }
                .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                .forEach { f ->
                    out.add(FileNode(f, depth, f.isDirectory))
                    if (f.isDirectory && f.absolutePath !in collapsed) walk(f, depth + 1)
                }
        }
        walk(root, 0)
        return out
    }

    private val SAMPLE_MAIN_JS = """
        // KeyIDE · proyecto de ejemplo
        import { greet } from "./src/utils.js"

        function main() {
          const name = "Andrés"
          console.log(greet(name))
          // TODO: conectar con la API
        }

        main()
    """.trimIndent()

    private val SAMPLE_UTILS_JS = """
        export function greet(name) {
          return `Hola, ${'$'}{name} — desde KeyIDE`
        }
    """.trimIndent()

    private val SAMPLE_INDEX_HTML = """
        <!DOCTYPE html>
        <html lang="es">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <title>KeyIDE demo</title>
          <link rel="stylesheet" href="styles.css">
        </head>
        <body>
          <h1>Hola desde KeyIDE 🧠</h1>
          <script src="main.js"></script>
        </body>
        </html>
    """.trimIndent()

    private val SAMPLE_CSS = """
        :root { color-scheme: dark; }
        body {
          margin: 0;
          font-family: system-ui, sans-serif;
          background: #0d1117;
          color: #e6edf3;
          display: grid;
          place-items: center;
          height: 100vh;
        }
        h1 { color: #58a6ff; }
    """.trimIndent()

    private val SAMPLE_PY = """
        # Prueba del Python embebido + dependencia externa (requests)
        import sys
        print("Python", sys.version.split()[0], "en el móvil")

        try:
            import requests
            r = requests.get("https://api.github.com", timeout=10)
            print("requests OK -> HTTP", r.status_code)
        except Exception as e:
            print("Sin requests:", e)

        for i in range(1, 4):
            print("linea", i, "->", i * i)
    """.trimIndent()

    private val SAMPLE_README = """
        # demo

        Proyecto de ejemplo creado por KeyIDE.

        - `main.js` — punto de entrada
        - `src/utils.js` — utilidades
        - `index.html` + `styles.css` — web

        FIXME: añadir build/test on-device.
    """.trimIndent()
}
