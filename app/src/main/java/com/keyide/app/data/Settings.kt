package com.keyide.app.data

import android.content.Context

/** Preferencias locales de KeyIDE (sin cuenta, todo en el dispositivo). */
class Settings(context: Context) {

    private val sp = context.applicationContext.getSharedPreferences("keyide", Context.MODE_PRIVATE)

    /** id del proveedor activo (ver AiProviders). */
    var aiProvider: String
        get() = sp.getString("ai_provider", DEFAULT_PROVIDER) ?: DEFAULT_PROVIDER
        set(v) = sp.edit().putString("ai_provider", v).apply()

    var aiBaseUrl: String
        get() = sp.getString("ai_base", DEFAULT_BASE) ?: DEFAULT_BASE
        set(v) = sp.edit().putString("ai_base", v).apply()

    var aiKey: String
        get() = sp.getString("ai_key", "") ?: ""
        set(v) = sp.edit().putString("ai_key", v).apply()

    var aiModel: String
        get() = sp.getString("ai_model", DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(v) = sp.edit().putString("ai_model", v).apply()

    var lastFile: String
        get() = sp.getString("last_file", "") ?: ""
        set(v) = sp.edit().putString("last_file", v).apply()

    /** URI (persistido) de la carpeta elegida por el usuario vía SAF. */
    var treeUri: String
        get() = sp.getString("tree_uri", "") ?: ""
        set(v) = sp.edit().putString("tree_uri", v).apply()

    var wordWrap: Boolean
        get() = sp.getBoolean("word_wrap", false)
        set(v) = sp.edit().putBoolean("word_wrap", v).apply()

    var appTheme: String
        get() = sp.getString("app_theme", "dark") ?: "dark"
        set(v) = sp.edit().putString("app_theme", v).apply()

    var appLanguage: String
        get() = sp.getString("app_language", "system") ?: "system"
        set(v) = sp.edit().putString("app_language", v).apply()

    var jsTrace: Boolean
        get() = sp.getBoolean("js_trace", false)
        set(v) = sp.edit().putBoolean("js_trace", v).apply()

    /** Motor de JavaScript: "v8" (WebView, por defecto) o "node". */
    var jsEngine: String
        get() = sp.getString("js_engine", "v8") ?: "v8"
        set(v) = sp.edit().putString("js_engine", v).apply()

    /** Oculta la barra de navegación inferior mientras se escribe (interruptor de seguridad). */
    var hideBarWhileTyping: Boolean
        get() = sp.getBoolean("hide_bar_typing", false)
        set(v) = sp.edit().putBoolean("hide_bar_typing", v).apply()

    var editorTheme: String
        get() = sp.getString("editor_theme", "onedark") ?: "onedark"
        set(v) = sp.edit().putString("editor_theme", v).apply()

    var editorFontSize: Int
        get() = sp.getInt("editor_font_size", 13)
        set(v) = sp.edit().putInt("editor_font_size", v).apply()

    /** Lista de pestañas abiertas ("file:/ruta" o "uri:<uri>") separadas por \n. */
    var openTabs: String
        get() = sp.getString("open_tabs", "") ?: ""
        set(v) = sp.edit().putString("open_tabs", v).apply()

    var activeTab: Int
        get() = sp.getInt("active_tab", 0)
        set(v) = sp.edit().putInt("active_tab", v).apply()

    var gitUserName: String
        get() = sp.getString("git_user_name", "KeyIDE User") ?: "KeyIDE User"
        set(v) = sp.edit().putString("git_user_name", v).apply()

    var gitUserEmail: String
        get() = sp.getString("git_user_email", "keyide@localhost") ?: "keyide@localhost"
        set(v) = sp.edit().putString("git_user_email", v).apply()

    var gitToken: String
        get() = sp.getString("git_token", "") ?: ""
        set(v) = sp.edit().putString("git_token", v).apply()

    val aiStyle: ApiStyle
        get() = AiProviders.byId(aiProvider).style

    val aiProviderLabel: String
        get() = AiProviders.byId(aiProvider).label

    companion object {
        const val DEFAULT_PROVIDER = "deepseek"
        const val DEFAULT_BASE = "https://api.deepseek.com/v1"
        const val DEFAULT_MODEL = "deepseek-chat"
    }
}
