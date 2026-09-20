package com.keyide.app.files

import com.keyide.app.ui.Ui
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.util.Locale

/**
 * Entrada unificada del explorador: puede venir del proyecto interno (File)
 * o de una carpeta elegida por el usuario vía SAF (DocumentFile).
 */
data class Entry(
    val name: String,
    val isDir: Boolean,
    val file: File? = null,
    val doc: DocumentFile? = null
) {
    val uri: android.net.Uri? get() = doc?.uri
}

/** Mime type aproximado por extensión (para crear documentos vía SAF). */
fun mimeFor(name: String): String = when (name.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
    "html", "htm" -> "text/html"
    "css" -> "text/css"
    "js", "mjs", "cjs" -> "application/javascript"
    "json" -> "application/json"
    "md" -> "text/markdown"
    "txt", "log" -> "text/plain"
    "py" -> "text/x-python"
    "kt", "java", "ts", "sh", "xml", "yml", "yaml" -> "text/plain"
    else -> "text/plain"
}

class EntryAdapter(private val onClick: (Entry) -> Unit) : RecyclerView.Adapter<EntryAdapter.VH>() {

    private var items: List<Entry> = emptyList()

    fun submit(list: List<Entry>) {
        items = list
        notifyDataSetChanged()
    }

    class VH(val row: LinearLayout, val icon: TextView, val name: TextView) : RecyclerView.ViewHolder(row)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val d = ctx.resources.displayMetrics.density
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val icon = TextView(ctx).apply { textSize = 16f }
        val name = TextView(ctx).apply {
            setTextColor(Ui.fg)
            textSize = 14f
            setPadding((10 * d).toInt(), (12 * d).toInt(), 0, (12 * d).toInt())
        }
        row.addView(icon, LinearLayout.LayoutParams((26 * d).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT))
        row.addView(name, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        return VH(row, icon, name)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = items[position]
        val d = holder.row.context.resources.displayMetrics.density
        holder.row.setPadding((12 * d).toInt(), 0, (12 * d).toInt(), 0)
        holder.icon.text = if (e.isDir) "\uD83D\uDCC1" else iconFor(e.name)
        holder.name.text = e.name
        holder.row.setOnClickListener { onClick(e) }
    }

    override fun getItemCount(): Int = items.size

    private fun iconFor(name: String): String = when (name.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
        "js", "mjs", "cjs" -> "\uD83D\uDFE8"
        "ts", "tsx" -> "\uD83D\uDD37"
        "kt", "kts" -> "\uD83D\uDFE7"
        "java" -> "\u2615"
        "py" -> "\uD83D\uDC0D"
        "html", "htm" -> "\uD83C\uDF10"
        "css" -> "\uD83C\uDFA8"
        "json" -> "\uD83E\uDDFE"
        "md" -> "\uD83D\uDCD8"
        "sh" -> "\uD83D\uDDF3"
        else -> "\uD83D\uDCC4"
    }
}
