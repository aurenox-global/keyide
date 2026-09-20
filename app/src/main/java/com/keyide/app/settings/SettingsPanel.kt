package com.keyide.app.settings

import com.keyide.app.ui.Ui
import android.content.Context
import android.graphics.Color
import android.text.InputType
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.keyide.app.ai.AiClient
import com.keyide.app.data.AiProviders
import com.keyide.app.data.Settings
import org.json.JSONArray
import org.json.JSONObject

/**
 * Hoja "Ajustes del copiloto": elegir proveedor (DeepSeek, OpenAI, Claude,
 * Qwen, Kimi, GLM, local llama.cpp o custom), endpoint, modelo y API key.
 */
class SettingsPanel(
    context: Context,
    private val settings: Settings,
    private val onSaved: () -> Unit
) : LinearLayout(context) {

    private val providerSpinner = Spinner(context)
    private val baseField = EditText(context)
    private val modelField = EditText(context)
    private val keyField = EditText(context)
    private val hint = TextView(context)
    private val result = TextView(context)

    private var firstSelection = true

    init {
        orientation = VERTICAL
        setBackgroundColor(Ui.panel)
        val d = resources.displayMetrics.density

        fun label(t: String) = TextView(context).apply {
            text = t
            setTextColor(Ui.mut)
            textSize = 12f
            setPadding(0, (10 * d).toInt(), 0, (2 * d).toInt())
        }

        val col = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding((16 * d).toInt(), (8 * d).toInt(), (16 * d).toInt(), (16 * d).toInt())
        }

        // Spinner de proveedores
        val labels = AiProviders.list.map { it.label }
        providerSpinner.adapter = ArrayAdapter(
            context, android.R.layout.simple_spinner_dropdown_item, labels
        )
        providerSpinner.setSelection(
            AiProviders.list.indexOfFirst { it.id == settings.aiProvider }.coerceAtLeast(0)
        )

        baseField.apply { setText(settings.aiBaseUrl); setSingleLine(true); inputType = InputType.TYPE_TEXT_VARIATION_URI }
        modelField.apply { setText(settings.aiModel); setSingleLine(true) }
        keyField.apply {
            setText(settings.aiKey)
            setSingleLine(true)
            hint = "API key (vacío para modelo local)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        for (f in listOf(baseField, modelField, keyField)) {
            f.setTextColor(Ui.fg)
            f.setHintTextColor(Color.parseColor("#6E7681"))
            f.setBackgroundColor(Ui.bg)
            f.setPadding((10 * d).toInt(), (10 * d).toInt(), (10 * d).toInt(), (10 * d).toInt())
        }

        hint.apply {
            setTextColor(Color.parseColor("#D29922"))
            textSize = 11.5f
            setPadding(0, (6 * d).toInt(), 0, 0)
        }
        result.apply {
            setTextColor(Ui.mut)
            textSize = 12f
            setPadding(0, (10 * d).toInt(), 0, 0)
        }

        col.addView(label("Proveedor"))
        col.addView(providerSpinner, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        col.addView(hint, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        col.addView(label("Endpoint base"))
        col.addView(baseField, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        col.addView(label("Modelo"))
        col.addView(modelField, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        col.addView(label("API key"))
        col.addView(keyField, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val buttons = LinearLayout(context).apply { orientation = HORIZONTAL }
        buttons.addView(Button(context).apply {
            text = "Guardar"
            setOnClickListener { save(); Toast.makeText(context, "Guardado", Toast.LENGTH_SHORT).show() }
        }, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        buttons.addView(Button(context).apply {
            text = "Probar"
            setOnClickListener { test() }
        }, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        col.addView(buttons, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        col.addView(result, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val scroll = ScrollView(context)
        scroll.addView(col, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(scroll, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        providerSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val p = AiProviders.list.getOrNull(position) ?: return
                hint.text = if (p.hint.isNotBlank()) p.hint else ""
                if (firstSelection) { firstSelection = false; return }
                baseField.setText(p.base)
                modelField.setText(p.model)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        hint.text = AiProviders.byId(settings.aiProvider).hint
    }

    private fun save() {
        val pos = providerSpinner.selectedItemPosition
        val p = AiProviders.list.getOrNull(pos) ?: AiProviders.list.first()
        settings.aiProvider = p.id
        settings.aiBaseUrl = baseField.text?.toString()?.trim().orEmpty().ifBlank { p.base }
        settings.aiModel = modelField.text?.toString()?.trim().orEmpty().ifBlank { p.model }
        settings.aiKey = keyField.text?.toString()?.trim().orEmpty()
        onSaved()
    }

    private fun test() {
        save()
        result.text = "Probando ${settings.aiProviderLabel} · ${settings.aiModel} …"
        val history = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", "Responde en una sola palabra."))
            put(JSONObject().put("role", "user").put("content", "Di: OK"))
        }
        AiClient(settings).send(history,
            onResult = { result.text = "\u2714 ${settings.aiProviderLabel} responde: ${it.take(120)}" },
            onError = { result.text = "\u2716 ${it.take(300)}" }
        )
    }
}
