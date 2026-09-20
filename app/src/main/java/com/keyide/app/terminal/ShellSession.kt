package com.keyide.app.terminal

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Sesión de shell PERSISTENTE sobre `/system/bin/sh` (fallback si no hay PTY).
 * Mantiene el proceso vivo: `cd`, variables de entorno y estado se conservan
 * entre comandos. No es un PTY (sin control de trabajos ni apps de pantalla completa).
 */
class ShellSession(private val cwd: File) : TerminalSession {

    private var process: Process? = null
    private var writer: BufferedWriter? = null

    @Volatile
    private var alive = false

    override var onLine: ((String) -> Unit)? = null
    override var onClosed: (() -> Unit)? = null

    override fun isAlive(): Boolean = alive

    override fun start() {
        if (alive) return
        try {
            val p = ProcessBuilder("/system/bin/sh")
                .directory(cwd)
                .redirectErrorStream(true)
                .start()
            process = p
            writer = BufferedWriter(OutputStreamWriter(p.outputStream))
            alive = true
            Thread {
                try {
                    BufferedReader(InputStreamReader(p.inputStream)).use { r ->
                        r.forEachLine { line -> onLine?.invoke(line) }
                    }
                } catch (e: Exception) {
                    onLine?.invoke("! " + (e.message ?: e.toString()))
                } finally {
                    alive = false
                    onClosed?.invoke()
                }
            }.start()
            send("stty -echo 2>/dev/null; echo \"[KeyIDE] shell lista · \$(pwd)\"")
        } catch (e: Exception) {
            onLine?.invoke("! No se pudo iniciar la shell: ${e.message}")
        }
    }

    override fun send(command: String): Boolean {
        val w = writer ?: return false
        return try {
            w.write(command)
            w.write("\n")
            w.flush()
            true
        } catch (e: Exception) {
            onLine?.invoke("! " + (e.message ?: e.toString()))
            false
        }
    }

    override fun stop() {
        alive = false
        runCatching { writer?.write("exit\n"); writer?.flush() }
        runCatching { process?.destroy() }
        process = null
        writer = null
    }
}
