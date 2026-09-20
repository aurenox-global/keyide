package com.keyide.app.edit

import android.content.Context
import android.graphics.Color
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.keyide.app.files.Entry

/** Coincidencia encontrada en el proyecto. */
data class Hit(val name: String, val line: Int, val preview: String, val entry: Entry)

/**
 * Hoja "Buscar en el proyecto": recorre los ficheros de la ubicación actual
 * (interna o SAF) y lista las líneas que contienen el texto.
 */
class ProjectSearchPanel(
    context: Context,
    private val search: (String) -> List<Hit>,
    private val onOpen: (Hit) -> Unit
) : LinearLayout(context) {

    private val query = EditText(context)
    private val results = LinearLayout(context)
    private val info = TextView(context)

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#0D1117"))
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        val top = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(4))
        }
        query.apply {
            hint = "Buscar en el proyecto…"
            setTextColor(Color.parseColor("#E6EDF3"))
            setHintTextColor(Color.parseColor("#6E7681"))
            setBackgroundColor(Color.parseColor("#161B22"))
            textSize = 13f
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        val go = Button(context).apply {
            text = "Buscar"
            setOnClickListener { runSearch() }
        }
        top.addView(query, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(go, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        info.apply {
            setTextColor(Color.parseColor("#8B949E"))
            textSize = 11.5f
            setPadding(dp(12), 0, dp(12), dp(4))
        }

        results.orientation = VERTICAL

        val scroll = ScrollView(context)
        scroll.addView(results, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        addView(top, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(info, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(scroll, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun runSearch() {
        val q = query.text?.toString().orEmpty()
        results.removeAllViews()
        if (q.trim().isEmpty()) { info.text = "Escribe algo."; return }
        info.text = "Buscando…"
        val hits = search(q.trim())
        info.text = if (hits.isEmpty()) "Sin coincidencias." else "${hits.size} coincidencias"
        val d = resources.displayMetrics.density
        for (h in hits) {
            val row = LinearLayout(context).apply {
                orientation = VERTICAL
                setPadding((12 * d).toInt(), (8 * d).toInt(), (12 * d).toInt(), (8 * d).toInt())
                setOnClickListener { onOpen(h) }
            }
            val title = TextView(context).apply {
                text = "${h.name}:${h.line}"
                setTextColor(Color.parseColor("#58A6FF"))
                textSize = 12.5f
            }
            val preview = TextView(context).apply {
                text = h.preview
                setTextColor(Color.parseColor("#C9D1D9"))
                textSize = 12f
                maxLines = 1
            }
            row.addView(title)
            row.addView(preview)
            results.addView(row, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }
}
