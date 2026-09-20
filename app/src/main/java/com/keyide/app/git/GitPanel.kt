package com.keyide.app.git

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
import com.keyide.app.files.WorkspaceRepo
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

/**
 * Git NATIVO con JGit (sin binario externo): estado, init, commit, log,
 * push/pull y clone. Corre en hilo aparte y vuelca todo al área de salida.
 */
class GitPanel(context: Context, private val settings: Settings) : LinearLayout(context) {

    private val out = TextView(context)
    private val scroll = ScrollView(context)
    private val root: File = WorkspaceRepo.root(context)

    private val msgField = EditText(context)
    private val urlField = EditText(context)
    private val userField = EditText(context)
    private val tokenField = EditText(context)

    private val density = resources.displayMetrics.density
    private fun dp(v: Int) = (v * density).toInt()

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#0D1117"))

        fun field(hint: String, type: Int = InputType.TYPE_CLASS_TEXT): EditText =
            EditText(context).apply {
                this.hint = hint
                setTextColor(Color.parseColor("#E6EDF3"))
                setHintTextColor(Color.parseColor("#6E7681"))
                setBackgroundColor(Color.parseColor("#161B22"))
                textSize = 12f
                inputType = type
                setPadding(dp(12), dp(10), dp(12), dp(10))
            }

        fun button(label: String, onClick: () -> Unit): Button = Button(context).apply {
            text = label
            textSize = 11f
            setOnClickListener { onClick() }
        }

        fun row(vararg views: android.view.View) = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(dp(8), dp(2), dp(8), dp(2))
            for (v in views) {
                addView(v, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            }
        }

        msgField.apply {
            hint = "Mensaje de commit"
            setTextColor(Color.parseColor("#E6EDF3"))
            setHintTextColor(Color.parseColor("#6E7681"))
            setBackgroundColor(Color.parseColor("#161B22"))
            textSize = 12f
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        urlField.apply {
            hint = "URL del remoto (clone / push)"
            setTextColor(Color.parseColor("#E6EDF3"))
            setHintTextColor(Color.parseColor("#6E7681"))
            setBackgroundColor(Color.parseColor("#161B22"))
            textSize = 12f
            inputType = InputType.TYPE_TEXT_VARIATION_URI
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        userField.apply {
            hint = "Usuario Git (email o login)"
            setText(settings.gitUserName)
            setTextColor(Color.parseColor("#E6EDF3"))
            setHintTextColor(Color.parseColor("#6E7681"))
            setBackgroundColor(Color.parseColor("#161B22"))
            textSize = 12f
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        tokenField.apply {
            hint = "Token (vacío si no hace falta)"
            setText(settings.gitToken)
            setTextColor(Color.parseColor("#E6EDF3"))
            setHintTextColor(Color.parseColor("#6E7681"))
            setBackgroundColor(Color.parseColor("#161B22"))
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
            button("Estado") { status() },
            button("Init") { initRepo() },
            button("Commit") { commit() }
        ), LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(row(
            button("Log") { log() },
            button("Pull") { pull() },
            button("Push") { push() }
        ), LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(urlField, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(row(
            button("Clone aquí") { clone() },
            button("Borrar salida") { out.text = "" }
        ), LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(userField, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(tokenField, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(scroll, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        append("Git nativo (JGit) · repo: ${root.absolutePath}")
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
        else changed.sorted().forEach { append("  ${st.untracked.contains(it).let { u -> if (u) "??" else " M" }} $it") }
    }

    private fun initRepo() = runOp("init") { git ->
        if (File(root, ".git").exists()) append("Ya es un repositorio.")
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
        val dest = File(root, "cloned-$name")
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
                val isRepo = File(root, ".git").exists()
                if (!isRepo) {
                    if (name == "init") {
                        Git.init().setDirectory(root).setInitialBranch("main").call().use { block(it) }
                    } else {
                        post { append("\u26A0 No hay repositorio. Pulsa Init.") }
                    }
                } else {
                    Git.open(root).use { block(it) }
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
