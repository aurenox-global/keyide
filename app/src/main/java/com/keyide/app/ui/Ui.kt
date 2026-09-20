package com.keyide.app.ui

/**
 * Paleta GLOBAL de la app (afecta a todos los paneles).
 * `applyTheme(light)` la cambia y la actividad se recrea para repintar todo.
 * El tema del editor (Syntax/EditorThemes) es independiente.
 */
object Ui {

    var light: Boolean = false
        private set

    var bg: Int = 0xFF0D1117.toInt()
    var panel: Int = 0xFF161B22.toInt()
    var panel2: Int = 0xFF1C2128.toInt()
    var line: Int = 0xFF30363D.toInt()
    var fg: Int = 0xFFE6EDF3.toInt()
    var mut: Int = 0xFF8B949E.toInt()
    var acc: Int = 0xFF58A6FF.toInt()

    fun applyTheme(isLight: Boolean) {
        light = isLight
        if (isLight) {
            bg = 0xFFFFFFFF.toInt()
            panel = 0xFFF6F8FA.toInt()
            panel2 = 0xFFEAEEF2.toInt()
            line = 0xFFD0D7DE.toInt()
            fg = 0xFF1F2328.toInt()
            mut = 0xFF57606A.toInt()
            acc = 0xFF0969DA.toInt()
        } else {
            bg = 0xFF0D1117.toInt()
            panel = 0xFF161B22.toInt()
            panel2 = 0xFF1C2128.toInt()
            line = 0xFF30363D.toInt()
            fg = 0xFFE6EDF3.toInt()
            mut = 0xFF8B949E.toInt()
            acc = 0xFF58A6FF.toInt()
        }
    }
}
