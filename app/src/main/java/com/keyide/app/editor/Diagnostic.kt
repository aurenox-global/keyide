package com.keyide.app.editor

/** Severidad de un diagnóstico procedente del analizador (LSP) o del linter. */
enum class Severity { ERROR, WARNING, INFO }

/** Diagnóstico asociado a una línea del documento. */
data class Diagnostic(val line: Int, val severity: Severity, val message: String)
