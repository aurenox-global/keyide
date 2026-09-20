package com.keyide.app.terminal

import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Sesión de terminal sobre un **PTY real** (posix_openpt + fork + sh -i).
 * Permite apps interactivas (control de trabajos, colores, `vim`/`htop`…).
 * Si el PTY no se puede abrir, `isAlive()` queda en false y el llamante usa
 * el shell persistente como fallback.
 */
class PtySession(private val cwd: File) : TerminalSession {

    private var fd = -1
    private var pfd: ParcelFileDescriptor? = null
    private var out: FileOutputStream? = null

    @Volatile
    private var alive = false

    override var onLine: ((String) -> Unit)? = null
    override var onClosed: (() -> Unit)? = null

    override fun isAlive(): Boolean = alive

    override fun start() {
        val f = runCatching { Pty.nativeOpenPt(cwd.absolutePath) }.getOrDefault(-1)
        if (f < 0) {
            alive = false
            return
        }
        fd = f
        runCatching { Pty.nativeSetWin(fd, 40, 100) }

        val p = ParcelFileDescriptor.adoptFd(fd)
        pfd = p
        val input = FileInputStream(p.fileDescriptor)
        out = FileOutputStream(p.fileDescriptor)
        alive = true

        val t = Thread {
            val buf = ByteArray(4096)
            val pending = StringBuilder()
            try {
                while (alive) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    pending.append(stripAnsi(String(buf, 0, n, Charsets.UTF_8)))
                    flushLines(pending)
                }
            } catch (_: Exception) {
            } finally {
                alive = false
                onClosed?.invoke()
            }
        }
        t.isDaemon = true
        t.start()
    }

    private fun flushLines(sb: StringBuilder) {
        while (true) {
            val nl = sb.indexOf("\n")
            val cr = sb.indexOf("\r")
            val idx = when {
                nl < 0 && cr < 0 -> -1
                nl < 0 -> cr
                cr < 0 -> nl
                else -> minOf(nl, cr)
            }
            if (idx < 0) break
            val line = sb.substring(0, idx)
            sb.delete(0, idx + 1)
            if (line.isNotEmpty()) onLine?.invoke(line)
        }
        if (sb.length > 8192) {
            onLine?.invoke(sb.toString())
            sb.setLength(0)
        }
    }

    override fun send(command: String): Boolean = try {
        out?.write((command + "\n").toByteArray())
        out?.flush()
        true
    } catch (e: Exception) {
        false
    }

    override fun stop() {
        alive = false
        runCatching { out?.close() }
        runCatching { pfd?.close() }
    }

    private fun stripAnsi(s: String): String = ANSI.replace(s, "")

    private companion object {
        val ANSI = Regex("\u001B\\[[0-9;?]*[ -/]*[@-~]")
    }
}
