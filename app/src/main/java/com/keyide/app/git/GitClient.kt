package com.keyide.app.git

import java.io.File

/**
 * Integración Git del dispositivo.
 * Estrategia: usar el binario `git` si existe (Termux / dispositivo rooteado);
 * si no, el panel lo indica y queda preparado para un backend nativo
 * (JGit / libgit2 vía NDK) como siguiente paso.
 */
object GitClient {

    fun gitBinary(): String? = listOf(
        "/system/bin/git",
        "/system/xbin/git",
        "/data/data/com.termux/files/usr/bin/git",
        "/usr/bin/git"
    ).firstOrNull { File(it).canExecute() }

    fun isAvailable(): Boolean = gitBinary() != null

    fun isRepo(dir: File): Boolean = File(dir, ".git").exists()

    fun remoteUrl(dir: File): String? {
        if (!isRepo(dir)) return null
        val cfg = File(dir, ".git/config")
        if (!cfg.exists()) return null
        return cfg.readText()
            .lineSequence()
            .dropWhile { !it.trim().startsWith("url =") }
            .firstOrNull()
            ?.substringAfter("url =")
            ?.trim()
    }
}
