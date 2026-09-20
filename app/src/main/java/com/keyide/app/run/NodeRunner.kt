package com.keyide.app.run

import android.content.Context
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/** Puente JNI a libnode (nodejs-mobile). */
object NodeRunner {

    private var loaded: Boolean = false

    init {
        loaded = try {
            System.loadLibrary("keyidenode")
            true
        } catch (e: Throwable) {
            false
        }
    }

    fun available(): Boolean = loaded

    /** Arranca Node con el bootstrap y devuelve el fd de LECTURA de su salida. */
    external fun nativeStart(bootstrap: String, cwd: String): Int

    /** fd de ESCRITURA (stdin de Node) para enviarle las rutas de scripts. */
    external fun nativeWriteFd(): Int
}

/**
 * Sesión Node PERSISTENTE: Node arranca una vez (node::Start solo puede llamarse
 * una vez por proceso) y va ejecutando los scripts que le enviamos por stdin.
 */
class NodeSession(private val context: Context) {

    private var out: FileOutputStream? = null
    private var pfd: ParcelFileDescriptor? = null

    @Volatile
    private var alive = false

    var onLine: ((String) -> Unit)? = null
    var onDone: (() -> Unit)? = null

    fun isAlive(): Boolean = alive

    fun start(cwd: File) {
        val boot = writeBootstrap()
        if (!boot.exists()) { alive = false; return }
        val rfd = runCatching { NodeRunner.nativeStart(boot.absolutePath, cwd.absolutePath) }.getOrDefault(-1)
        if (rfd < 0) { alive = false; return }
        val wfd = runCatching { NodeRunner.nativeWriteFd() }.getOrDefault(-1)
        if (wfd < 0) { alive = false; return }

        pfd = ParcelFileDescriptor.adoptFd(wfd)
        out = FileOutputStream(pfd!!.fileDescriptor)
        val input = FileInputStream(ParcelFileDescriptor.adoptFd(rfd).fileDescriptor)
        alive = true

        val t = Thread {
            val buf = ByteArray(4096)
            val pending = StringBuilder()
            try {
                while (alive) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    pending.append(String(buf, 0, n, Charsets.UTF_8))
                    flushLines(pending)
                }
            } catch (_: Exception) {
            } finally {
                alive = false
            }
        }
        t.isDaemon = true
        t.start()
    }

    fun send(scriptPath: String): Boolean = try {
        out?.write((scriptPath + "\n").toByteArray())
        out?.flush()
        true
    } catch (e: Exception) {
        false
    }

    fun stop() {
        alive = false
        runCatching { out?.close() }
        runCatching { pfd?.close() }
    }

    private fun flushLines(sb: StringBuilder) {
        while (true) {
            val idx = sb.indexOf("\n")
            if (idx < 0) break
            val line = sb.substring(0, idx).trimEnd('\r')
            sb.delete(0, idx + 1)
            if (line == DONE) onDone?.invoke() else if (line.isNotEmpty()) onLine?.invoke(line)
        }
        if (sb.length > 8192) {
            onLine?.invoke(sb.toString())
            sb.setLength(0)
        }
    }

    private fun writeBootstrap(): File {
        val f = File(context.filesDir, "keyide_node_bootstrap.js")
        runCatching { f.writeText(BOOTSTRAP) }
        return f
    }

    companion object {
        const val DONE = "__KEYIDE_NODE_DONE__"

        private val BOOTSTRAP = """
process.stdin.setEncoding('utf8');
console.log('[KeyIDE] Node ' + process.version + ' listo');
var __buf = '';
process.stdin.on('data', function (d) {
  __buf += d;
  var i;
  while ((i = __buf.indexOf('\n')) >= 0) {
    var line = __buf.slice(0, i).trim();
    __buf = __buf.slice(i + 1);
    if (!line) continue;
    try {
      Object.keys(require.cache).forEach(function (k) { delete require.cache[k]; });
      require(line);
    } catch (e) {
      console.error(e && e.stack ? e.stack : String(e));
    }
    console.log('__KEYIDE_NODE_DONE__');
  }
});
""".trimIndent()
    }
}
