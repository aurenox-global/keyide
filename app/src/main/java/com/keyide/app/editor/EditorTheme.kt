package com.keyide.app.editor

/** Paleta de colores del editor (tema). */
data class EditorTheme(
    val id: String,
    val label: String,
    val bg: Int,
    val fg: Int,
    val gutter: Int,
    val keyword: Int,
    val string: Int,
    val comment: Int,
    val number: Int,
    val type: Int
)

object EditorThemes {

    val ONE_DARK = EditorTheme(
        "onedark", "One Dark (oscuro)",
        0xFF0D1117.toInt(), 0xFFC9D1D9.toInt(), 0xFF6E7681.toInt(),
        0xFFFF7B72.toInt(), 0xFFA5D6FF.toInt(), 0xFF6E7681.toInt(),
        0xFF79C0FF.toInt(), 0xFFFFA657.toInt()
    )

    val MONOKAI = EditorTheme(
        "monokai", "Monokai",
        0xFF272822.toInt(), 0xFFF8F8F2.toInt(), 0xFF75715E.toInt(),
        0xFFF92672.toInt(), 0xFFE6DB74.toInt(), 0xFF75715E.toInt(),
        0xFFAE81FF.toInt(), 0xFF66D9EF.toInt()
    )

    val LIGHT = EditorTheme(
        "light", "Claro (GitHub)",
        0xFFFFFFFF.toInt(), 0xFF24292F.toInt(), 0xFF8C959F.toInt(),
        0xFFCF222E.toInt(), 0xFF0A3069.toInt(), 0xFF6E7781.toInt(),
        0xFF0550AE.toInt(), 0xFF953800.toInt()
    )

    val list = listOf(ONE_DARK, MONOKAI, LIGHT)

    fun byId(id: String?): EditorTheme = list.firstOrNull { it.id == id } ?: ONE_DARK
}
