package com.keyide.app.edit

import com.keyide.app.ui.Ui
import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.keyide.app.R
import com.keyide.app.editor.CodeEditorView

/**
 * Hoja "Buscar y reemplazar": busca en el documento actual, resalta todas las
 * coincidencias, navega entre ellas y permite reemplazar una o todas.
 */
class FindPanel(context: Context, private val editor: CodeEditorView) : LinearLayout(context) {

    private val query = EditText(context)
    private val replace = EditText(context)
    private val status = TextView(context)

    private var caseSensitive = false
    private val matches = mutableListOf<IntRange>()
    private var index = -1
    private var busy = false

    private val hlAll = 0x55D29922
    private val hlCurrent = 0xCCF0C674.toInt()

    init {
        orientation = VERTICAL
        setBackgroundColor(Ui.panel)
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        editor.completionEnabled = false

        fun field(hint: String) = EditText(context).apply {
            this.hint = hint
            setTextColor(Ui.fg)
            setHintTextColor(Color.parseColor("#6E7681"))
            setBackgroundColor(Ui.bg)
            textSize = 13f
            setSingleLine(true)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        query.hint = context.getString(R.string.hint_find)
        replace.hint = context.getString(R.string.hint_replace)

        fun btn(label: String, onClick: () -> Unit) = Button(context).apply {
            text = label
            textSize = 11f
            isAllCaps = false
            setOnClickListener { onClick() }
        }

        val caseBtn = btn("Aa") { }
        caseBtn.alpha = 0.5f
        caseBtn.setOnClickListener {
            caseSensitive = !caseSensitive
            caseBtn.alpha = if (caseSensitive) 1f else 0.5f
            recompute()
            highlight()
        }

        val row1 = LinearLayout(context).apply { orientation = HORIZONTAL; setPadding(dp(8), dp(6), dp(8), dp(2)) }
        row1.addView(query, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row1.addView(caseBtn, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val row2 = LinearLayout(context).apply { orientation = HORIZONTAL; setPadding(dp(8), dp(2), dp(8), dp(2)) }
        row2.addView(replace, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val row3 = LinearLayout(context).apply { orientation = HORIZONTAL; setPadding(dp(8), dp(2), dp(8), dp(2)) }
        row3.addView(btn("\u25C0 " + context.getString(R.string.action_prev)) { prev() }, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row3.addView(btn(context.getString(R.string.action_next) + " \u25B6") { next() }, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row3.addView(btn(context.getString(R.string.action_replace)) { replaceCurrent() }, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row3.addView(btn(context.getString(R.string.action_replace_all)) { replaceAll() }, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        status.apply {
            setTextColor(Ui.mut)
            textSize = 12f
            gravity = Gravity.START
            setPadding(dp(12), dp(6), dp(12), dp(8))
        }

        addView(row1, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(row2, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(row3, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(status, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        query.addTextChangedListener(simpleWatcher { index = -1; recompute(); highlight() })
        replace.addTextChangedListener(simpleWatcher { })
        editor.editor.addTextChangedListener(simpleWatcher { if (!busy) { recompute(); highlight() } })
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        editor.completionEnabled = true
        clearHighlight()
    }

    // ── Lógica ────────────────────────────────────────────────────────────

    private fun simpleWatcher(block: () -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        override fun afterTextChanged(s: Editable?) = block()
    }

    private fun recompute() {
        matches.clear()
        val q = query.text?.toString().orEmpty()
        if (q.isEmpty()) { status.text = "Escribe algo para buscar."; return }
        val text = editor.text()
        val hay = if (caseSensitive) text else text.lowercase()
        val needle = if (caseSensitive) q else q.lowercase()
        var i = 0
        while (i <= hay.length - needle.length) {
            val j = hay.indexOf(needle, i)
            if (j < 0) break
            matches.add(j until (j + needle.length))
            i = j + maxOf(1, needle.length)
        }
        if (index >= matches.size) index = -1
        status.text = if (matches.isEmpty()) "Sin coincidencias." else "${matches.size} coincidencias."
    }

    private fun highlight() {
        val ed = editor.editor
        val text = ed.text ?: return
        busy = true
        runCatching {
            for (s in text.getSpans(0, text.length, BackgroundColorSpan::class.java)) text.removeSpan(s)
            matches.forEachIndexed { i, r ->
                if (r.first < 0 || r.last + 1 > text.length) return@forEachIndexed
                val color = if (i == index) hlCurrent else hlAll
                text.setSpan(BackgroundColorSpan(color), r.first, r.last + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        busy = false
    }

    private fun clearHighlight() {
        val ed = editor.editor
        val text = ed.text ?: return
        busy = true
        runCatching {
            for (s in text.getSpans(0, text.length, BackgroundColorSpan::class.java)) text.removeSpan(s)
        }
        busy = false
    }

    private fun select(i: Int) {
        if (i !in matches.indices) return
        index = i
        val r = matches[i]
        editor.editor.setSelection(r.first, r.last + 1)
        editor.editor.requestFocus()
        highlight()
        status.text = "Coincidencia ${i + 1} de ${matches.size}"
    }

    private fun next() {
        if (matches.isEmpty()) { recompute(); highlight() }
        if (matches.isEmpty()) return
        select(if (index + 1 >= matches.size) 0 else index + 1)
    }

    private fun prev() {
        if (matches.isEmpty()) { recompute(); highlight() }
        if (matches.isEmpty()) return
        select(if (index <= 0) matches.size - 1 else index - 1)
    }

    private fun replaceCurrent() {
        val rep = replace.text?.toString().orEmpty()
        if (index !in matches.indices) { next(); return }
        val r = matches[index]
        busy = true
        editor.editor.text?.replace(r.first, r.last + 1, rep)
        busy = false
        recompute()
        if (matches.isNotEmpty()) next() else highlight()
    }

    private fun replaceAll() {
        val rep = replace.text?.toString().orEmpty()
        recompute()
        if (matches.isEmpty()) return
        val sb = StringBuilder(editor.text())
        for (r in matches.asReversed()) sb.replace(r.first, r.last + 1, rep)
        busy = true
        editor.editor.setText(sb.toString())
        busy = false
        recompute()
        highlight()
        status.text = "Reemplazadas ${matches.size} coincidencias."
    }
}
