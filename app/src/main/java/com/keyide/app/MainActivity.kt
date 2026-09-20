package com.keyide.app

import com.keyide.app.ui.Ui
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.documentfile.provider.DocumentFile
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.keyide.app.ai.AiPanel
import com.keyide.app.data.Settings
import com.keyide.app.databinding.ActivityMainBinding
import com.keyide.app.editor.Diagnostic
import com.keyide.app.editor.EditorThemes
import com.keyide.app.editor.Severity
import com.keyide.app.editor.Snippets
import com.keyide.app.editor.Syntax
import com.keyide.app.files.Entry
import com.keyide.app.files.FilesPanel
import com.keyide.app.files.WorkspaceRepo
import com.keyide.app.files.mimeFor
import com.keyide.app.git.GitPanel
import com.keyide.app.edit.FindPanel
import com.keyide.app.edit.Hit
import com.keyide.app.edit.ProjectSearchPanel
import com.keyide.app.palette.CommandPalette
import com.keyide.app.preview.PreviewPanel
import com.keyide.app.run.JsRunner
import com.keyide.app.run.PythonBridge
import com.keyide.app.settings.SettingsPanel
import com.keyide.app.terminal.TerminalPanel
import com.keyide.app.util.Markdown
import java.io.File
import java.util.Locale

/**
 * KeyIDE · v0.7.0
 * Explorador con SAF (abrir/c crear en carpetas reales), pestañas, split,
 * vista previa, Run real (JS/Python), Git, IA multi-proveedor y "Acerca de".
 */
class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var sheetBehavior: BottomSheetBehavior<View>
    private lateinit var settings: Settings

    /** Documento abierto: interno (file) o SAF (uri). */
    private class Doc(
        var name: String,
        var file: File?,
        var uri: Uri?,
        val lang: String,
        var content: String,
        var modified: Boolean
    )

    private val docs = mutableListOf<Doc>()
    private var active = -1
    private val chipViews = mutableListOf<View>()

    private val currentFile: File? get() = docs.getOrNull(active)?.file
    private val currentDoc: Doc? get() = docs.getOrNull(active)

    // Explorador
    private var safMode = false
    private val internalStack = mutableListOf<File>()
    private val safStack = mutableListOf<DocumentFile>()

    private var filesPanel: FilesPanel? = null
    private var terminalPanel: TerminalPanel? = null
    private var gitPanel: GitPanel? = null
    private var aiPanel: AiPanel? = null
    private var previewPanel: PreviewPanel? = null
    private var jsRunner: JsRunner? = null
    private var findPanel: FindPanel? = null
    private var searchPanel: ProjectSearchPanel? = null

    private val pickFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) onFolderPicked(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Idioma (ES/EN/Sistema) ANTES de inflar la UI.
        val prefLang = getSharedPreferences("keyide", MODE_PRIVATE).getString("app_language", "system") ?: "system"
        val wantLocales = if (prefLang == "system") LocaleListCompat.getEmptyLocaleList()
        else LocaleListCompat.forLanguageTags(prefLang)
        if (AppCompatDelegate.getApplicationLocales() != wantLocales) {
            AppCompatDelegate.setApplicationLocales(wantLocales)
        }

        // Tema GLOBAL (oscuro/claro/sistema) ANTES de inflar la UI.
        val prefTheme = getSharedPreferences("keyide", MODE_PRIVATE).getString("app_theme", "dark") ?: "dark"
        val sysDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val isLight = when (prefTheme) {
            "light" -> true
            "dark" -> false
            else -> !sysDark
        }
        com.keyide.app.ui.Ui.applyTheme(isLight)
        setTheme(if (isLight) R.style.Theme_KeyIDE_Light else R.style.Theme_KeyIDE)

        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        settings = Settings(this)
        WorkspaceRepo.ensure(this)
        internalStack.clear()
        internalStack.add(WorkspaceRepo.root(this))

        WindowCompat.setDecorFitsSystemWindows(window, false)
        applyInsets()

        sheetBehavior = BottomSheetBehavior.from(b.sheet)
        sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        buildKeysBar()
        setupNav()
        setupToolbar()

        b.sheetClose.setOnClickListener { hideSheet() }
        b.editor.onChanged = { onEditorChanged() }
        b.editor.setWordWrap(settings.wordWrap)

        restoreSafIfPossible()
        restoreSession()
        applyEditorPrefs()

        if (isWide()) showSplit()
    }

    // ── Insets ────────────────────────────────────────────────────────────

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(b.root) { _, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottom = maxOf(sys.bottom, ime.bottom)
            // La barra inferior (teclas + nav) queda SIEMPRE pegada encima del teclado,
            // sin estirarse: desplazamos TODA la columna (padding del contenedor),
            // NO metemos el inset como padding dentro de la barra.
            b.rootColumn.setPadding(0, sys.top, 0, bottom)
            b.sheet.setPadding(0, 0, 0, bottom)
            insets
        }
        ViewCompat.requestApplyInsets(b.root)
    }

    // ── Toolbar ───────────────────────────────────────────────────────────

    private fun setupToolbar() {
        b.toolbar.inflateMenu(R.menu.main_menu)
        b.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_split -> { toggleSplit(); true }
                R.id.action_preview -> { openPreview(); true }
                R.id.action_open_folder -> { pickFolder.launch(null); true }
                R.id.action_save -> { saveCurrent(); true }
                R.id.action_about -> { showAbout(); true }
                R.id.action_settings -> { openSheet(SHEET_SETTINGS); true }
                R.id.action_palette -> { showPalette(); true }
                else -> false
            }
        }
        b.toolbar.setOnLongClickListener { showPalette(); true }
    }

    // ── SAF ───────────────────────────────────────────────────────────────

    private fun restoreSafIfPossible() {
        val saved = settings.treeUri
        if (saved.isBlank()) return
        runCatching {
            val uri = Uri.parse(saved)
            val root = DocumentFile.fromTreeUri(this, uri)
            if (root != null && root.canRead()) {
                safMode = true
                safStack.clear()
                safStack.add(root)
            }
        }
    }

    private fun onFolderPicked(uri: Uri) {
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        settings.treeUri = uri.toString()
        val root = DocumentFile.fromTreeUri(this, uri)
        if (root == null) { toast("No se pudo abrir la carpeta"); return }
        safMode = true
        safStack.clear()
        safStack.add(root)
        openSheet(SHEET_FILES)
        toast("Carpeta abierta: ${root.name ?: uri.lastPathSegment}")
    }

    // ── Explorador ────────────────────────────────────────────────────────

    private fun currentEntries(): List<Entry> {
        if (safMode) {
            val dir = safStack.lastOrNull() ?: return emptyList()
            val files = dir.listFiles().toList()
            return files
                .filter { it.name != null }
                .sortedWith(compareBy({ !it.isDirectory }, { it.name!!.lowercase(Locale.ROOT) }))
                .map { Entry(it.name ?: "?", it.isDirectory, doc = it) }
        }
        val dir = internalStack.lastOrNull() ?: WorkspaceRepo.root(this)
        val files = dir.listFiles() ?: return emptyList()
        return files
            .filter { !it.name.startsWith(".") }
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.ROOT) }))
            .map { Entry(it.name, it.isDirectory, file = it) }
    }

    private fun pathLabel(): String = if (safMode) {
        "SAF · " + safStack.joinToString(" / ") { it.name ?: "?" }
    } else {
        internalStack.joinToString("/") { it.name }
    }

    private fun canGoUp(): Boolean =
        if (safMode) safStack.size > 1 else internalStack.size > 1

    private fun refreshExplorer() {
        filesPanel?.render(pathLabel(), currentEntries(), canGoUp())
    }

    private fun openEntry(e: Entry) {
        if (e.isDir) {
            if (safMode) e.doc?.let { safStack.add(it) } else e.file?.let { internalStack.add(it) }
            refreshExplorer()
        } else {
            if (safMode) e.doc?.let { openDocument(it) } else e.file?.let { openFile(it) }
            hideSheet()
        }
    }

    private fun upOneLevel() {
        if (safMode) { if (safStack.size > 1) safStack.removeAt(safStack.size - 1) }
        else { if (internalStack.size > 1) internalStack.removeAt(internalStack.size - 1) }
        refreshExplorer()
    }

    private fun promptNewFile() {
        val input = EditText(this).apply {
            hint = "nombre.ext"
            setText("nuevo.txt")
            setPadding(dp(20), dp(12), dp(20), dp(12))
        }
        AlertDialog.Builder(this)
            .setTitle("Nuevo fichero en ${pathLabel()}")
            .setView(input)
            .setPositiveButton("Crear") { _, _ ->
                val name = input.text?.toString()?.trim().orEmpty()
                if (name.isNotEmpty()) createNewFile(name)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun createNewFile(name: String) {
        val template = "// $name\n"
        if (safMode) {
            val dir = safStack.lastOrNull() ?: return
            val created = runCatching { dir.createFile(mimeFor(name), name) }.getOrNull()
            if (created == null) { toast("No se pudo crear (¿permiso de escritura?)"); return }
            runCatching {
                contentResolver.openOutputStream(created.uri, "wt")?.use { it.write(template.toByteArray()) }
            }
            openDocument(created)
        } else {
            val target = File(internalStack.lastOrNull() ?: WorkspaceRepo.root(this), name)
            if (target.exists()) { toast("Ya existe ese fichero"); return }
            runCatching { target.writeText(template) }
            openFile(target)
        }
        refreshExplorer()
    }

    private fun promptNewFolder() {
        val input = EditText(this).apply {
            hint = "nombre de carpeta"
            setText("src")
            setPadding(dp(20), dp(12), dp(20), dp(12))
        }
        AlertDialog.Builder(this)
            .setTitle("Nueva carpeta")
            .setView(input)
            .setPositiveButton("Crear") { _, _ ->
                val name = input.text?.toString()?.trim().orEmpty()
                if (name.isEmpty()) return@setPositiveButton
                if (safMode) {
                    safStack.lastOrNull()?.createDirectory(name)
                } else {
                    File(internalStack.lastOrNull() ?: WorkspaceRepo.root(this), name).mkdirs()
                }
                refreshExplorer()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ── Documentos ────────────────────────────────────────────────────────

    private fun openFile(file: File) {
        if (!file.isFile) return
        val key = file.absolutePath
        docs.indexOfFirst { it.file?.absolutePath == key }.takeIf { it >= 0 }?.let { switchTo(it); return }
        val content = runCatching { file.readText() }.getOrNull() ?: return
        docs.add(Doc(file.name, file, null, Syntax.languageOf(file.name), content, false))
        settings.lastFile = key
        switchTo(docs.size - 1)
    }

    private fun openDocument(df: DocumentFile) {
        val uri = df.uri
        docs.indexOfFirst { it.uri == uri }.takeIf { it >= 0 }?.let { switchTo(it); return }
        val content = runCatching {
            contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        }.getOrNull()
        if (content == null) { toast("No se pudo leer el fichero (¿binario?)"); return }
        val name = df.name ?: uri.lastPathSegment ?: "documento"
        docs.add(Doc(name, null, uri, Syntax.languageOf(name), content, false))
        switchTo(docs.size - 1)
    }

    private fun stashCurrent() {
        val d = docs.getOrNull(active) ?: return
        d.content = b.editor.text()
        d.modified = b.editor.isModified
    }

    private fun switchTo(index: Int) {
        if (index !in docs.indices) return
        stashCurrent()
        active = index
        val d = docs[index]
        b.editor.loadFile(d.name, d.content, d.lang)
        b.editor.setWordWrap(settings.wordWrap)
        applyEditorPrefs()
        b.editor.isModified = d.modified
        b.editor.setDiagnostics(analyze(d.content))
        renderTabs()
        updateTitle()
        persistSession()
    }

    private fun closeTab(index: Int) {
        if (index !in docs.indices) return
        if (index == active && docs[index].modified) saveCurrent()
        docs.removeAt(index)
        when {
            docs.isEmpty() -> {
                active = -1
                b.editor.loadFile("", "", "txt")
                b.editor.isModified = false
                renderTabs(); updateTitle(); persistSession()
            }
            index < active -> { active -= 1; switchTo(active) }
            index == active -> switchTo(active.coerceAtMost(docs.size - 1))
            else -> { renderTabs(); updateTitle() }
        }
    }

    private fun renderTabs() {
        b.tabsRow.removeAllViews()
        chipViews.clear()
        for ((i, d) in docs.withIndex()) {
            val chip = buildChip(d, i)
            b.tabsRow.addView(chip, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(4) })
            chipViews.add(chip)
        }
        b.tabsBar.visibility = if (docs.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun buildChip(d: Doc, index: Int): View {
        val chip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setBackgroundResource(if (index == active) R.drawable.bg_tab_active else R.drawable.bg_key)
            setPadding(dp(10), dp(6), dp(4), dp(6))
        }
        val name = TextView(this).apply {
            text = d.name + (if (d.modified) " \u25CF" else "")
            setTextColor(Ui.fg)
            textSize = 12f
            typeface = android.graphics.Typeface.MONOSPACE
            setOnClickListener { switchTo(index) }
        }
        val close = TextView(this).apply {
            text = "\u2715"
            setTextColor(Ui.mut)
            textSize = 12f
            setPadding(dp(8), 0, dp(4), 0)
            setOnClickListener { closeTab(index) }
        }
        chip.addView(name)
        chip.addView(close)
        return chip
    }

    private fun onEditorChanged() {
        val d = docs.getOrNull(active) ?: return
        d.content = b.editor.text()
        d.modified = b.editor.isModified
        updateActiveChip()
        updateTitle()
    }

    private fun updateActiveChip() {
        chipViews.getOrNull(active)?.let { chip ->
            if (chip is LinearLayout && chip.childCount > 0) {
                val name = chip.getChildAt(0) as TextView
                val d = docs[active]
                name.text = d.name + (if (d.modified) " \u25CF" else "")
            }
        }
    }

    private fun updateTitle() {
        b.toolbar.title = currentDoc?.name ?: getString(R.string.app_name)
        val d = docs.getOrNull(active)
        val loc = if (d?.uri != null) "SAF" else "interno"
        b.toolbar.subtitle = buildString {
            append(loc)
            if (d?.modified == true) append("  \u25CF")
            append("  ·  ${b.editor.language}")
            if (docs.size > 1) append("  ·  ${docs.size} abiertos")
        }
    }

    private fun analyze(text: String): List<Diagnostic> {
        val out = ArrayList<Diagnostic>()
        text.lineSequence().forEachIndexed { i, line ->
            val l = line.uppercase(Locale.ROOT)
            when {
                l.contains("FIXME") -> out.add(Diagnostic(i + 1, Severity.ERROR, "FIXME pendiente"))
                l.contains("TODO") -> out.add(Diagnostic(i + 1, Severity.WARNING, "TODO pendiente"))
            }
        }
        return out
    }

    private fun saveCurrent(): Boolean {
        val d = docs.getOrNull(active) ?: return false
        val text = b.editor.text()
        val uri = d.uri
        val ok = runCatching {
            if (uri != null) {
                contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
                    ?: return false
            } else {
                d.file?.parentFile?.mkdirs()
                d.file?.writeText(text)
            }
            true
        }.getOrDefault(false)
        if (ok) {
            d.content = text
            d.modified = false
            b.editor.isModified = false
            b.editor.setDiagnostics(analyze(text))
            updateActiveChip()
            updateTitle()
        } else {
            toast("No se pudo guardar")
        }
        return ok
    }

    // ── Barra de símbolos ─────────────────────────────────────────────────

    private fun buildKeysBar() {
        val symbols = listOf(
            "\u21B6", "\u21B7", "\uD83D\uDD0D",
            "TAB", "{", "}", "(", ")", "[", "]", "<", ">", ";", "\"", "'",
            "=", "+", "-", "/", "*", "_", "$", "#", "@", ":", ".", ",", "|", "&", "!", "?", "%", "~", "\\", "`", "⌘"
        )
        for (s in symbols) {
            val key = TextView(this).apply {
                text = if (s == "TAB") "⇥" else s
                setTextColor(Ui.fg)
                textSize = 15f
                typeface = android.graphics.Typeface.MONOSPACE
                gravity = android.view.Gravity.CENTER
                setBackgroundResource(R.drawable.bg_key)
                setOnClickListener {
                    when (s) {
                        "TAB" -> b.editor.insert("  ")
                        "⌘" -> showPalette()
                        "\u21B6" -> b.editor.undo()
                        "\u21B7" -> b.editor.redo()
                        "\uD83D\uDD0D" -> openSheet(SHEET_FIND)
                        else -> b.editor.insert(s)
                    }
                }
            }
            val lp = ViewGroup.MarginLayoutParams(dp(40), dp(40))
            lp.setMargins(dp(3), 0, dp(3), 0)
            b.keysRow.addView(key, lp)
        }
    }

    private fun toggleKeys() {
        b.keysBar.visibility = if (b.keysBar.visibility != View.VISIBLE) View.VISIBLE else View.GONE
    }

    // ── Navegación ────────────────────────────────────────────────────────

    private fun setupNav() {
        b.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_files -> openSheet(SHEET_FILES)
                R.id.nav_keys -> toggleKeys()
                R.id.nav_run -> runCurrent()
                R.id.nav_git -> openSheet(SHEET_GIT)
                R.id.nav_ai -> openSheet(SHEET_AI)
            }
            true
        }
        b.bottomNav.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.nav_keys) toggleKeys()
        }
    }

    // ── Sheets ────────────────────────────────────────────────────────────

    private fun openSheet(kind: Int) {
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
        b.sheetContent.removeAllViews()
        when (kind) {
            SHEET_FILES -> {
                val panel = filesPanel ?: FilesPanel(this).also {
                    it.onOpen = { e -> openEntry(e) }
                    it.onUp = { upOneLevel() }
                    it.onPickFolder = { pickFolder.launch(null) }
                    it.onNewFile = { promptNewFile() }
                    it.onNewFolder = { promptNewFolder() }
                    filesPanel = it
                }
                b.sheetTitle.text = getString(R.string.explorer_prefix) + " · " + (if (safMode) safStack.lastOrNull()?.name else internalStack.lastOrNull()?.name).orEmpty()
                b.sheetContent.addView(panel, params)
                refreshExplorer()
            }
            SHEET_TERMINAL -> {
                val panel = terminalPanel ?: TerminalPanel(this).also { terminalPanel = it }
                b.sheetTitle.text = getString(R.string.sheet_terminal)
                b.sheetContent.addView(panel, params)
            }
            SHEET_GIT -> {
                val panel = gitPanel ?: GitPanel(
                    this, settings,
                    { internalStack.lastOrNull() ?: WorkspaceRepo.root(this) },
                    { safMode }
                ).also { gitPanel = it }
                b.sheetTitle.text = getString(R.string.sheet_git)
                b.sheetContent.addView(panel, params)
                panel.refreshRoot()
            }
            SHEET_AI -> {
                val panel = aiPanel ?: AiPanel(this, settings).also { aiPanel = it }
                currentDoc?.let { d -> runCatching { panel.setContext(d.name, b.editor.text()) } }
                b.sheetTitle.text = getString(R.string.sheet_ai)
                b.sheetContent.addView(panel, params)
            }
            SHEET_SETTINGS -> {
                val panel = SettingsPanel(this, settings) { aiPanel = null; updateTitle() }
                b.sheetTitle.text = getString(R.string.sheet_settings)
                b.sheetContent.addView(panel, params)
            }
            SHEET_FIND -> {
                val panel = findPanel ?: FindPanel(this, b.editor).also { findPanel = it }
                b.sheetTitle.text = getString(R.string.sheet_find)
                b.sheetContent.addView(panel, params)
            }
            SHEET_SEARCH -> {
                val panel = searchPanel ?: ProjectSearchPanel(
                    this,
                    search = { q -> searchInProject(q) },
                    onOpen = { hit -> openSearchResult(hit) }
                ).also { searchPanel = it }
                b.sheetTitle.text = getString(R.string.sheet_search)
                b.sheetContent.addView(panel, params)
            }
        }
        sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun hideSheet() { sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN }

    // ── Vista previa / split ──────────────────────────────────────────────

    private fun ensurePreview(): PreviewPanel =
        previewPanel ?: PreviewPanel(this).also { previewPanel = it }

    private fun loadPreviewContent(p: PreviewPanel) {
        val d = currentDoc ?: return
        val ext = d.name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        when (ext) {
            "html", "htm", "svg" -> loadIntoPreview(p, d)
            "md", "markdown" -> p.loadHtml(Markdown.toHtml(b.editor.text()), null, d.name)
            else -> p.loadHtml(
                "<body style='font-family:sans-serif;padding:20px;color:#8b949e'>" +
                    "Sin vista previa para .$ext.<br>Usa <b>Run</b>.</body>", null, "—"
            )
        }
    }

    private fun loadIntoPreview(p: PreviewPanel, d: Doc) {
        val uri = d.uri
        if (uri != null) {
            // Documento SAF: se inyecta el HTML directamente.
            val html = runCatching {
                contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            }.getOrNull() ?: ""
            p.loadHtml(html, null, d.name)
        } else {
            d.file?.let { p.loadFile(it) }
        }
    }

    private fun showSplit() {
        if (b.editor.isModified) saveCurrent()
        val p = ensurePreview()
        loadPreviewContent(p)
        (p.parent as? ViewGroup)?.removeView(p)
        b.splitPreview.removeAllViews()
        b.splitPreview.addView(p, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
        b.splitPreview.visibility = View.VISIBLE
    }

    private fun hideSplit() {
        b.splitPreview.removeAllViews()
        b.splitPreview.visibility = View.GONE
    }

    private fun toggleSplit() {
        if (b.splitPreview.visibility == View.VISIBLE) hideSplit() else showSplit()
    }

    private fun openPreview() {
        if (isWide() || b.splitPreview.visibility == View.VISIBLE) { showSplit(); return }
        if (b.editor.isModified) saveCurrent()
        val p = ensurePreview()
        loadPreviewContent(p)
        (p.parent as? ViewGroup)?.removeView(p)
        b.sheetContent.removeAllViews()
        b.sheetContent.addView(p, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
        b.sheetTitle.text = getString(R.string.sheet_preview)
        sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    // ── Run ───────────────────────────────────────────────────────────────

    private fun runCurrent() {
        val d = currentDoc
        if (d == null) { openSheet(SHEET_TERMINAL); return }
        if (b.editor.isModified) saveCurrent()
        openSheet(SHEET_TERMINAL)
        val ext = d.name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        when (ext) {
            "js", "mjs", "cjs" -> runJs(d)
            "py" -> runPy(d)
            else -> {
                val cmd = when (ext) {
                    "sh" -> "sh ${d.name}"
                    "html", "htm" -> "echo 'Usa Vista previa (ojo) para renderizar el HTML'"
                    else -> "echo 'Sin runner para .$ext'"
                }
                terminalPanel?.post { terminalPanel?.runCommand(cmd) }
            }
        }
    }

    private fun runJs(d: Doc) {
        val js = jsRunner ?: JsRunner(this).also { jsRunner = it }
        val (base, modules) = buildJsModules()
        val bp = b.editor.breakpoints.toSet()
        val trace = settings.jsTrace
        val mode = buildString {
            append("V8 + require local · ${modules.size} módulos")
            if (bp.isNotEmpty()) append(" · \uD83D\uDC1E ${bp.size} puntos de parada")
            if (trace) append(" · traza")
        }
        terminalPanel?.appendOutput("$ node ${d.name}   ($mode)")
        if (bp.isEmpty() && !trace) {
            terminalPanel?.appendOutput("\u2139 Toca el margen izquierdo para poner/quitar puntos de parada.")
        }
        js.run(b.editor.text(), base, modules, bp, trace,
            onLine = { line -> terminalPanel?.appendOutput(line) },
            onDone = { terminalPanel?.appendOutput("\u2714 fin de la ejecución") },
            onPause = { line -> terminalPanel?.appendOutput("\u23F8 pausa en la línea $line (quita el punto y vuelve a ejecutar)") }
        )
    }

    private fun toggleJsTrace() {
        settings.jsTrace = !settings.jsTrace
        toast(if (settings.jsTrace) "Traza JS: ON" else "Traza JS: OFF")
    }

    /** Módulos JS locales (ruta relativa → código) para el `require` del runner. */
    private fun buildJsModules(): Pair<String, Map<String, String>> {
        val d = currentDoc ?: return "" to emptyMap()
        val f = d.file ?: return d.name to emptyMap()
        val root = internalStack.lastOrNull() ?: WorkspaceRepo.root(this)
        fun rel(x: File): String = runCatching { root.toURI().relativize(x.toURI()).path }.getOrDefault(x.name)
        val map = HashMap<String, String>()
        fun walk(dir: File, depth: Int) {
            if (depth > 6 || map.size >= 200) return
            dir.listFiles()?.forEach { c ->
                if (c.name.startsWith(".")) return@forEach
                if (c.isDirectory) walk(c, depth + 1)
                else if (c.extension.lowercase(Locale.ROOT) == "js" && c.length() < 500_000) {
                    runCatching { map[rel(c)] = c.readText() }
                }
            }
        }
        walk(root, 0)
        map[rel(f)] = b.editor.text()
        return rel(f) to map
    }

    private fun runPy(d: Doc) {
        if (PythonBridge.available()) {
            terminalPanel?.appendOutput("$ python3 ${d.name}   (intérprete embebido)")
            PythonBridge.run(b.editor.text()) { out ->
                terminalPanel?.appendOutput(if (out.isBlank()) "(sin salida)" else out.trimEnd())
                terminalPanel?.appendOutput("\u2714 fin de la ejecución")
            }
        } else {
            terminalPanel?.post {
                terminalPanel?.runCommand(
                    "(python3 ${d.name} 2>&1 || python ${d.name} 2>&1) || echo '\u26A0 Sin intérprete Python embebido.'"
                )
            }
        }
    }

    // ── Acerca de ─────────────────────────────────────────────────────────

    private fun showAbout() {
        val msg = """
            KeyIDE · v${BuildConfig.VERSION_NAME}

            IDE de código para Android.

            Creado por Andrés Mag.

            Editor con pestañas, autocompletado y diagnósticos · Vista previa web
            · Run real de JavaScript y Python (intérprete embebido) · Terminal
            integrado · Git nativo (JGit) · Copiloto IA multi-proveedor
            (DeepSeek, OpenAI, Claude, Qwen, Kimi, GLM o modelo local llama.cpp).

            Explorador con acceso a carpetas reales del dispositivo (SAF).

            (C) 2026 Andrés Mag
        """.trimIndent()
        AlertDialog.Builder(this)
            .setTitle("Acerca de KeyIDE")
            .setMessage(msg)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    // ── Paleta ────────────────────────────────────────────────────────────

    private fun showPalette() {
        val cmds = listOf(
            CommandPalette.Cmd("Archivo: Abrir carpeta… (SAF)") { pickFolder.launch(null) },
            CommandPalette.Cmd("Archivo: Nuevo fichero") { promptNewFile() },
            CommandPalette.Cmd("Archivo: Guardar") { saveCurrent() },
            CommandPalette.Cmd("Archivo: Guardar como…") { saveAs() },
            CommandPalette.Cmd("Archivo: Renombrar actual…") { renameCurrent() },
            CommandPalette.Cmd("Archivo: Borrar actual…") { deleteCurrent() },
            CommandPalette.Cmd("Archivo: Cerrar pestaña") { if (active >= 0) closeTab(active) },
            CommandPalette.Cmd("Edición: Deshacer") { b.editor.undo() },
            CommandPalette.Cmd("Edición: Rehacer") { b.editor.redo() },
            CommandPalette.Cmd("Edición: Buscar y reemplazar") { openSheet(SHEET_FIND) },
            CommandPalette.Cmd("Edición: Buscar en el proyecto…") { openSheet(SHEET_SEARCH) },
            CommandPalette.Cmd("Edición: Ir a definición") { goToDefinition() },
            CommandPalette.Cmd("Edición: Información del símbolo (hover)") { showHover() },
            CommandPalette.Cmd("Edición: Buscar referencias") { showReferences() },
            CommandPalette.Cmd("Edición: Renombrar símbolo…") { renameSymbol() },
            CommandPalette.Cmd("Edición: Formatear documento") { formatDocument() },
            CommandPalette.Cmd("Edición: Insertar snippet…") { showSnippets() },
            CommandPalette.Cmd("Ver: Dividir editor + vista previa") { toggleSplit() },
            CommandPalette.Cmd("Ver: Vista previa") { openPreview() },
            CommandPalette.Cmd("Ver: Explorador") { openSheet(SHEET_FILES) },
            CommandPalette.Cmd("Ver: Terminal") { openSheet(SHEET_TERMINAL) },
            CommandPalette.Cmd("Ver: Barra de símbolos") { toggleKeys() },
            CommandPalette.Cmd("Ver: Ajuste de línea (wrap)") { toggleWordWrap() },
            CommandPalette.Cmd("Ver: Tema del editor…") { showThemeDialog() },
            CommandPalette.Cmd("Ver: Tema de la app (oscuro/claro)…") { showAppThemeDialog() },
            CommandPalette.Cmd("Ver: Idioma (Sistema / ES / EN)…") { showLanguageDialog() },
            CommandPalette.Cmd("Ver: Tamaño de fuente…") { showFontSizeDialog() },
            CommandPalette.Cmd("Edición: Ir a línea…") { goToLineDialog() },
            CommandPalette.Cmd("Ejecutar: fichero actual") { runCurrent() },
            CommandPalette.Cmd("Depurar: ejecutar con puntos de parada") { runCurrent() },
            CommandPalette.Cmd("Depurar: activar/desactivar traza") { toggleJsTrace() },
            CommandPalette.Cmd("Depurar: limpiar puntos de parada") { b.editor.clearBreakpoints(); toast("Puntos de parada borrados") },
            CommandPalette.Cmd("Git: Estado") { openSheet(SHEET_GIT) },
            CommandPalette.Cmd("Git: Commit + Push") { openSheet(SHEET_GIT); gitPanel?.post { gitPanel?.runCommit() } },
            CommandPalette.Cmd("AI: Abrir copiloto") { openSheet(SHEET_AI) },
            CommandPalette.Cmd("AI: Ajustes (proveedor/modelo)") { openSheet(SHEET_SETTINGS) },
            CommandPalette.Cmd("Ayuda: Acerca de") { showAbout() }
        )
        CommandPalette.show(this, cmds)
    }

    private fun toggleWordWrap() {
        val w = !settings.wordWrap
        settings.wordWrap = w
        b.editor.setWordWrap(w)
        toast(if (w) "Ajuste de línea: ON" else "Ajuste de línea: OFF")
    }

    private fun goToLineDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Nº de línea (1–${b.editor.lineCount()})"
            setPadding(dp(20), dp(12), dp(20), dp(12))
        }
        AlertDialog.Builder(this)
            .setTitle("Ir a línea")
            .setView(input)
            .setPositiveButton("Ir") { _, _ ->
                input.text?.toString()?.toIntOrNull()?.let { b.editor.goToLine(it) }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ── Buscar en el proyecto ─────────────────────────────────────────────

    private fun searchInProject(query: String): List<Hit> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val out = ArrayList<Hit>()
        val ql = q.lowercase(Locale.ROOT)
        val textExt = setOf(
            "js", "mjs", "cjs", "ts", "tsx", "jsx", "kt", "kts", "java", "py",
            "html", "htm", "css", "json", "md", "txt", "sh", "xml", "yml", "yaml", "gradle", "properties"
        )
        fun scan(name: String, entry: Entry, read: () -> String?) {
            val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
            if (ext !in textExt) return
            val content = runCatching { read() }.getOrNull() ?: return
            content.lineSequence().forEachIndexed { i, line ->
                if (out.size < 300 && line.lowercase(Locale.ROOT).contains(ql)) {
                    out.add(Hit(name, i + 1, line.trim().take(140), entry))
                }
            }
        }
        if (safMode) {
            fun walk(df: DocumentFile, depth: Int) {
                if (depth > 6 || out.size >= 300) return
                for (f in df.listFiles()) {
                    val n = f.name ?: continue
                    if (f.isDirectory) walk(f, depth + 1)
                    else scan(n, Entry(n, false, doc = f)) {
                        contentResolver.openInputStream(f.uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    }
                }
            }
            safStack.lastOrNull()?.let { walk(it, 0) }
        } else {
            fun walk(dir: File, depth: Int) {
                if (depth > 8 || out.size >= 300) return
                dir.listFiles()?.forEach { f ->
                    if (f.name.startsWith(".")) return@forEach
                    if (f.isDirectory) walk(f, depth + 1)
                    else scan(f.name, Entry(f.name, false, file = f)) {
                        if (f.length() < 1_000_000) f.readText() else null
                    }
                }
            }
            walk(internalStack.lastOrNull() ?: WorkspaceRepo.root(this), 0)
        }
        return out
    }

    private fun openSearchResult(hit: Hit) {
        val e = hit.entry
        if (e.doc != null) e.doc?.let { openDocument(it) } else e.file?.let { openFile(it) }
        b.editor.post { b.editor.goToLine(hit.line) }
        hideSheet()
    }

    // ── Operaciones de fichero ────────────────────────────────────────────

    private fun saveAs() {
        val d = currentDoc ?: return
        val input = EditText(this).apply {
            setText(if (d.name.contains('.')) "copia-${d.name}" else "${d.name}-copia.txt")
            setPadding(dp(20), dp(12), dp(20), dp(12))
        }
        AlertDialog.Builder(this)
            .setTitle("Guardar como (en ${pathLabel()})")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val name = input.text?.toString()?.trim().orEmpty()
                if (name.isEmpty()) return@setPositiveButton
                if (safMode) {
                    val dir = safStack.lastOrNull() ?: return@setPositiveButton
                    val created = runCatching { dir.createFile(mimeFor(name), name) }.getOrNull()
                    if (created == null) { toast("No se pudo crear"); return@setPositiveButton }
                    runCatching {
                        contentResolver.openOutputStream(created.uri, "wt")?.use { it.write(b.editor.text().toByteArray()) }
                    }
                    openDocument(created)
                } else {
                    val target = File(internalStack.lastOrNull() ?: WorkspaceRepo.root(this), name)
                    runCatching { target.writeText(b.editor.text()) }
                    openFile(target)
                }
                refreshExplorer()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun renameCurrent() {
        val d = currentDoc ?: return
        val input = EditText(this).apply { setText(d.name); setPadding(dp(20), dp(12), dp(20), dp(12)) }
        AlertDialog.Builder(this)
            .setTitle("Renombrar")
            .setView(input)
            .setPositiveButton("Renombrar") { _, _ ->
                val newName = input.text?.toString()?.trim().orEmpty()
                if (newName.isEmpty() || newName == d.name) return@setPositiveButton
                val uriNow = d.uri
                if (uriNow != null) {
                    val df = DocumentFile.fromSingleUri(this, uriNow)
                    val ok = runCatching { df?.renameTo(newName) ?: false }.getOrDefault(false)
                    if (ok) {
                        val moved = safStack.lastOrNull()?.listFiles()?.firstOrNull { it.name == newName }
                        d.name = newName
                        if (moved != null) d.uri = moved.uri
                        toast("Renombrado a $newName")
                    } else toast("No se pudo renombrar")
                } else {
                    val f = d.file
                    if (f != null) {
                        val nf = File(f.parentFile, newName)
                        val ok = runCatching { f.renameTo(nf) }.getOrDefault(false)
                        if (ok) {
                            d.file = nf
                            d.name = newName
                            settings.lastFile = nf.absolutePath
                            toast("Renombrado a $newName")
                        } else toast("No se pudo renombrar")
                    }
                }
                renderTabs(); updateTitle(); refreshExplorer()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteCurrent() {
        val d = currentDoc ?: return
        AlertDialog.Builder(this)
            .setTitle("Borrar ${d.name}")
            .setMessage("¿Seguro que quieres borrar \"${d.name}\"? No se puede deshacer.")
            .setPositiveButton("Borrar") { _, _ ->
                val duri = d.uri
                val ok = runCatching {
                    if (duri != null) DocumentFile.fromSingleUri(this, duri)?.delete() ?: false
                    else d.file?.delete() ?: false
                }.getOrDefault(false)
                if (ok) {
                    d.modified = false
                    closeTab(active)
                    refreshExplorer()
                    toast("Borrado")
                } else toast("No se pudo borrar")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ── Utilidades ────────────────────────────────────────────────────────

    // ── Preferencias del editor / sesión ──────────────────────────────────

    private fun applyEditorPrefs() {
        b.editor.setTheme(EditorThemes.byId(settings.editorTheme))
        b.editor.setFontSize(settings.editorFontSize.toFloat())
    }

    private fun persistSession() {
        val entries = docs.mapNotNull { d ->
            val f = d.file
            val u = d.uri
            when {
                f != null -> "file:${f.absolutePath}"
                u != null -> "uri:$u"
                else -> null
            }
        }
        settings.openTabs = entries.joinToString("\n")
        settings.activeTab = active.coerceAtLeast(0)
    }

    private fun restoreSession() {
        val stored = settings.openTabs.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        for (entry in stored) {
            runCatching {
                when {
                    entry.startsWith("file:") -> {
                        val f = File(entry.removePrefix("file:"))
                        if (f.isFile) openFile(f)
                    }
                    entry.startsWith("uri:") -> {
                        val uri = Uri.parse(entry.removePrefix("uri:"))
                        DocumentFile.fromSingleUri(this, uri)?.let { openDocument(it) }
                    }
                }
            }
        }
        if (docs.isEmpty()) {
            val root = WorkspaceRepo.root(this)
            val last = File(settings.lastFile)
            openFile(if (last.exists() && last.isFile) last else File(root, "main.js"))
        } else {
            switchTo(settings.activeTab.coerceIn(0, docs.size - 1))
        }
    }

    private fun showLanguageDialog() {
        val opts = arrayOf("Sistema", "Español", "English")
        val cur = when (settings.appLanguage) { "es" -> 1; "en" -> 2; else -> 0 }
        AlertDialog.Builder(this)
            .setTitle("Idioma")
            .setSingleChoiceItems(opts, cur) { dialog, which ->
                settings.appLanguage = when (which) { 1 -> "es"; 2 -> "en"; else -> "system" }
                dialog.dismiss()
                recreate()
            }
            .show()
    }

    private fun showAppThemeDialog() {
        val opts = arrayOf("Oscuro", "Claro", "Sistema")
        val cur = when (settings.appTheme) { "light" -> 1; "system" -> 2; else -> 0 }
        AlertDialog.Builder(this)
            .setTitle("Tema de la app")
            .setSingleChoiceItems(opts, cur) { dialog, which ->
                settings.appTheme = when (which) { 1 -> "light"; 2 -> "system"; else -> "dark" }
                dialog.dismiss()
                recreate()
            }
            .show()
    }

    private fun showThemeDialog() {
        val themes = EditorThemes.list
        val labels = themes.map { it.label }.toTypedArray()
        val current = themes.indexOfFirst { it.id == settings.editorTheme }.coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Tema del editor")
            .setSingleChoiceItems(labels, current) { dialog, which ->
                settings.editorTheme = themes[which].id
                applyEditorPrefs()
                dialog.dismiss()
            }
            .show()
    }

    private fun showFontSizeDialog() {
        val sizes = intArrayOf(10, 11, 12, 13, 14, 16, 18, 20, 22, 24)
        val labels = sizes.map { "${it}sp" }.toTypedArray()
        val current = sizes.indexOfFirst { it == settings.editorFontSize }.coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Tamaño de fuente")
            .setSingleChoiceItems(labels, current) { dialog, which ->
                settings.editorFontSize = sizes[which]
                applyEditorPrefs()
                dialog.dismiss()
            }
            .show()
    }

    // ── Snippets / formato / hover / referencias / renombrar ──────────────

    private fun showSnippets() {
        val snips = Snippets.forLanguage(b.editor.language)
        val labels = snips.map { it.label }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Insertar snippet · ${b.editor.language}")
            .setItems(labels) { _, which -> b.editor.insert(snips[which].code) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun formatDocument() {
        if (!b.editor.formatDocument()) toast("Formato no disponible para ${b.editor.language}")
        else toast("Documento formateado")
    }

    private fun showHover() {
        val word = b.editor.wordAtCursor()
        if (word.isNullOrBlank()) { toast("Coloca el cursor sobre un nombre"); return }
        val line = findDefinitionLine(b.editor.text(), word)
        if (line != null) {
            val code = b.editor.text().split('\n').getOrNull(line - 1)?.trim() ?: ""
            AlertDialog.Builder(this)
                .setTitle("\u2139 $word")
                .setMessage("Definición en este fichero · línea $line\n\n$code")
                .setPositiveButton("Ir") { _, _ -> b.editor.goToLine(line) }
                .setNegativeButton("Cerrar", null)
                .show()
            return
        }
        val hit = findDefinitionInProject(word)
        if (hit != null) {
            AlertDialog.Builder(this)
                .setTitle("\u2139 $word")
                .setMessage("Definición en ${hit.name} · línea ${hit.line}\n\n${hit.preview}")
                .setPositiveButton("Abrir") { _, _ -> openSearchResult(hit) }
                .setNegativeButton("Cerrar", null)
                .show()
        } else {
            toast("Sin información para «$word»")
        }
    }

    private fun showReferences() {
        val word = b.editor.wordAtCursor()
        if (word.isNullOrBlank()) { toast("Coloca el cursor sobre un nombre"); return }
        openSheet(SHEET_SEARCH)
        searchPanel?.setQuery(word, true)
    }

    private fun renameSymbol() {
        val word = b.editor.wordAtCursor()
        if (word.isNullOrBlank()) { toast("Coloca el cursor sobre un nombre"); return }
        val input = EditText(this).apply {
            setText(word)
            setPadding(dp(20), dp(12), dp(20), dp(12))
        }
        AlertDialog.Builder(this)
            .setTitle("Renombrar «$word»")
            .setMessage("Se renombran todas las apariciones en este documento.")
            .setView(input)
            .setPositiveButton("Renombrar") { _, _ ->
                val nn = input.text?.toString()?.trim().orEmpty()
                if (nn.isEmpty() || nn == word) return@setPositiveButton
                val t = b.editor.text()
                val rx = Regex("\\b${Regex.escape(word)}\\b")
                val count = rx.findAll(t).count()
                if (count == 0) { toast("Sin coincidencias"); return@setPositiveButton }
                b.editor.applyText(rx.replace(t, nn))
                toast("Renombradas $count apariciones")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ── Ir a definición (LSP-lite) ────────────────────────────────────────

    private fun goToDefinition() {
        val word = b.editor.wordAtCursor()
        if (word.isNullOrBlank()) { toast("Coloca el cursor sobre un nombre"); return }
        val line = findDefinitionLine(b.editor.text(), word)
        if (line != null) {
            b.editor.goToLine(line)
            toast("Definición de $word · línea $line")
            return
        }
        val hit = findDefinitionInProject(word)
        if (hit != null) {
            openSearchResult(hit)
            toast("Definición de $word en ${hit.name}")
        } else {
            toast("No se encontró definición de $word")
        }
    }

    private fun findDefinitionLine(text: String, word: String): Int? {
        val w = Regex.escape(word)
        val patterns = listOf(
            "\\b(function|class|def|fun|val|var|let|const|interface|struct|type|enum|object)\\s+$w\\b",
            "\\b$w\\s*=\\s*(function|\\()",
            "\\b$w\\s*:\\s*(function|\\()",
            "^\\s*$w\\s*\\("
        )
        text.lineSequence().forEachIndexed { i, l ->
            for (p in patterns) if (Regex(p).containsMatchIn(l)) return i + 1
        }
        return null
    }

    private fun findDefinitionInProject(word: String): Hit? {
        val textExt = setOf(
            "js", "mjs", "cjs", "ts", "tsx", "jsx", "kt", "kts", "java", "py",
            "html", "htm", "css", "json", "md", "sh", "xml"
        )
        if (safMode) {
            fun walk(df: DocumentFile, depth: Int): Hit? {
                if (depth > 6) return null
                for (f in df.listFiles()) {
                    val n = f.name ?: continue
                    if (f.isDirectory) {
                        walk(f, depth + 1)?.let { return it }
                    } else if (n.substringAfterLast('.', "").lowercase(Locale.ROOT) in textExt) {
                        val content = runCatching {
                            contentResolver.openInputStream(f.uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                        }.getOrNull() ?: continue
                        val line = findDefinitionLine(content, word)
                        if (line != null) {
                            val prev = content.lineSequence().elementAtOrNull(line - 1)?.trim()?.take(120) ?: ""
                            return Hit(n, line, prev, Entry(n, false, doc = f))
                        }
                    }
                }
                return null
            }
            return safStack.lastOrNull()?.let { walk(it, 0) }
        }
        fun walk(dir: File, depth: Int): Hit? {
            if (depth > 8) return null
            val children = dir.listFiles() ?: return null
            for (f in children) {
                if (f.name.startsWith(".")) continue
                if (f.isDirectory) {
                    walk(f, depth + 1)?.let { return it }
                } else if (f.extension.lowercase(Locale.ROOT) in textExt && f.length() < 1_000_000) {
                    val content = runCatching { f.readText() }.getOrNull() ?: continue
                    val line = findDefinitionLine(content, word)
                    if (line != null) {
                        val prev = content.lineSequence().elementAtOrNull(line - 1)?.trim()?.take(120) ?: ""
                        return Hit(f.name, line, prev, Entry(f.name, false, file = f))
                    }
                }
            }
            return null
        }
        return walk(internalStack.lastOrNull() ?: WorkspaceRepo.root(this), 0)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun isWide(): Boolean {
        val sc = resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        return sc >= Configuration.SCREENLAYOUT_SIZE_LARGE ||
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.isCtrlPressed) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_K -> { showPalette(); return true }
                KeyEvent.KEYCODE_S -> { saveCurrent(); toast("Guardado"); return true }
                KeyEvent.KEYCODE_P -> { openPreview(); return true }
                KeyEvent.KEYCODE_W -> { if (active >= 0) closeTab(active); return true }
                KeyEvent.KEYCODE_O -> { pickFolder.launch(null); return true }
                KeyEvent.KEYCODE_F -> { openSheet(SHEET_FIND); return true }
                KeyEvent.KEYCODE_Z -> { b.editor.undo(); return true }
                KeyEvent.KEYCODE_B -> { goToDefinition(); return true }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    @Deprecated("Compat con el botón atrás del sistema")
    override fun onBackPressed() {
        if (sheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN) hideSheet()
        else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    companion object {
        private const val SHEET_FILES = 1
        private const val SHEET_TERMINAL = 2
        private const val SHEET_GIT = 3
        private const val SHEET_AI = 4
        private const val SHEET_SETTINGS = 6
        private const val SHEET_FIND = 7
        private const val SHEET_SEARCH = 8
    }
}
