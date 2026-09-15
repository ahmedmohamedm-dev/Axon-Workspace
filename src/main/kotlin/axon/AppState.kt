package axon

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import javax.swing.Timer

class AppState {
    var workspace by mutableStateOf(Workspace())
    var revision by mutableStateOf(0)
    var selectedProjectId by mutableStateOf<String?>(null)
    var selectedNodeId by mutableStateOf<String?>(null)
    var selectedNodeIds by mutableStateOf<Set<String>>(emptySet())
    var visibleFlatIds by mutableStateOf<List<String>>(emptyList())
    var searchQuery by mutableStateOf("")
    var searchExpanded by mutableStateOf(false)
    var hideDone by mutableStateOf(false)
    var fileState by mutableStateOf("Local only")
    var dirty by mutableStateOf(false)
    var projectsCollapsed by mutableStateOf(false)
    var explorerWidth by mutableStateOf(390f)
    var workspaceFilePath by mutableStateOf<String?>(null)
    var workspaceFileLastModified by mutableStateOf(0L)
    var meta by mutableStateOf(Meta())

    data class UndoSnapshot(
        val json: String,
        val selectedNodeId: String?,
        val selectedNodeIds: Set<String>
    )

    private val undoStack = ArrayDeque<UndoSnapshot>()
    private val redoStack = ArrayDeque<UndoSnapshot>()
    private val maxUndo = 60

    var modalVisible by mutableStateOf(false)
    var modalTitle by mutableStateOf("")
    var modalButtonText by mutableStateOf("Create")
    var modalValue by mutableStateOf("")
    private var modalCallback: ((String) -> Unit)? = null

    var draggingNodeId by mutableStateOf<String?>(null)
    var draggingNodeIds by mutableStateOf<Set<String>>(emptySet())
    var dropTargetId by mutableStateOf<String?>(null)
    val rowBounds = mutableMapOf<String, Rect>()
    var renamingNodeId by mutableStateOf<String?>(null)
    var textFieldFocused by mutableStateOf(false)
    var toasts by mutableStateOf<List<ToastMessage>>(emptyList())
    var shortcutsEditorVisible by mutableStateOf(false)
    var hamburgerVisible by mutableStateOf(false)
    var contextMenuVisible by mutableStateOf(false)
    var contextMenuTargetId by mutableStateOf<String?>(null)
    var contextMenuX by mutableStateOf(0)
    var clipboard by mutableStateOf<List<Node>>(emptyList())
    var contextMenuY by mutableStateOf(0)

    private var saveTimer: Timer? = null

    init {
        val cached = Persistence.loadCache()
        if (cached != null && cached.projects.isNotEmpty()) {
            workspace = cached
            val validId = if (cached.projects.any { it.id == cached.activeId }) cached.activeId
            else cached.projects.firstOrNull()?.id
            selectedProjectId = validId
            workspace.activeId = validId
        } else {
            workspace = Workspace(mutableListOf(), null)
            selectedProjectId = null
        }
        meta = Persistence.loadMeta()
        Theme.apply(parseThemeMode(meta.themeMode))
        explorerWidth = meta.explorerWidth
        projectsCollapsed = meta.projectsCollapsed
        Timer(80) { restoreLinkedWorkspace() }.apply { isRepeats = false; start() }
    }

    fun currentProject(): Project? = workspace.projects.firstOrNull { it.id == selectedProjectId }
    fun currentTree(): Node? = currentProject()?.tree

    fun selectedNode(): Node? {
        val id = selectedNodeId ?: return null
        return TreeOps.findNode(currentTree() ?: return null, id)
    }

    fun selectOnly(id: String) {
        selectedNodeId = id
        selectedNodeIds = setOf(id)
    }

    fun toggleSelected(id: String) {
        val cur = selectedNodeIds
        val next = if (id in cur) cur - id else cur + id
        selectedNodeIds = next
        selectedNodeId = if (id in next) id else next.lastOrNull()
    }

    fun selectRangeTo(id: String) {
        val ids = visibleFlatIds
        val anchor = selectedNodeId ?: id
        val a = ids.indexOf(anchor)
        val b = ids.indexOf(id)
        if (a < 0 || b < 0) {
            selectOnly(id); return
        }
        val s = if (a <= b) a else b
        val e = if (a <= b) b else a
        selectedNodeIds = ids.subList(s, e + 1).toSet()
        selectedNodeId = id
    }

    fun clearSelection() {
        selectedNodeId = null
        selectedNodeIds = emptySet()
    }

    fun emptyProject(name: String): Project {
        val clean = name.trim().ifEmpty { "New Project" }
        return Project(
            name = clean,
            tree = Node(type = "folder", name = clean, open = true, children = mutableListOf())
        )
    }

    fun mutate(block: () -> Unit) {
        block()
        // Force Compose to notice workspace changes (new reference)
        workspace = workspace.copy()
        val p = currentProject()
        if (p != null && selectedNodeIds.isNotEmpty()) {
            val valid = selectedNodeIds.filter { TreeOps.findNode(p.tree, it) != null }.toSet()
            if (valid != selectedNodeIds) selectedNodeIds = valid
        }
        revision++
        onChanged()
    }

    private fun snapshotJson(): String =
        Persistence.json.encodeToString(Workspace.serializer(), workspace)

    fun snapshot() {
        undoStack.addLast(UndoSnapshot(snapshotJson(), selectedNodeId, selectedNodeIds))
        while (undoStack.size > maxUndo) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(UndoSnapshot(snapshotJson(), selectedNodeId, selectedNodeIds))
        val snap = undoStack.removeLast()
        val openStates = captureOpenStates()
        workspace = Persistence.json.decodeFromString(Workspace.serializer(), snap.json)
        restoreOpenStates(openStates)
        selectedProjectId =
            if (workspace.projects.any { it.id == workspace.activeId }) workspace.activeId else workspace.projects.firstOrNull()?.id
        restoreSelection(snap.selectedNodeId, snap.selectedNodeIds)
        revision++
        onChanged()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(UndoSnapshot(snapshotJson(), selectedNodeId, selectedNodeIds))
        val snap = redoStack.removeLast()
        val openStates = captureOpenStates()
        workspace = Persistence.json.decodeFromString(Workspace.serializer(), snap.json)
        restoreOpenStates(openStates)
        selectedProjectId =
            if (workspace.projects.any { it.id == workspace.activeId }) workspace.activeId else workspace.projects.firstOrNull()?.id
        restoreSelection(snap.selectedNodeId, snap.selectedNodeIds)
        revision++
        onChanged()
    }

    private fun captureOpenStates(): Map<String, Boolean> {
        val m = mutableMapOf<String, Boolean>()
        workspace.projects.forEach { p -> TreeOps.walk(p.tree) { n -> m[n.id] = n.open } }
        return m
    }

    private fun restoreOpenStates(map: Map<String, Boolean>) {
        workspace.projects.forEach { p ->
            TreeOps.walk(p.tree) { n -> map[n.id]?.let { n.open = it } }
        }
    }

    private fun restoreSelection(prevId: String?, prevIds: Set<String>) {
        val p = workspace.projects.firstOrNull { it.id == selectedProjectId }
        if (p == null) {
            selectedNodeId = null
            selectedNodeIds = emptySet()
            return
        }
        val valid = prevIds.filter { TreeOps.findNode(p.tree, it) != null }.toSet()
        selectedNodeIds = valid
        val anchor = if (prevId != null && TreeOps.findNode(p.tree, prevId) != null) prevId
        else valid.lastOrNull()
        selectedNodeId = anchor
        if (anchor != null) {
            expandAncestors(p.tree, anchor)
            val node = TreeOps.findNode(p.tree, anchor)
            if (node != null && node.isFolder() && node.children.isNotEmpty()) {
                node.open = true
            }
        }
    }

    private fun expandAncestors(root: Node, targetId: String): Boolean {
        if (root.id == targetId) return true
        for (child in root.children) {
            if (expandAncestors(child, targetId)) {
                if (root.isFolder()) root.open = true
                return true
            }
        }
        return false
    }

    private fun onChanged() {
        dirty = true
        persistLocalCache()
        fileState =
            if (workspaceFilePath != null) "Modified - local saved - sync pending" else "Local saved"
        scheduleSave()
    }

    private val persistExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "axon-persist").apply { isDaemon = true }
    }

    fun persistLocalCache() {
        workspace.activeId = selectedProjectId
        meta.localUpdatedAt = System.currentTimeMillis()
        val wsText = Persistence.json.encodeToString(Workspace.serializer(), workspace)
        val metaText = Persistence.json.encodeToString(Meta.serializer(), meta)
        persistExecutor.submit {
            Persistence.writeCacheRaw(wsText)
            Persistence.writeMetaRaw(metaText)
        }
    }

    fun cycleTheme() {
        val next = when (Theme.mode) {
            ThemeMode.Dark -> ThemeMode.Light
            ThemeMode.Light -> ThemeMode.Auto
            ThemeMode.Auto -> ThemeMode.Dark
        }
        Theme.apply(next)
        meta.themeMode = next.name.lowercase()
        Persistence.saveMeta(meta)
        revision++
        val label = when (next) {
            ThemeMode.Dark -> "Dark theme"
            ThemeMode.Light -> "Light theme"
            ThemeMode.Auto -> "Auto (follows Windows)"
        }
        toast(label, ToastType.Info, 1200L)
    }

    private fun parseThemeMode(s: String): ThemeMode =
        when (s.lowercase()) {
            "dark" -> ThemeMode.Dark
            "light" -> ThemeMode.Light
            else -> ThemeMode.Auto
        }

    fun forceSync() {
        val path = workspaceFilePath
        if (path == null) {
            toast("No JSON linked. Use Save JSON first.", ToastType.Warning)
            return
        }
        saveTimer?.stop()
        syncToJson(force = true)
        toast("Synced", ToastType.Success, 1500L)
    }

    fun markClean() {
        dirty = false
        meta.lastSyncedLocalUpdatedAt = meta.localUpdatedAt
        Persistence.saveMeta(meta)
    }

    private fun scheduleSave() {
        saveTimer?.stop()
        val path = workspaceFilePath ?: return
        saveTimer = Timer(700) { syncToJson(false) }.apply { isRepeats = false; start() }
    }

    fun openWorkspaceFileDialog() {
        if (dirty) {
            val ok = javax.swing.JOptionPane.showConfirmDialog(
                null, "You have local changes. Open another JSON?",
                "Confirm", javax.swing.JOptionPane.YES_NO_OPTION
            )
            if (ok != javax.swing.JOptionPane.YES_OPTION) return
        }
        val f = Dialogs.chooseOpenJson() ?: return
        if (f.name != "axon-workspace.json") {
            val ok = javax.swing.JOptionPane.showConfirmDialog(
                null, "For auto-linking, file must be named axon-workspace.json. Continue?",
                "Confirm", javax.swing.JOptionPane.YES_NO_OPTION
            )
            if (ok != javax.swing.JOptionPane.YES_OPTION) return
        }
        loadRemoteFile(f, force = true)
        workspaceFilePath = f.absolutePath
        meta.linkedFilePath = if (f.name == "axon-workspace.json") f.absolutePath else null
        Persistence.saveMeta(meta)
        fileState =
            if (f.name == "axon-workspace.json") "Linked - ${f.name}" else "Opened - ${f.name}"
    }

    fun saveWorkspaceFileDialog() {
        val appRoot = Persistence.appRootDir()
        val suggested = if (appRoot != null)
            File(appRoot, "axon-workspace.json")
        else
            File(System.getProperty("user.home"), "axon-workspace.json")
        val f = Dialogs.chooseSaveJson(suggested) ?: return
        workspace.activeId = selectedProjectId
        runCatching {
            f.writeText(Persistence.json.encodeToString(Workspace.serializer(), workspace))
        }.onFailure {
            javax.swing.JOptionPane.showMessageDialog(null, "Save failed: ${it.message}")
            return
        }
        workspaceFilePath = f.absolutePath
        workspaceFileLastModified = f.lastModified()
        meta.linkedFilePath = if (f.name == "axon-workspace.json") f.absolutePath else null
        meta.lastSyncedRemoteModified = workspaceFileLastModified
        meta.lastSyncedLocalUpdatedAt = meta.localUpdatedAt
        Persistence.saveMeta(meta)
        markClean()
        fileState = "Saved - ${f.name}"
        toast("Saved to ${f.name}", ToastType.Success)
    }

    fun importWorkspaceDialog() {
        try {
            val f = Dialogs.chooseOpenJson()
            if (f == null) {
                println("[Import] No file"); return
            }
            println("[Import] File: ${f.absolutePath}")
            var text = f.readText(Charsets.UTF_8)
            if (text.isNotEmpty() && text[0] == '\uFEFF') text = text.substring(1)
            text = text.trim()
            println("[Import] Length=${text.length}")

            val imported = tryParseAny(text)
            if (imported == null || imported.projects.isEmpty()) {
                val root = runCatching {
                    Persistence.json.parseToJsonElement(text)
                }.getOrNull()
                val info = when (root) {
                    is JsonObject -> "JsonObject keys: " + root.keys.joinToString(", ")
                    else -> "Not a JSON object: ${root?.javaClass?.simpleName}"
                }
                javax.swing.JOptionPane.showMessageDialog(
                    null, "Could not parse.\n$info\n\nFirst 200 chars:\n${text.take(200)}",
                    "Import Error", javax.swing.JOptionPane.ERROR_MESSAGE
                )
                return
            }

            snapshot()
            val existingIds = workspace.projects.map { it.id }.toMutableSet()
            val existingNames = workspace.projects.map { it.name.lowercase() }.toMutableSet()
            var added = 0
            for (p in imported.projects) {
                var newId = p.id
                if (newId in existingIds) newId = newId()
                var name = p.name
                if (name.lowercase() in existingNames) {
                    var i = 2
                    while ("$name $i".lowercase() in existingNames) i++
                    name = "$name $i"
                }
                existingIds += newId
                existingNames += name.lowercase()
                workspace.projects.add(Project(id = newId, name = name, tree = p.tree))
                added++
            }
            selectedProjectId = workspace.projects.last().id
            workspace.activeId = selectedProjectId
            selectedNodeId = null
            revision++
            onChanged()
            println("[Import] Added $added projects")
            toast("Imported $added project${if (added == 1) "" else "s"}", ToastType.Info)
        } catch (e: Exception) {
            e.printStackTrace()
            toast("Import failed: ${e.message ?: e.javaClass.simpleName}", ToastType.Error, 4000L)
        }
    }

    private fun tryParseAny(text: String): Workspace? {
        runCatching {
            val w = Persistence.json.decodeFromString(Workspace.serializer(), text)
            if (w.projects.isNotEmpty()) return w
        }

        val root = runCatching {
            Persistence.json.parseToJsonElement(text) as? JsonObject
        }.getOrNull() ?: return null

        root["projects"]?.let {
            return runCatching {
                Persistence.json.decodeFromString(Workspace.serializer(), text)
            }.getOrNull()
        }

        fun str(key: String): String? = (root[key] as? JsonPrimitive)?.content
        val nameKeys = listOf("name", "ide name", "ideName", "title")
        val treeKeys = listOf("tree", "root", "content")
        val idKeys = listOf("id", "_id", "uuid")

        val name = nameKeys.firstNotNullOfOrNull { str(it) }
        val id = idKeys.firstNotNullOfOrNull { str(it) } ?: newId()
        val treeEl = treeKeys.firstNotNullOfOrNull { root[it] } ?: return null
        val tree = runCatching {
            Persistence.json.decodeFromJsonElement(Node.serializer(), treeEl)
        }.getOrNull() ?: return null

        val proj = Project(id = id, name = name ?: tree.name.ifEmpty { "Imported" }, tree = tree)
        return Workspace(mutableListOf(proj), proj.id)
    }

    private fun loadRemoteFile(f: File, force: Boolean) {
        val text = runCatching { f.readText() }.getOrNull() ?: return
        val parsed = runCatching {
            Persistence.json.decodeFromString(Workspace.serializer(), text)
        }.getOrNull() ?: run { fileState = "JSON load failed"; return }
        if (!force && sharedChangesExist(f.lastModified())) {
            fileState = "Conflict - local changes kept"; return
        }
        workspace = parsed
        val validId = if (parsed.projects.any { it.id == parsed.activeId }) parsed.activeId
        else parsed.projects.firstOrNull()?.id
        selectedProjectId = validId
        workspace.activeId = validId
        selectedNodeId = null
        undoStack.clear(); redoStack.clear()
        workspaceFilePath = f.absolutePath
        workspaceFileLastModified = f.lastModified()
        persistLocalCache()
        markClean()
        revision++
        fileState = "Synced - ${f.name}"
    }

    private fun restoreLinkedWorkspace() {
        // 1) Try previously-linked file
        val path = meta.linkedFilePath
        if (path != null) {
            val f = File(path)
            if (f.exists() && f.name == "axon-workspace.json") {
                loadRemoteFile(f, force = false)
                return
            }
        }

        // 2) Auto-find axon-workspace.json in app root (portable mode)
        val appRoot = Persistence.appRootDir()
        if (appRoot != null) {
            val candidate = File(appRoot, "axon-workspace.json")
            if (candidate.exists()) {
                workspaceFilePath = candidate.absolutePath
                meta.linkedFilePath = candidate.absolutePath
                Persistence.saveMeta(meta)
                loadRemoteFile(candidate, force = false)
                println("[AutoLink] Found: ${candidate.absolutePath}")
                return
            }
        }

        // 3) Nothing found
        fileState = "Local only"
    }

    private fun sharedChangesExist(fileModified: Long): Boolean =
        fileModified > meta.lastSyncedRemoteModified &&
                meta.localUpdatedAt > meta.lastSyncedLocalUpdatedAt

    private fun syncToJson(force: Boolean) {
        saveTimer = null
        val path = workspaceFilePath ?: return
        val f = File(path)
        if (!force && f.exists() && workspaceFileLastModified != 0L &&
            f.lastModified() != workspaceFileLastModified
        ) {
            fileState = "Conflict - remote changed"; return
        }
        runCatching {
            workspace.activeId = selectedProjectId
            f.writeText(Persistence.json.encodeToString(Workspace.serializer(), workspace))
        }.onFailure { fileState = "Local saved - JSON sync unavailable"; return }
        workspaceFileLastModified = f.lastModified()
        meta.lastSyncedRemoteModified = workspaceFileLastModified
        meta.lastSyncedLocalUpdatedAt = meta.localUpdatedAt
        Persistence.saveMeta(meta)
        markClean()
        fileState = "Saved - ${f.name}"
    }

    fun saveWindowState(x: Int, y: Int, w: Int, h: Int) {
        if (x == meta.windowX && y == meta.windowY && w == meta.windowWidth && h == meta.windowHeight) return
        meta.windowX = x
        meta.windowY = y
        meta.windowWidth = w
        meta.windowHeight = h
        Persistence.saveMeta(meta)
    }

    fun saveUiState() {
        if (explorerWidth == meta.explorerWidth && projectsCollapsed == meta.projectsCollapsed) return
        meta.explorerWidth = explorerWidth
        meta.projectsCollapsed = projectsCollapsed
        Persistence.saveMeta(meta)
    }

    fun showModal(title: String, buttonText: String, initial: String = "", cb: (String) -> Unit) {
        modalTitle = title
        modalButtonText = buttonText
        modalValue = initial
        modalCallback = cb
        modalVisible = true
    }

    fun closeModal() {
        modalVisible = false; modalCallback = null
    }

    fun toast(text: String, type: ToastType = ToastType.Info, durationMs: Long = 2500L) {
        toasts = toasts + ToastMessage(text = text, type = type, durationMs = durationMs)
    }

    fun dismissToast(id: Long) {
        toasts = toasts.filter { it.id != id }
    }

    fun confirmModal() {
        val v = modalValue.trim()
        val cb = modalCallback
        closeModal()
        cb?.invoke(v)
    }

    fun copySelected() {
        val p = currentProject() ?: return
        val ids = selectedNodeIds.ifEmpty { setOfNotNull(selectedNodeId) }
        val nodes = ids
            .filter { it != p.tree.id }
            .mapNotNull { TreeOps.findNode(p.tree, it) }
        if (nodes.isEmpty()) return
        clipboard = nodes.map { deepCloneNode(it) }
        toast(
            "Copied ${clipboard.size} item${if (clipboard.size == 1) "" else "s"}",
            ToastType.Info
        )
    }

    fun pasteIntoSelection() {
        if (clipboard.isEmpty()) return
        val p = currentProject() ?: return
        val target = parentForPaste(p) ?: return
        snapshot()
        mutate {
            for (src in clipboard) {
                var clone = deepCloneNode(src).copy(id = newId())
                if (!TreeOps.canName(target, clone.name)) {
                    clone = clone.copy(name = TreeOps.uniqueName(target.children, clone.name))
                }
                clone = regenerateIds(clone)
                target.children.add(clone)
            }
            target.open = true
        }
        // Select the last pasted item
        val lastChild = target.children.lastOrNull()
        if (lastChild != null) selectOnly(lastChild.id)
        toast(
            "Pasted ${clipboard.size} item${if (clipboard.size == 1) "" else "s"}",
            ToastType.Success
        )
    }

    private fun parentForPaste(p: Project): Node? {
        val selId = selectedNodeId ?: return p.tree
        val sel = TreeOps.findNode(p.tree, selId) ?: return p.tree
        return when {
            sel.isFolder() -> sel
            else -> TreeOps.findParent(p.tree, sel.id) ?: p.tree
        }
    }

    private fun deepCloneNode(n: Node): Node {
        val clone = Node(
            id = n.id,
            type = n.type,
            name = n.name,
            description = n.description,
            status = n.status,
            open = n.open,
            children = n.children.map { deepCloneNode(it) }.toMutableList()
        )
        return clone
    }

    private fun regenerateIds(n: Node): Node {
        val newChildren = n.children.map { c ->
            regenerateIds(c.copy(id = newId()))
        }.toMutableList()
        return n.copy(children = newChildren)
    }

    fun getShortcut(action: String): String =
        meta.shortcuts[action] ?: defaultShortcuts()[action] ?: ""

    fun setShortcut(action: String, shortcut: String) {
        val cur = meta.shortcuts.toMutableMap()
        cur[action] = shortcut
        meta.shortcuts = cur
        Persistence.saveMeta(meta)
        revision++
        toast("Shortcut updated", ToastType.Info, 1200L)
    }

    fun resetShortcuts() {
        meta.shortcuts = defaultShortcuts()
        Persistence.saveMeta(meta)
        revision++
        toast("Shortcuts reset to defaults", ToastType.Info, 1500L)
    }

    fun selectAllNodes() {
        val p = currentProject() ?: return
        // Select all currently visible items (respect filters/search)
        val ids = visibleFlatIds.toSet()
        if (ids.isEmpty()) return
        selectedNodeIds = ids
        selectedNodeId = ids.lastOrNull()
        revision++
    }

    fun logNodeHistory(nodeId: String, action: String) {
        val p = currentProject() ?: return
        val n = TreeOps.findNode(p.tree, nodeId) ?: return
        n.history.add(HistoryEntry(action = action))
        if (n.history.size > 100) n.history.removeAt(0)
        revision++
    }

    fun hasClipboard(): Boolean = clipboard.isNotEmpty()

    fun moveMultipleTo(nodeIds: Set<String>, targetId: String): Boolean {
        val p = currentProject() ?: return false
        val target = TreeOps.findNode(p.tree, targetId) ?: return false
        if (!target.isFolder()) return false
        var anyMoved = false
        for (id in nodeIds) {
            if (id == targetId) continue
            if (id == p.tree.id) continue
            val node = TreeOps.findNode(p.tree, id) ?: continue
            if (TreeOps.contains(node, targetId)) continue
            val source = TreeOps.findParent(p.tree, id) ?: continue
            if (source.id == target.id) continue
            if (!TreeOps.canName(target, node.name)) {
                node.name = TreeOps.uniqueName(target.children, node.name)
            }
            source.children.remove(node)
            target.children.add(node)
            anyMoved = true
        }
        if (anyMoved) target.open = true
        return anyMoved
    }

    fun findRowAt(pos: Offset): String? =
        rowBounds.entries.firstOrNull { it.value.contains(pos) }?.key

    fun showContextMenuAtScreen(screenX: Int, screenY: Int, nodeId: String) {
        contextMenuTargetId = nodeId
        contextMenuX = screenX
        contextMenuY = screenY
        contextMenuVisible = true
    }
}
