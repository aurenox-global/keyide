package com.keyide.app.editor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

/**
 * Margen izquierdo del editor: números de línea + marcas de diagnóstico.
 * Se sincroniza con el scroll vertical del EditText para que los números
 * sigan al texto.
 */
class GutterView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var lineCount: Int = 1
        set(v) { if (v != field) { field = v; requestLayout(); invalidate() } }

    private var lineHeight: Float = 0f
    private var padTop: Float = 0f
    private var scrollYOffset: Int = 0

    var diagnostics: List<Diagnostic> = emptyList()
        set(v) { field = v; invalidate() }

    /** Líneas con punto de parada (1-based). */
    var breakpoints: Set<Int> = emptySet()
        set(v) { field = v; invalidate() }

    /** Toque sobre el margen → línea pulsada (1-based). */
    var onTapLine: ((Int) -> Unit)? = null

    private val density = resources.displayMetrics.density

    private val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6E7681")
        textAlign = Paint.Align.RIGHT
        textSize = 11f * density
        typeface = android.graphics.Typeface.MONOSPACE
    }

    private val markPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var bgColor: Int = Color.parseColor("#0D1117")

    /** Aplica la paleta del editor al margen. */
    fun applyTheme(t: EditorTheme) {
        bgColor = t.bg
        numPaint.color = t.gutter
        invalidate()
    }

    /** Ajusta el tamaño de los números (en px). */
    fun setNumberTextSize(px: Float) {
        numPaint.textSize = px
        invalidate()
    }

    fun syncLayout(lh: Int, pt: Int) {
        if (lh > 0) lineHeight = lh.toFloat()
        padTop = pt.toFloat()
        invalidate()
    }

    fun setEditorScroll(y: Int) {
        if (y != scrollYOffset) {
            scrollYOffset = y
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = (46 * density).toInt()
        setMeasuredDimension(w, MeasureSpec.getSize(heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(bgColor)
        if (lineHeight <= 0f) return

        val first = max(0, ((scrollYOffset - padTop) / lineHeight).toInt() - 1)
        val last = first + (height / lineHeight).toInt() + 3
        val x = width - 10f * density

        for (i in first until last) {
            if (i >= lineCount) break
            val baseline = padTop - scrollYOffset + i * lineHeight + lineHeight * 0.72f
            canvas.drawText((i + 1).toString(), x, baseline, numPaint)
        }

        // Marcas de diagnóstico (borde derecho)
        for (d in diagnostics) {
            val idx = d.line - 1
            if (idx < 0 || idx >= lineCount) continue
            val cy = padTop - scrollYOffset + idx * lineHeight + lineHeight / 2f
            markPaint.color = when (d.severity) {
                Severity.ERROR -> Color.parseColor("#F85149")
                Severity.WARNING -> Color.parseColor("#D29922")
                Severity.INFO -> Color.parseColor("#58A6FF")
            }
            canvas.drawCircle(width - 7f * density, cy, 2.6f * density, markPaint)
        }

        // Puntos de parada (borde izquierdo)
        if (breakpoints.isNotEmpty()) {
            markPaint.color = Color.parseColor("#F85149")
            for (b in breakpoints) {
                val idx = b - 1
                if (idx < 0 || idx >= lineCount) continue
                val cy = padTop - scrollYOffset + idx * lineHeight + lineHeight / 2f
                canvas.drawCircle(7f * density, cy, 4.4f * density, markPaint)
            }
        }
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action == android.view.MotionEvent.ACTION_UP && lineHeight > 0f) {
            val line = ((scrollYOffset + event.y - padTop) / lineHeight).toInt() + 1
            if (line in 1..lineCount) onTapLine?.invoke(line)
        }
        return true
    }
}
