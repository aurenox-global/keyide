package com.keyide.app

import com.keyide.app.data.AiProviders
import com.keyide.app.editor.EditorThemes
import com.keyide.app.editor.Snippets
import com.keyide.app.util.Markdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests unitarios de la lógica pura (sin Android). */
class CoreTest {

    @Test
    fun markdownRenderizaTitulosYNegritas() {
        val html = Markdown.toHtml("# Hola\n\nEsto es **negrita** y `codigo`.")
        assertTrue(html.contains("<h1>Hola</h1>"))
        assertTrue(html.contains("<b>negrita</b>"))
        assertTrue(html.contains("<code>codigo</code>"))
    }

    @Test
    fun markdownEscapaHtml() {
        val html = Markdown.toHtml("<script>alert(1)</script>")
        assertTrue(html.contains("&lt;script&gt;"))
    }

    @Test
    fun snippetsPorLenguaje() {
        assertTrue(Snippets.forLanguage("javascript").isNotEmpty())
        assertTrue(Snippets.forLanguage("python").any { it.label == "def" })
        assertTrue(Snippets.forLanguage("html").any { it.label == "HTML5" })
        assertTrue(Snippets.forLanguage("desconocido").isNotEmpty())
    }

    @Test
    fun proveedoresYFallback() {
        assertEquals("deepseek", AiProviders.byId("deepseek").id)
        assertEquals("anthropic", AiProviders.byId("anthropic").style.name.lowercase())
        // id desconocido → primer proveedor
        assertEquals(AiProviders.list.first().id, AiProviders.byId("no-existe").id)
    }

    @Test
    fun temasDelEditor() {
        assertEquals(3, EditorThemes.list.size)
        assertEquals("onedark", EditorThemes.byId("onedark").id)
        assertEquals(EditorThemes.ONE_DARK.id, EditorThemes.byId(null).id)
    }
}
