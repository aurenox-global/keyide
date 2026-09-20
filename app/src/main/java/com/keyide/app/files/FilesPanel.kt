package com.keyide.app.files

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Hoja "Ficheros": explorador tipo gestor de archivos que funciona tanto con
 * el proyecto interno como con una carpeta real elegida por el usuario (SAF).
 */
class FilesPanel(context: Context) : LinearLayout(context) {

    var onOpen: ((Entry) -> Unit)? = null
    var onUp: (() -> Unit)? = null
    var onPickFolder: (() -> Unit)? = null
    var onNewFile: (() -> Unit)? = null
    var onNewFolder: (() -> Unit)? = null

    private val pathLabel = TextView(context)
    private val upButton = TextView(context)
    private val adapter = EntryAdapter { onOpen?.invoke(it) }
    private val rv = RecyclerView(context)

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#161B22"))
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        // Barra de ruta + acciones
        val top = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(4))
        }
        upButton.apply {
            text = "\u2B06"
            setTextColor(Color.parseColor("#E6EDF3"))
            textSize = 16f
            gravity = Gravity.CENTER
            setBackgroundResource(com.keyide.app.R.drawable.bg_key)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener { onUp?.invoke() }
        }
        pathLabel.apply {
            setTextColor(Color.parseColor("#8B949E"))
            textSize = 12f
            maxLines = 1
            setPadding(dp(10), 0, dp(6), 0)
        }
        top.addView(upButton, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        top.addView(pathLabel, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

        val actions = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(dp(8), 0, dp(8), dp(6))
        }
        actions.addView(actionButton(context, "\uD83D\uDCC2 Abrir carpeta") { onPickFolder?.invoke() })
        actions.addView(actionButton(context, "\uFF0B Fichero") { onNewFile?.invoke() })
        actions.addView(actionButton(context, "\uD83D\uDCC1 Carpeta") { onNewFolder?.invoke() })

        rv.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@FilesPanel.adapter
            setBackgroundColor(Color.TRANSPARENT)
        }

        addView(top, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(actions, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(rv, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun actionButton(ctx: Context, label: String, onClick: () -> Unit): TextView {
        val d = ctx.resources.displayMetrics.density
        return TextView(ctx).apply {
            text = label
            setTextColor(Color.parseColor("#E6EDF3"))
            textSize = 11.5f
            gravity = Gravity.CENTER
            setBackgroundResource(com.keyide.app.R.drawable.bg_key)
            setPadding((10 * d).toInt(), (8 * d).toInt(), (10 * d).toInt(), (8 * d).toInt())
            setOnClickListener { onClick() }
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            lp.marginStart = (6 * d).toInt()
            layoutParams = lp
        }
    }

    fun render(path: String, entries: List<Entry>, canGoUp: Boolean) {
        pathLabel.text = path
        upButton.alpha = if (canGoUp) 1f else 0.35f
        adapter.submit(entries)
    }

    fun refreshList() = adapter.notifyDataSetChanged()
}
