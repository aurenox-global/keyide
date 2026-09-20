package com.keyide.app.ai

import android.os.Handler
import android.os.Looper
import com.keyide.app.data.ApiStyle
import com.keyide.app.data.Settings
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente del copiloto. Soporta dos familias de API:
 *  - OPENAI     → /chat/completions (DeepSeek, OpenAI, Qwen, Kimi, GLM, local llama.cpp)
 *  - ANTHROPIC  → /messages con cabecera x-api-key
 */
class AiClient(private val settings: Settings) {

    private val main = Handler(Looper.getMainLooper())

    fun send(history: JSONArray, onResult: (String) -> Unit, onError: (String) -> Unit) {
        Thread {
            try {
                val reply = when (settings.aiStyle) {
                    ApiStyle.ANTHROPIC -> callAnthropic(history)
                    ApiStyle.OPENAI -> callOpenAi(history)
                }
                main.post { onResult(reply) }
            } catch (e: Exception) {
                main.post { onError(e.message ?: e.toString()) }
            }
        }.start()
    }

    // ── OpenAI-compatible ────────────────────────────────────────────────

    private fun callOpenAi(history: JSONArray): String {
        val base = settings.aiBaseUrl.trimEnd('/')
        val body = JSONObject()
            .put("model", settings.aiModel)
            .put("messages", history)
            .put("stream", false)
            .put("temperature", 0.3)
            .toString()
        val headers = mutableMapOf("Content-Type" to "application/json")
        if (settings.aiKey.isNotBlank()) headers["Authorization"] = "Bearer ${settings.aiKey}"

        val (code, text) = post("$base/chat/completions", headers, body)
        if (code !in 200..299) throw RuntimeException("HTTP $code · ${text.take(400)}")
        return JSONObject(text)
            .getJSONArray("choices").getJSONObject(0)
            .getJSONObject("message").optString("content", "")
            .ifBlank { "(respuesta vacía)" }
    }

    // ── Anthropic ────────────────────────────────────────────────────────

    private fun callAnthropic(history: JSONArray): String {
        val base = settings.aiBaseUrl.trimEnd('/')
        val url = if (base.endsWith("/messages")) base else "$base/messages"

        var system = ""
        val msgs = JSONArray()
        for (i in 0 until history.length()) {
            val m = history.getJSONObject(i)
            when (m.optString("role")) {
                "system" -> system = m.optString("content")
                "user", "assistant" -> msgs.put(
                    JSONObject().put("role", m.optString("role")).put("content", m.optString("content"))
                )
            }
        }
        val bodyObj = JSONObject()
            .put("model", settings.aiModel)
            .put("max_tokens", 1024)
            .put("messages", msgs)
        if (system.isNotBlank()) bodyObj.put("system", system)

        val headers = mutableMapOf(
            "Content-Type" to "application/json",
            "anthropic-version" to "2023-06-01"
        )
        if (settings.aiKey.isNotBlank()) headers["x-api-key"] = settings.aiKey

        val (code, text) = post(url, headers, bodyObj.toString())
        if (code !in 200..299) throw RuntimeException("HTTP $code · ${text.take(400)}")
        return JSONObject(text)
            .getJSONArray("content").getJSONObject(0).optString("text", "")
            .ifBlank { "(respuesta vacía)" }
    }

    // ── HTTP ─────────────────────────────────────────────────────────────

    private fun post(
        urlStr: String,
        headers: Map<String, String>,
        body: String,
        readTimeout: Int = 120_000
    ): Pair<Int, String> {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 30_000
        conn.readTimeout = readTimeout
        conn.doOutput = true
        headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        return code to text
    }
}
