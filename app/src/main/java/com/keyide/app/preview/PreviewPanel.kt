package com.keyide.app.preview

import com.keyide.app.ui.Ui
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File

/**
 * Hoja "Vista previa": WebView para HTML/CSS/JS, Markdown renderizado y SVG.
 * Usa baseURL del directorio del fichero para que funcionen rutas relativas.
 */
class PreviewPanel(context: Context) : LinearLayout(context) {

    private val web = WebView(context)
    private val label = TextView(context)

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.WHITE)
        val d = resources.displayMetrics.density

        val bar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Ui.panel)
            setPadding((12 * d).toInt(), (4 * d).toInt(), (6 * d).toInt(), (4 * d).toInt())
        }
        label.apply {
            setTextColor(Ui.mut)
            textSize = 12f
        }
        bar.addView(label, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        bar.addView(Button(context).apply {
            text = "\u21BB"
            textSize = 14f
            setOnClickListener { web.reload() }
        }, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        addView(bar, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(web, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        @SuppressLint("SetJavaScriptEnabled")
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = true
        web.settings.allowContentAccess = true
        web.settings.loadWithOverviewMode = true
        web.settings.useWideViewPort = true
        web.settings.builtInZoomControls = true
        web.settings.displayZoomControls = false
        web.webViewClient = WebViewClient()
        web.setBackgroundColor(Color.WHITE)
    }

    fun loadFile(f: File) {
        label.text = f.name
        val base = "file://" + (f.parentFile?.absolutePath ?: "") + "/"
        web.loadDataWithBaseURL(base, f.readText(), "text/html", "utf-8", null)
    }

    fun loadHtml(html: String, baseDir: File?, title: String) {
        label.text = title
        val base = "file://" + (baseDir?.absolutePath ?: "") + "/"
        web.loadDataWithBaseURL(base, html, "text/html", "utf-8", null)
    }

    fun reload() = web.reload()
}
