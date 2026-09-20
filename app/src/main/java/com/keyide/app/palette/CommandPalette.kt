package com.keyide.app.palette

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import androidx.appcompat.app.AlertDialog

/** Paleta de comandos (Ctrl+K): toda la potencia de un IDE sin ocupar píxeles. */
object CommandPalette {

    data class Cmd(val title: String, val run: () -> Unit)

    fun show(activity: Activity, commands: List<Cmd>) {
        val ctx: Context = activity
        val d = ctx.resources.displayMetrics.density

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * d).toInt(), (8 * d).toInt(), (16 * d).toInt(), 0)
        }
        val input = EditText(ctx).apply {
            hint = "Escribe un comando…"
            setTextColor(Color.parseColor("#E6EDF3"))
            setHintTextColor(Color.parseColor("#6E7681"))
            setSingleLine(true)
        }
        val list = ListView(ctx)

        var current = commands
        val adapter = ArrayAdapter(ctx, android.R.layout.simple_list_item_1, current.map { it.title })
        list.adapter = adapter

        val dialog = AlertDialog.Builder(ctx)
            .setTitle("Paleta de comandos")
            .setView(container)
            .create()

        input.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val q = s?.toString().orEmpty()
                current = commands.filter { it.title.contains(q, ignoreCase = true) }
                adapter.clear()
                adapter.addAll(current.map { it.title })
                adapter.notifyDataSetChanged()
            }
        })

        list.setOnItemClickListener { _, _, position, _ ->
            current.getOrNull(position)?.run?.invoke()
            dialog.dismiss()
        }

        container.addView(input, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        container.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (280 * d).toInt()))

        dialog.show()
        input.requestFocus()
    }
}
