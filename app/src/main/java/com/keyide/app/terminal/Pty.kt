package com.keyide.app.terminal

/** Puente JNI al PTY nativo (libkeyidepty). */
object Pty {

    private var loaded: Boolean = false

    init {
        loaded = try {
            System.loadLibrary("keyidepty")
            true
        } catch (e: Throwable) {
            false
        }
    }

    fun available(): Boolean = loaded

    /** Abre un PTY con shell interactiva; devuelve el fd maestro o -1. */
    external fun nativeOpenPt(cwd: String): Int

    /** Fija el tamaño (filas/columnas) del PTY. */
    external fun nativeSetWin(fd: Int, rows: Int, cols: Int)
}
