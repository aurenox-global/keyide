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

    fun run(code: String, onResult: (String) -> Unit) {
        Thread {
            val out = try {
                Python.getInstance()
                    .getModule("runner")
                    .callAttr("execute", code)
                    .toString()
            } catch (e: Throwable) {
                "\u2716 ${e.javaClass.simpleName}: ${e.message ?: ""}"
            }
            onResult(out)
        }.start()
    }
}
