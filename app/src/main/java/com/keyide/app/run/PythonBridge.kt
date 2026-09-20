package com.keyide.app.run

import com.chaquo.python.Python

/**
 * Intérprete Python REAL embebido (Chaquopy). Ejecuta el código en un hilo
 * aparte y devuelve todo lo que imprima por stdout/stderr.
 */
object PythonBridge {

    fun available(): Boolean = try {
        Python.getInstance()
        true
    } catch (e: Throwable) {
        false
    }

    fun run(
        code: String,
        breakpoints: Set<Int> = emptySet(),
        trace: Boolean = false,
        onResult: (String) -> Unit
    ) {
        Thread {
            val out = try {
                val mod = Python.getInstance().getModule("runner")
                if (breakpoints.isEmpty() && !trace) {
                    mod.callAttr("execute", code).toString()
                } else {
                    mod.callAttr("execute_debug", code, breakpoints.toList(), trace).toString()
                }
            } catch (e: Throwable) {
                "\u2716 ${e.javaClass.simpleName}: ${e.message ?: ""}"
            }
            onResult(out)
        }.start()
    }
}
