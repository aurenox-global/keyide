package com.keyide.app.data

/** Familia de API que habla el proveedor. */
enum class ApiStyle { OPENAI, ANTHROPIC }

/** Proveedor de IA predefinido (todos los "grandes" + local). */
data class AiProvider(
    val id: String,
    val label: String,
    val base: String,
    val model: String,
    val style: ApiStyle,
    val hint: String = ""
)

/**
 * Catálogo de proveedores. Los OpenAI-compatible comparten formato; Anthropic
 * usa su propia API de mensajes. `local` apunta a llama.cpp/ollama en la LAN.
 */
object AiProviders {

    val list: List<AiProvider> = listOf(
        AiProvider("deepseek", "DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat", ApiStyle.OPENAI),
        AiProvider("openai", "OpenAI / ChatGPT", "https://api.openai.com/v1", "gpt-4o-mini", ApiStyle.OPENAI),
        AiProvider("anthropic", "Anthropic / Claude", "https://api.anthropic.com/v1", "claude-3-5-sonnet-latest", ApiStyle.ANTHROPIC),
        AiProvider("qwen", "Qwen (DashScope)", "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus", ApiStyle.OPENAI),
        AiProvider("kimi", "Kimi (Moonshot)", "https://api.moonshot.cn/v1", "moonshot-v1-8k", ApiStyle.OPENAI),
        AiProvider("glm", "GLM (Zhipu)", "https://open.bigmodel.cn/api/paas/v4", "glm-4-flash", ApiStyle.OPENAI),
        AiProvider("local", "Modelo local (llama.cpp / ollama)", "http://192.168.1.100:8080/v1", "local-model", ApiStyle.OPENAI,
            hint = "En el PC: llama-server --host 0.0.0.0 -m modelo.gguf. Mismo Wi-Fi que el móvil."),
        AiProvider("custom", "Personalizado (OpenAI-compatible)", "https://", "modelo", ApiStyle.OPENAI)
    )

    fun byId(id: String?): AiProvider = list.firstOrNull { it.id == id } ?: list.first()
}
