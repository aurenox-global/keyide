package com.keyide.app.terminal

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.keyide.app.files.WorkspaceRepo

/**
 * Hoja "Terminal": sesión de shell **persistente**. El `cd` y el entorno se
 * mantienen entre comandos; la salida se va volcando en tiempo real.
 */
class TerminalPanel(context: Context) : LinearLayout(context) {

    private val log = TextView(context)
    private val input = EditText(context)
    private val scroll = ScrollView(context)
    private val session = ShellSession(WorkspaceRepo.root(context))

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#0D1117"))
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(4), dp(6), dp(4))
        }
        val title = TextView(context).apply {
            text = "Sesión persistente · /system/bin/sh"
            setTextColor(Color.parseColor("#8B949E"))
            textSize = 11f
        }
        val clear = Button(context).apply {
            text = "Limpiar"
            textSize = 11f
            isAllCaps = false
            setOnClickListener { log.text = "" }
        }
        header.addView(title, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(clear, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        log.apply {
            typeface = Typeface.MONOSPACE
            setTextColor(Color.parseColor("#C9D1D9"))
            textSize = 12.5f
            setPadding(dp(12), dp(6), dp(12), dp(8))
        }
        scroll.addView(log, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        input.apply {
            hint = "\$ comando…"
            typeface = Typeface.MONOSPACE
            setTextColor(Color.parseColor("#E6EDF3"))
            setHintTextColor(Color.parseColor("#6E7681"))
            setBackgroundColor(Color.parseColor("#161B22"))
            textSize = 13f
            setPadding(dp(12), dp(10), dp(12), dp(10))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setOnEditorActionListener { _, _, _ -> submit(); true }
        }

        addView(header, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(scroll, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        addView(input, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        session.onLine = { line -> post { append(line) } }
        session.onClosed = { post { append("[sesión terminada]") } }
        session.start()
    }

    private fun submit() {
        val cmd = input.text?.toString().orEmpty()
        if (cmd.isBlank()) return
        input.setText("")
        runCommand(cmd)
    }

    fun runCommand(cmd: String) {
        append("$ $cmd")
        if (!session.isAlive()) session.start()
        if (!session.send(cmd)) append("! la sesión no está disponible")
    }

    /** Salida externa (p. ej. del runner de JS). Segura desde cualquier hilo. */
    fun appendOutput(line: String) {
        post { append(line) }
    }

    private fun append(line: String) {
        log.append(if (log.text.isEmpty()) line else "\n$line")
        scroll.post { scroll.fullScroll(Gravity.BOTTOM) }
    }
}
