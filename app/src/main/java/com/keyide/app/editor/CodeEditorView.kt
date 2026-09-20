package com.keyide.app.editor

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListPopupWindow

/**
 * Editor de código:
 *   [ margen (números + diagnóstico) | EditText monoespaciado ]
 * - auto-cierre de paréntesis/comillas
 * - auto-indentación al pulsar Enter
 * - autocompletado LSP-lite (palabras del documento + keywords/builtins)
 * - ajuste de línea conmutable, ir a línea
 */
class CodeEditorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    val gutter = GutterView(context)
    val editor = EditText(context)

    private var suppress = false
    private var prevLen = 0

    var filePath: String? = null
        private set

    var language: String = "txt"
        private set

    var onChanged: ((String) -> Unit)? = null

    var isModified: Boolean = false

    var wordWrap: Boolean = false
        private set

    var completionEnabled: Boolean = true

    private val dp = resources.displayMetrics.density

    private val popup = ListPopupWindow(context)
    private val popupAdapter = ArrayAdapter<String>(context, android.R.layout.simple_list_item_1)
    private var wordCache: List<String> = emptyList()

    private val pairs = mapOf(
        '(' to ')', '[' to ']', '{' to '}',
        '"' to '"', '\'' to '\'', '`' to '`'
    )

    init {
        setBackgroundColor(Syntax.BG)

        editor.apply {
            setBackgroundColor(Color.TRANSPARENT)
            typeface = Typeface.MONOSPACE
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(Syntax.FG)
            setHintTextColor(Color.parseColor("#6E7681"))
            gravity = Gravity.TOP or Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setHorizontallyScrolling(true)
            setSingleLine(false)
            setLineSpacing(0f, 1.3f)
            setPadding((10 * dp).toInt(), (8 * dp).toInt(), (12 * dp).toInt(), (8 * dp).toInt())
            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = true
            setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) dismissCompletions() }
        }

        popup.anchorView = editor
        popup.setAdapter(popupAdapter)
        popup.isModal = false
        popup.setOnItemClickListener { _, _, position, _ ->
            applyCompletion(popupAdapter.getItem(position).orEmpty())
        }

        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(gutter, LinearLayout.LayoutParams((46 * dp).toInt(), LinearLayout.LayoutParams.MATCH_PARENT))
        row.addView(editor, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        editor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (suppress) { prevLen = s?.length ?: 0; return }
                val cur = s?.toString() ?: ""
                if (cur.length == prevLen + 1) {
                    val pos = editor.selectionStart
                    if (pos in 1..cur.length) {
                        val inserted = cur[pos - 1]
                        if (handleAutoClose(inserted, pos)) {
                            finishEdit()
                            return
                        }
                        if (inserted == '\n') autoIndent(pos, cur)
                    }
                }
                finishEdit()
            }
        })

        editor.setOnScrollChangeListener { _, _, scrollY, _, _ -> gutter.setEditorScroll(scrollY) }
        gutter.onTapLine = { line -> toggleBreakpoint(line) }

        post { gutter.syncLayout(editor.lineHeight, editor.totalPaddingTop); refreshGutter() }
        setLanguage("javascript")
    }

    // ── API ──────────────────────────────────────────────────────────────

    fun setWordWrap(wrap: Boolean) {
        wordWrap = wrap
        editor.setHorizontallyScrolling(!wrap)
        editor.isHorizontalScrollBarEnabled = !wrap
    }

    fun setLanguage(lang: String) {
        language = lang
        suppress = true
        Syntax.highlight(editor, lang)
        suppress = false
        refreshGutter()
    }

    fun loadFile(path: String, content: String, lang: String) {
        filePath = path
        dismissCompletions()
        suppress = true
        editor.setText(content)
        editor.setSelection(0)
        Syntax.highlight(editor, lang)
        suppress = false
        prevLen = editor.text?.length ?: 0
        wordCache = emptyList()
        setLanguage(lang)
        isModified = false
        post {
            gutter.syncLayout(editor.lineHeight, editor.totalPaddingTop)
            refreshGutter()
            gutter.setEditorScroll(editor.scrollY)
        }
    }

    fun text(): String = editor.text?.toString() ?: ""

    fun lineCount(): Int = text().count { it == '\n' } + 1

    fun goToLine(n: Int) {
        val t = text()
        if (t.isEmpty()) return
        val lines = t.split('\n')
        val target = n.coerceIn(1, lines.size)
        var offset = 0
        for (i in 0 until target - 1) offset += lines[i].length + 1
        val start = offset.coerceIn(0, t.length)
        val end = (offset + lines[target - 1].length).coerceIn(start, t.length)
        editor.setSelection(start, end)
        editor.requestFocus()
    }

    fun insert(text: String) {
        val start = editor.selectionStart.coerceAtLeast(0)
        val end = editor.selectionEnd.coerceAtLeast(0)
        editor.text.replace(minOf(start, end), maxOf(start, end), text)
    }

    fun setDiagnostics(list: List<Diagnostic>) {
        gutter.diagnostics = list
    }

    /** Puntos de parada del depurador (1-based). */
    val breakpoints: MutableSet<Int> = sortedSetOf()

    fun toggleBreakpoint(line: Int) {
        if (!breakpoints.add(line)) breakpoints.remove(line)
        gutter.breakpoints = breakpoints.toSet()
    }

    fun clearBreakpoints() {
        breakpoints.clear()
        gutter.breakpoints = emptySet()
    }

    fun dismissCompletions() {
        if (popup.isShowing) runCatching { popup.dismiss() }
    }

    /** Cambia el tamaño de fuente del editor (y del margen). */
    fun setFontSize(sp: Float) {
        editor.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        gutter.setNumberTextSize(sp * dp * 0.85f)
        refreshGutter()
    }

    /** Aplica un tema de color al editor, al margen y al resaltado. */
    fun setTheme(t: EditorTheme) {
        Syntax.applyTheme(t)
        setBackgroundColor(t.bg)
        editor.setTextColor(t.fg)
        editor.setHintTextColor(t.comment)
        gutter.applyTheme(t)
        suppress = true
        Syntax.highlight(editor, language)
        suppress = false
        invalidate()
        gutter.invalidate()
    }

    /** Deshacer / rehacer usando el undo nativo del EditText. */
    fun undo() { editor.onTextContextMenuItem(android.R.id.undo) }

    fun redo() { editor.onTextContextMenuItem(android.R.id.redo) }

    /** Reemplaza todo el texto (marcando como modificado) sin tocar el fichero. */
    fun applyText(newText: String) {
        suppress = true
        editor.setText(newText)
        Syntax.highlight(editor, language)
        suppress = false
        prevLen = editor.text?.length ?: 0
        refreshGutter()
        isModified = true
        onChanged?.invoke(newText)
    }

    /**
     * Formateo básico: re-indenta por anidamiento de {} [] ().
     * Solo para lenguajes con llaves (no python/html/markdown/shell).
     */
    fun formatDocument(): Boolean {
        val fmt = setOf("javascript", "typescript", "java", "kotlin", "json", "css")
        if (language !in fmt) return false
        val lines = text().split('\n')
        if (lines.size > 5000) return false
        val sb = StringBuilder()
        var indent = 0
        for ((i, raw) in lines.withIndex()) {
            val trim = raw.trim()
            if (trim.isEmpty()) {
                if (i < lines.size - 1) sb.append('\n')
                continue
            }
            val leadingClose = trim.startsWith("}") || trim.startsWith(")") || trim.startsWith("]")
            val display = (indent - if (leadingClose) 1 else 0).coerceAtLeast(0)
            repeat(display) { sb.append("  ") }
            sb.append(trim)
            if (i < lines.size - 1) sb.append('\n')
            val opens = trim.count { it == '{' || it == '(' || it == '[' }
            val closes = trim.count { it == '}' || it == ')' || it == ']' }
            indent = (indent + opens - closes).coerceAtLeast(0)
        }
        applyText(sb.toString())
        return true
    }

    /** Palabra bajo el cursor (o la selección actual). Para "ir a definición". */
    fun wordAtCursor(): String? {
        val t = text()
        val s = editor.selectionStart
        val e = editor.selectionEnd
        if (s in 0..t.length && e in 0..t.length && s != e) {
            return t.substring(minOf(s, e), maxOf(s, e)).trim().takeIf { it.isNotEmpty() }
        }
        if (s <= 0 || s > t.length) return null
        var a = s
        var b = s
        while (a > 0 && isIdent(t[a - 1])) a--
        while (b < t.length && isIdent(t[b])) b++
        if (a == b) return null
        return t.substring(a, b)
    }

    private fun isIdent(c: Char) = c.isLetterOrDigit() || c == '_' || c == '$'

    // ── Interno ──────────────────────────────────────────────────────────

    private fun finishEdit() {
        prevLen = editor.text?.length ?: 0
        suppress = true
        Syntax.highlight(editor, language)
        suppress = false
        refreshGutter()
        isModified = true
        onChanged?.invoke(editor.text.toString())
        maybeComplete()
    }

    private fun handleAutoClose(inserted: Char, pos: Int): Boolean {
        val text = editor.text ?: return false
        if (pairs.containsKey(inserted)) {
            suppress = true
            text.insert(pos, pairs[inserted].toString())
            editor.setSelection(pos)
            suppress = false
            return true
        }
        if (pairs.containsValue(inserted) && pos < text.length && text[pos] == inserted) {
            // "skip over": ya estaba el cierre, no duplicar
            suppress = true
            text.delete(pos - 1, pos)
            editor.setSelection(pos.coerceAtMost(text.length))
            suppress = false
            return true
        }
        return false
    }

    private fun autoIndent(pos: Int, cur: String) {
        val lineStart = cur.lastIndexOf('\n', pos - 2) + 1
        val from = if (lineStart >= 0) lineStart else 0
        val currentLine = cur.substring(from, pos - 1)
        val indent = currentLine.takeWhile { it == ' ' || it == '\t' }
        val extra = if (currentLine.trimEnd().endsWith("{")) "  " else ""
        if (indent.isNotEmpty() || extra.isNotEmpty()) {
            suppress = true
            editor.text?.insert(pos, indent + extra)
            suppress = false
        }
    }

    // ── Autocompletado ───────────────────────────────────────────────────

    private fun maybeComplete() {
        if (!completionEnabled) return
        if (!editor.isAttachedToWindow || !editor.hasFocus()) return
        val t = text()
        val pos = editor.selectionStart
        if (pos <= 0 || pos > t.length) { dismissCompletions(); return }
        val ch = t[pos - 1]
        if (!(ch.isLetterOrDigit() || ch == '_' || ch == '.')) { dismissCompletions(); return }
        val start = wordStart(t, pos)
        val prefix = t.substring(start, pos)
        if (prefix.length < 2) { dismissCompletions(); return }

        val cands = candidates(prefix)
        if (cands.isEmpty()) { dismissCompletions(); return }

        popupAdapter.clear()
        popupAdapter.addAll(cands)
        popupAdapter.notifyDataSetChanged()
        runCatching {
            if (popup.isShowing) popup.dismiss()
            popup.show()
        }
    }

    private fun wordStart(t: String, pos: Int): Int {
        var i = pos
        while (i > 0) {
            val c = t[i - 1]
            if (c.isLetterOrDigit() || c == '_' || c == '.') i-- else break
        }
        return i
    }

    private fun candidates(prefix: String): List<String> {
        val out = LinkedHashSet<String>()
        val p = prefix.lowercase()
        Syntax.keywordsFor(language)
            .filter { it.startsWith(prefix, true) && it != prefix }
            .forEach { out.add(it) }
        if (wordCache.isEmpty()) {
            wordCache = Regex("[A-Za-z_][A-Za-z0-9_]{2,}")
                .findAll(text())
                .map { it.value }
                .distinct()
                .toList()
        }
        wordCache
            .filter { it.lowercase().startsWith(p) && it != prefix }
            .forEach { out.add(it) }
        return out.take(24)
    }

    private fun applyCompletion(value: String) {
        if (value.isEmpty()) { dismissCompletions(); return }
        val t = text()
        val pos = editor.selectionStart
        val start = wordStart(t, pos)
        suppress = true
        editor.text?.replace(start, pos, value)
        suppress = false
        editor.setSelection((start + value.length).coerceAtMost(editor.text?.length ?: 0))
        runCatching { popup.dismiss() }
        finishEdit()
    }

    private fun refreshGutter() {
        gutter.lineCount = lineCount()
        gutter.syncLayout(editor.lineHeight, editor.totalPaddingTop)
    }
}
