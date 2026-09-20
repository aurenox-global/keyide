package com.keyide.app.ai

import com.keyide.app.ui.Ui
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
import com.keyide.app.data.Settings
import org.json.JSONArray
import com.keyide.app.R
import org.json.JSONObject

/** Hoja "AI": chat con el copiloto, con el código actual como contexto. */
class AiPanel(context: Context, private val settings: Settings) : LinearLayout(context) {

    private val log = TextView(context)
    private val scroll = ScrollView(context)
    private val input = EditText(context)
    private val history = JSONArray()
    private val client = AiClient(settings)
    private var fileContext: String = ""

    init {
        orientation = VERTICAL
        setBackgroundColor(Ui.bg)
        val d = resources.displayMetrics.density

        history.put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))

        log.apply {
            typeface = Typeface.MONOSPACE
            setTextColor(Color.parseColor("#C9D1D9"))
            textSize = 12.5f
            setPadding((12 * d).toInt(), (8 * d).toInt(), (12 * d).toInt(), (8 * d).toInt())
        }
        scroll.addView(log, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val row = LinearLayout(context).apply { orientation = HORIZONTAL }
        input.apply {
            hint = context.getString(R.string.hint_prompt)
            setTextColor(Ui.fg)
            setHintTextColor(Color.parseColor("#6E7681"))
            setBackgroundColor(Ui.panel)
            textSize = 13f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setPadding((12 * d).toInt(), (10 * d).toInt(), (12 * d).toInt(), (10 * d).toInt())
        }
        row.addView(input, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(Button(context).apply {
            text = context.getString(R.string.action_send)
            setOnClickListener { send() }
        }, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        addView(scroll, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        addView(row, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        append("✨ Copiloto · ${settings.aiProviderLabel} · ${settings.aiModel}")
        if (settings.aiKey.isBlank() && settings.aiProvider != "local") {
            append("⚠ Sin API key. Toca ⚙ (arriba) para configurar el proveedor o apuntar a un modelo local llama.cpp.")
        }
    }

    /** El editor llama a esto al abrir el panel para dar contexto al modelo. */
    fun setContext(path: String, content: String) {
        fileContext = "Fichero actual: $path\n\n```\n${content.take(6000)}\n```"
    }

    private fun send() {
        val prompt = input.text?.toString()?.trim().orEmpty()
        if (prompt.isEmpty()) return
        input.setText("")
        append("\n▸ $prompt")
        val userContent = if (fileContext.isBlank()) prompt else "$prompt\n\n$fileContext"
        history.put(JSONObject().put("role", "user").put("content", userContent))
        append("… pensando")
        client.send(history,
            onResult = { reply ->
                history.put(JSONObject().put("role", "assistant").put("content", reply))
                append("\n◂ $reply")
            },
            onError = { err -> append("\n✖ $err") }
        )
    }

    private fun append(text: String) {
        log.append(if (log.text.isEmpty()) text else "$text")
        scroll.post { scroll.fullScroll(Gravity.BOTTOM) }
    }

    companion object {
        private const val SYSTEM_PROMPT =
            "Eres KeyIDE Copilot, asistente de programación dentro de un IDE para Android. " +
                "Responde en español, breve y directo. Cuando des código, usa bloques con el lenguaje " +
                "y prioriza soluciones que funcionen en un móvil (sin herramientas de escritorio)."
    }
}
