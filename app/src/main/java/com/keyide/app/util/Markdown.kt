package com.keyide.app.util

/** Render de Markdown → HTML mínimo (para la vista previa de .md). */
object Markdown {

    fun toHtml(md: String): String {
        val sb = StringBuilder()
        var inCode = false
        var inList = false

        for (raw in md.lines()) {
            val line = raw
            if (line.trimStart().startsWith("```")) {
                if (!inCode) {
                    closeList(sb, inList); inList = false
                    sb.append("<pre><code>")
                } else {
                    sb.append("</code></pre>")
                }
                inCode = !inCode
                continue
            }
            if (inCode) {
                sb.append(escape(line)).append('\n')
                continue
            }
            val trimmed = line.trim()
            when {
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    if (!inList) { sb.append("<ul>"); inList = true }
                    sb.append("<li>").append(inline(trimmed.substring(2))).append("</li>")
                }
                trimmed.startsWith("#") -> {
                    closeList(sb, inList); inList = false
                    val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 6)
                    val text = trimmed.drop(level).trim()
                    sb.append("<h").append(level).append(">").append(inline(text))
                        .append("</h").append(level).append(">")
                }
                trimmed.startsWith("> ") -> {
                    closeList(sb, inList); inList = false
                    sb.append("<blockquote>").append(inline(trimmed.substring(2))).append("</blockquote>")
                }
                trimmed.isEmpty() -> {
                    closeList(sb, inList); inList = false
                }
                else -> {
                    closeList(sb, inList); inList = false
                    sb.append("<p>").append(inline(trimmed)).append("</p>")
                }
            }
        }
        closeList(sb, inList)
        if (inCode) sb.append("</code></pre>")

        return """
            <!DOCTYPE html><html><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
              body{font-family:system-ui,-apple-system,sans-serif;line-height:1.6;
                   padding:16px;color:#e6edf3;background:#0d1117}
              h1,h2,h3{border-bottom:1px solid #30363d;padding-bottom:.3em}
              a{color:#58a6ff}
              code{background:#1c2128;padding:2px 5px;border-radius:4px;font-family:monospace}
              pre{background:#161b22;padding:12px;border-radius:8px;overflow:auto}
              pre code{background:none;padding:0}
              blockquote{border-left:3px solid #30363d;margin:0;padding-left:12px;color:#8b949e}
            </style></head><body>
            $sb
            </body></html>
        """.trimIndent()
    }

    private fun closeList(sb: StringBuilder, inList: Boolean) {
        if (inList) sb.append("</ul>")
    }

    private fun inline(s: String): String {
        var out = escape(s)
        out = out.replace(Regex("`([^`]+)`"), "<code>$1</code>")
        out = out.replace(Regex("\\*\\*([^*]+)\\*\\*"), "<b>$1</b>")
        out = out.replace(Regex("\\*([^*]+)\\*"), "<i>$1</i>")
        out = out.replace(Regex("\\[([^]]+)]\\(([^)]+)\\)"), "<a href=\"$2\">$1</a>")
        return out
    }

    private fun escape(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}
