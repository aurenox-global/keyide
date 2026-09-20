package com.keyide.app.git

import com.keyide.app.ui.Ui
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.keyide.app.data.Settings
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import com.keyide.app.R
import java.io.File

/**
 * Git NATIVO con JGit. Opera sobre la **carpeta activa** del explorador
 * (no sobre el demo interno fijo). Avisa si la carpeta activa es SAF, porque
 * JGit necesita un filesystem real (java.io.File) y los árboles SAF no lo son.
 */
class GitPanel(
    context: Context,
    private val settings: Settings,
    private val dirProvider: () -> File,
    private val isSaf: () -> Boolean
) : LinearLayout(context) {

    private val out = TextView(context)
    private val scroll = ScrollView(context)

    private val msgField = EditText(context)
    private val urlField = EditText(context)
    private val userField = EditText(context)
    private val tokenField = EditText(context)

    private val density = resources.displayMetrics.density
    private fun dp(v: Int) = (v * density).toInt()

    init {
        orientation = VERTICAL
        setBackgroundColor(Ui.bg)

        fun button(label: String, onClick: () -> Unit): Button = Button(context).apply {
            text = label
            textSize = 11f
            isAllCaps = false
            setOnClickListener { onClick() }
        }

        fun row(vararg views: android.view.View) = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(dp(8), dp(2), dp(8), dp(2))
            for (v in views) addView(v, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }

        fun field(hint: String, type: Int = InputType.TYPE_CLASS_TEXT): EditText =
            EditText(context).apply {
                this.hint = hint
                setTextColor(Ui.fg)
                setHintTextColor(Color.parseColor("#6E7681"))
                setBackgroundColor(Ui.panel)
                textSize = 12f
                inputType = type
                setPadding(dp(12), dp(10), dp(12), dp(10))
            }

        msgField.apply {
            hint = context.getString(R.string.git_msg)
            setTextColor(Ui.fg)
            setHintTextColor(Color.parseColor("#6E7681"))
            setBackgroundColor(Ui.panel)
            textSize = 12f
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        urlField.apply {
            hint = context.getString(R.string.git_url)
            setTextColor(Ui.fg)
            setHintTextColor(Color.parseColor("#6E7681"))
            setBackgroundColor(Ui.panel)
            textSize = 12f
            inputType = InputType.TYPE_TEXT_VARIATION_URI
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        userField.apply {
            hint = context.getString(R.string.git_user)
            setText(settings.gitUserName)
            setTextColor(Ui.fg)
            setHintTextColor(Color.parseColor("#6E7681"))
            setBackgroundColor(Ui.panel)
            textSize = 12f
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        tokenField.apply {
            hint = context.getString(R.string.git_token)
            setText(settings.gitToken)
            setTextColor(Ui.fg)
            setHintTextColor(Color.parseColor("#6E7681"))
            setBackgroundColor(Ui.panel)
            textSize = 12f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        out.apply {
            typeface = Typeface.MONOSPACE
            setTextColor(Color.parseColor("#C9D1D9"))
            textSize = 12f
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        scroll.addView(out, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        addView(msgField, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(row(
            button(context.getString(R.string.action_status)) { status() },
            button(context.getString(R.string.action_init)) { initRepo() },
            button(context.getString(R.string.git_commit)) { commit() }
        ), LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(row(
            button(context.getString(R.string.git_log)) { log() },
            button(context.getString(R.string.git_pull)) { pull() },
            button(context.getString(R.string.git_push)) { push() }
        ), LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(urlField, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(row(
            button(context.getString(R.string.git_clone_here)) { clone() },
            button(context.getString(R.string.git_clear_output)) { out.text = "" }
        ), LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(userField, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(tokenField, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(scroll, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun cwd(): File = dirProvider()

    /** Reinicia la salida y muestra el estado de la carpeta activa. */
    fun refreshRoot() {
        out.text = ""
        append("Git nativo (JGit) · repo: ${cwd().absolutePath}")
        if (isSaf()) {
            append("\u26A0 La carpeta abierta es SAF (content://). Git necesita un")
            append("  filesystem real, así que opera sobre el proyecto interno de arriba.")
        }
        status()
    }

    // ── Operaciones ───────────────────────────────────────────────────────

    fun runStatus() = status()

    fun runCommit() {
        if (msgField.text.isNullOrBlank()) msgField.setText("KeyIDE: commit")
        commit()
    }

    private fun status() = runOp("status") { git ->
        append("rama: ${git.repository.branch}  ·  remoto: ${remoteUrl(git) ?: "—"}")
        val st = git.status().call()
        val changed = (st.added + st.changed + st.modified + st.untracked + st.removed + st.missing)
        if (changed.isEmpty()) append("  (sin cambios)")
        else changed.sorted().forEach { append("  ${if (st.untracked.contains(it)) "??" else " M"} $it") }
    }

    private fun initRepo() = runOp("init") { git ->
        if (File(cwd(), ".git").exists()) append("Ya es un repositorio.")
    }

    private fun commit() = runOp("commit") { git ->
        val msg = msgField.text?.toString()?.ifBlank { "KeyIDE: commit" } ?: "KeyIDE: commit"
        git.add().addFilepattern(".").call()
        git.add().setUpdate(true).addFilepattern(".").call()
        val res = git.commit()
            .setMessage(msg)
            .setAuthor(settings.gitUserName, settings.gitUserEmail)
            .setCommitter(settings.gitUserName, settings.gitUserEmail)
            .call()
        append("commit ${res.name.take(8)} · $msg")
    }

    private fun log() = runOp("log") { git ->
        git.log().setMaxCount(20).call().forEach { c ->
            append("${c.name.take(8)}  ${c.shortMessage}  —  ${c.authorIdent.name}")
        }
    }

    private fun pull() = runOp("pull") { git ->
        val r = git.pull().setCredentialsProvider(creds()).call()
        append("pull: ${if (r.isSuccessful) "OK" else "con conflictos/avisos"}")
    }

    private fun push() = runOp("push") { git ->
        val r = git.push().setCredentialsProvider(creds()).call()
        r.forEach { append("push: ${it.messages.ifBlank { "OK" }}") }
    }

    private fun clone() = runOp("clone") { _ ->
        val url = urlField.text?.toString()?.trim().orEmpty()
        if (url.isBlank()) { append("\u26A0 Pon una URL para clonar."); return@runOp }
        val name = url.substringAfterLast('/').removeSuffix(".git").ifBlank { "repo" }
        val dest = File(cwd(), "cloned-$name")
        if (dest.exists()) { append("\u26A0 Ya existe ${dest.name}"); return@runOp }
        Git.cloneRepository()
            .setURI(url)
            .setDirectory(dest)
            .setCredentialsProvider(creds())
            .call()
            .use { append("clonado en ${dest.absolutePath}") }
    }

    // ── Infra ─────────────────────────────────────────────────────────────

    private fun creds(): UsernamePasswordCredentialsProvider {
        settings.gitUserName = userField.text?.toString()?.trim().orEmpty()
        settings.gitToken = tokenField.text?.toString()?.trim().orEmpty()
        return UsernamePasswordCredentialsProvider(settings.gitUserName, settings.gitToken)
    }

    private fun remoteUrl(git: Git): String? = runCatching {
        git.repository.config.getString("remote", "origin", "url")
    }.getOrNull()

    private fun runOp(name: String, block: (Git) -> Unit) {
        append("\n\u25B8 git $name")
        Thread {
            try {
                val dir = cwd()
                val isRepo = File(dir, ".git").exists()
                if (!isRepo) {
                    if (name == "init") {
                        Git.init().setDirectory(dir).setInitialBranch("main").call().use { block(it) }
                    } else {
                        post { append("\u26A0 No hay repositorio en ${dir.name}. Pulsa Init.") }
                    }
                } else {
                    Git.open(dir).use { block(it) }
                }
            } catch (e: Throwable) {
                post { append("\u2716 ${e.javaClass.simpleName}: ${e.message ?: ""}") }
            }
        }.start()
    }

    private fun append(line: String) {
        post {
            out.append(if (out.text.isEmpty()) line else "\n$line")
            scroll.post { scroll.fullScroll(Gravity.BOTTOM) }
        }
    }
}
