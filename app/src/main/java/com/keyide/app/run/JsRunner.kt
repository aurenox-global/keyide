package com.keyide.app.run

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject

/**
 * Ejecutor REAL de JavaScript con el motor V8 del WebView.
 *
 * Incluye un **mini CommonJS** (`require` de módulos locales) y un
 * **depurador ligero**: puntos de parada (la ejecución se detiene en la línea
 * marcada) y **traza** (registra las líneas ejecutadas).
 * Sin npm ni módulos nativos.
 */
class JsRunner(context: Context) {

    private val web = WebView(context)
    private var onLine: ((String) -> Unit)? = null
    private var onDone: (() -> Unit)? = null
    private var onPause: ((Int) -> Unit)? = null

    init {
        @SuppressLint("SetJavaScriptEnabled")
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webViewClient = WebViewClient()
        web.addJavascriptInterface(ConsoleBridge(), "KeyIDEConsole")
    }

    private inner class ConsoleBridge {
        @JavascriptInterface
        fun emit(line: String) {
            when {
                line == DONE -> onDone?.invoke()
                line.startsWith(PAUSE) -> onPause?.invoke(line.removePrefix(PAUSE).toIntOrNull() ?: -1)
                else -> onLine?.invoke(line)
            }
        }
    }

    fun run(
        code: String,
        basePath: String,
        modules: Map<String, String>,
        breakpoints: Set<Int> = emptySet(),
        trace: Boolean = false,
        onLine: (String) -> Unit,
        onDone: () -> Unit,
        onPause: (Int) -> Unit = {}
    ) {
        this.onLine = onLine
        this.onDone = onDone
        this.onPause = onPause

        val modulesObj = JSONObject()
        modules.forEach { (k, v) -> modulesObj.put(k, v) }
        val modulesJson = modulesObj.toString().replace("</script>", "<\\/script>")
        val baseJson = JSONObject.quote(basePath)
        val body = if (breakpoints.isEmpty() && !trace) escape(code) else escape(instrument(code, breakpoints, trace))

        val html = """
            <!DOCTYPE html><html><head><meta charset="utf-8"></head><body>
            <script>
            (function () {
              function fmt(x) {
                try {
                  if (typeof x === 'string') return x;
                  if (x === undefined) return 'undefined';
                  return JSON.stringify(x);
                } catch (e) { return String(x); }
              }
              var emit = function (x) { KeyIDEConsole.emit(fmt(x)); };
              var console = { log: emit, info: emit, warn: emit, error: emit, debug: emit };

              var __modules = $modulesJson;
              var __cache = {};

              function __norm(from, req) {
                if (req.charAt(0) !== '.') return req;
                var base = from.split('/'); base.pop();
                var parts = req.split('/');
                for (var i = 0; i < parts.length; i++) {
                  var p = parts[i];
                  if (p === '.' || p === '') continue;
                  if (p === '..') base.pop(); else base.push(p);
                }
                return base.join('/');
              }
              function __resolve(from, req) {
                var id = __norm(from, req);
                if (__modules[id] !== undefined) return id;
                if (__modules[id + '.js'] !== undefined) return id + '.js';
                if (__modules[id + '/index.js'] !== undefined) return id + '/index.js';
                return id;
              }
              function __load(id) {
                if (__cache[id]) return __cache[id].exports;
                var src = __modules[id];
                if (src === undefined) throw new Error("No se encuentra el módulo '" + id + "'");
                var module = { exports: {} };
                __cache[id] = module;
                var fn = new Function('module', 'exports', 'require', 'console', 'process', '__filename', src);
                fn(module, module.exports,
                   function (r) { return __load(__resolve(id, r)); },
                   console, process, id);
                return module.exports;
              }

              var process = {
                argv: [$baseJson],
                env: {},
                platform: 'android',
                version: 'keyide-0.15.0',
                cwd: function () { return $baseJson; }
              };
              function __bp(n) { throw { __keyide_pause__: n }; }
              var module = { exports: {} };
              var require = function (r) { return __load(__resolve($baseJson, r)); };

              try {
                (function (module, exports, require, console, process) {
                  $body
                })(module, module.exports, require, console, process);
              } catch (e) {
                if (e && e.__keyide_pause__) {
                  KeyIDEConsole.emit('$PAUSE' + e.__keyide_pause__);
                } else {
                  KeyIDEConsole.emit('\u2716 ' + (e && e.stack ? e.stack : e));
                }
              }
              KeyIDEConsole.emit('$DONE');
            })();
            </script></body></html>
        """.trimIndent()

        web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    /** Prefija las líneas "seguras" con traza y/o punto de parada. */
    private fun instrument(code: String, breakpoints: Set<Int>, trace: Boolean): String {
        val lines = code.split('\n')
        val sb = StringBuilder()
        lines.forEachIndexed { i, l ->
            val n = i + 1
            val t = l.trimStart()
            val continuation = t.isEmpty() || t.startsWith(".") || t.startsWith(")") || t.startsWith("]") ||
                t.startsWith("}") || t.startsWith("else") || t.startsWith("catch") ||
                t.startsWith("finally") || t.startsWith("&&") || t.startsWith("||") ||
                t.startsWith("?") || t.startsWith(":") || t.startsWith("*") || t.startsWith("//") ||
                t.startsWith("/*")
            if (!continuation) {
                if (breakpoints.contains(n)) sb.append("__bp(").append(n).append(");")
                else if (trace) sb.append("emit('\u2192 L").append(n).append("');")
            }
            sb.append(l)
            if (i < lines.size - 1) sb.append('\n')
        }
        return sb.toString()
    }

    private fun escape(code: String) = code.replace("</script>", "<\\/script>")

    private companion object {
        const val DONE = "__KEYIDE_DONE__"
        const val PAUSE = "__KEYIDE_PAUSE__"
    }
}
