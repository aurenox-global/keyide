package com.keyide.app.editor

import android.graphics.Color
import android.text.Editable
import android.text.Spannable
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.EditText
import java.util.regex.Pattern

/**
 * Resaltado de sintaxis ligero (regex) para el editor.
 * No pretende sustituir a un LSP: es el "primer plano" visual mientras llega
 * el análisis semántico real.
 */
object Syntax {

    var BG = Color.parseColor("#0D1117")
    var FG = Color.parseColor("#C9D1D9")

    private var C_KEYWORD = Color.parseColor("#FF7B72")
    private var C_STRING = Color.parseColor("#A5D6FF")
    private var C_COMMENT = Color.parseColor("#6E7681")
    private var C_NUMBER = Color.parseColor("#79C0FF")
    private var C_TYPE = Color.parseColor("#FFA657")

    /** Aplica una paleta (tema) a los colores del resaltado. */
    fun applyTheme(t: EditorTheme) {
        BG = t.bg
        FG = t.fg
        C_KEYWORD = t.keyword
        C_STRING = t.string
        C_COMMENT = t.comment
        C_NUMBER = t.number
        C_TYPE = t.type
    }

    private val KEYWORDS = Pattern.compile(
        "\\b(abstract|as|async|await|break|case|catch|class|const|continue|def|default|" +
            "delete|do|else|enum|export|extends|final|finally|fn|for|from|func|function|" +
            "get|if|implements|import|in|instanceof|interface|is|it|lambda|let|match|new|" +
            "null|None|package|private|protected|public|raise|return|self|set|static|struct|" +
            "super|switch|this|throw|try|type|typeof|val|var|void|while|with|yield|" +
            "true|false|True|False|None|undefined|println|print)\\b")

    private val TYPES = Pattern.compile(
        "\\b(String|Int|Long|Float|Double|Boolean|List|Map|Array|Object|Void|void|int|" +
            "float|double|bool|char|byte|short|number|string|boolean|any)\\b")

    private val STRINGS = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'|`([^`\\\\]|\\\\.)*`")
    private val NUMBERS = Pattern.compile("\\b\\d+(\\.\\d+)?\\b")
    private val LINE_COMMENT = Pattern.compile("(//|#).*$", Pattern.MULTILINE)
    private val BLOCK_COMMENT = Pattern.compile("/\\*[\\s\\S]*?\\*/")

    fun languageOf(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
        "kt", "kts" -> "kotlin"
        "java" -> "java"
        "js", "mjs", "cjs", "jsx" -> "javascript"
        "ts", "tsx" -> "typescript"
        "py" -> "python"
        "html", "htm" -> "html"
        "css" -> "css"
        "json" -> "json"
        "md", "markdown" -> "markdown"
        "sh", "bash" -> "shell"
        "xml" -> "xml"
        else -> "txt"
    }

    /** Palabras clave / builtins por lenguaje (para el autocompletado). */
    fun keywordsFor(lang: String): List<String> = when (lang) {
        "javascript", "typescript" -> listOf(
            "const", "let", "var", "function", "return", "if", "else", "for", "while", "do",
            "switch", "case", "break", "continue", "new", "class", "extends", "super", "this",
            "import", "export", "from", "default", "async", "await", "try", "catch", "finally",
            "throw", "typeof", "instanceof", "of", "in", "delete", "null", "undefined", "true", "false",
            "console", "document", "window", "JSON", "Math", "Promise", "Array", "Object", "String",
            "Number", "log", "length", "push", "map", "filter", "querySelector", "addEventListener"
        )
        "python" -> listOf(
            "def", "class", "import", "from", "as", "return", "if", "elif", "else", "for", "while",
            "break", "continue", "try", "except", "finally", "raise", "with", "lambda", "yield",
            "global", "nonlocal", "pass", "and", "or", "not", "in", "is", "None", "True", "False",
            "self", "print", "len", "range", "str", "int", "float", "list", "dict", "set", "tuple",
            "open", "enumerate", "zip", "map", "filter", "sum", "min", "max", "abs", "sorted"
        )
        "kotlin" -> listOf(
            "fun", "val", "var", "class", "object", "interface", "if", "else", "when", "for", "while",
            "return", "import", "package", "private", "public", "protected", "internal", "override",
            "suspend", "null", "true", "false", "it", "this", "super", "init", "companion", "data",
            "sealed", "enum", "try", "catch", "finally", "throw", "is", "as", "in", "out",
            "println", "listOf", "mapOf", "setOf", "mutableListOf"
        )
        "java" -> listOf(
            "public", "private", "protected", "class", "interface", "extends", "implements", "new",
            "return", "if", "else", "for", "while", "switch", "case", "break", "continue", "try",
            "catch", "finally", "throw", "void", "int", "long", "double", "float", "boolean", "char",
            "String", "null", "true", "false", "static", "final", "import", "package", "this", "super", "System"
        )
        "html" -> listOf(
            "html", "head", "body", "div", "span", "script", "link", "meta", "title", "a", "p",
            "h1", "h2", "h3", "ul", "li", "img", "button", "input", "form", "style", "class", "id",
            "href", "src"
        )
        "css" -> listOf(
            "color", "background", "margin", "padding", "display", "flex", "grid", "font-size", "border",
            "width", "height", "position", "top", "left", "right", "bottom", "z-index", "opacity",
            "transition", "transform", "align-items", "justify-content"
        )
        "shell" -> listOf(
            "echo", "cd", "ls", "cat", "grep", "sed", "awk", "curl", "wget", "chmod", "mkdir",
            "rm", "cp", "mv", "git", "npm", "node", "python", "for", "do", "done", "if", "then",
            "fi", "while", "export"
        )
        else -> emptyList()
    }

    fun highlight(et: EditText, language: String) {
        val text: Editable = et.text ?: return
        if (text.length == 0 || text.length > 40_000) return
        clear(text)
        val src = text.toString()
        apply(text, BLOCK_COMMENT, C_COMMENT, src)
        apply(text, LINE_COMMENT, C_COMMENT, src)
        apply(text, STRINGS, C_STRING, src)
        apply(text, KEYWORDS, C_KEYWORD, src)
        apply(text, TYPES, C_TYPE, src)
        apply(text, NUMBERS, C_NUMBER, src)
    }

    private fun clear(text: Spannable) {
        for (span in text.getSpans(0, text.length, ForegroundColorSpan::class.java)) {
            text.removeSpan(span)
        }
    }

    private fun apply(text: Editable, p: Pattern, color: Int, src: String) {
        val m = p.matcher(src)
        while (m.find()) {
            // No resaltar dentro de comentarios de línea ya pintados: se deja simple,
            // el orden de aplicación hace que lo último gane.
            text.setSpan(ForegroundColorSpan(color), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}
