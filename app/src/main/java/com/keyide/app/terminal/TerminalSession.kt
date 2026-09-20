package com.keyide.app.terminal

/** Abstracción del terminal: PTY real o shell persistente (fallback). */
interface TerminalSession {
    var onLine: ((String) -> Unit)?
    var onClosed: (() -> Unit)?
    fun isAlive(): Boolean
    fun start()
    fun send(command: String): Boolean
    fun stop()
}
