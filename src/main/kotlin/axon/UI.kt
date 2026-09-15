package axon

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import java.awt.Cursor

@Composable
fun AxonApp(state: AppState) {
    val rootFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        // Small delay so window is ready, then grab focus
        delay(100)
        try {
            rootFocus.requestFocus()
        } catch (_: Exception) {
        }
    }
    Box(
        Modifier.fillMaxSize().background(Theme.bg)
            .focusRequester(rootFocus)
            .focusable()
            .onPreviewKeyEvent { ev -> handleGlobalKey(state, ev) }
    ) {
        Column(Modifier.fillMaxSize()) {
            TopBar(state)
            Row(Modifier.weight(1f).fillMaxWidth()) {
                ProjectsPanel(state)
                Row(Modifier.weight(1f).fillMaxHeight()) {
                    ExplorerPanel(state)
                    Splitter(state)
                    PreviewPanel(state, Modifier.weight(1f).fillMaxHeight())
                }
            }
            StatusBar(state)
        }
        if (state.modalVisible) ModalOverlay(state)
        if (state.shortcutsEditorVisible) ShortcutsEditor(state)
        HamburgerMenu(state)
        val proj = state.currentProject()
        if (proj != null) ContextMenuView(state, proj)
        ToastHost(state)
    }
}

private data class ParsedShortcut(
    val ctrl: Boolean,
    val shift: Boolean,
    val alt: Boolean,
    val key: Key
)

private fun keyFromName(name: String): Key? {
    return when (name.uppercase()) {
        "A" -> Key.A; "B" -> Key.B; "C" -> Key.C; "D" -> Key.D; "E" -> Key.E
        "F" -> Key.F; "G" -> Key.G; "H" -> Key.H; "I" -> Key.I; "J" -> Key.J
        "K" -> Key.K; "L" -> Key.L; "M" -> Key.M; "N" -> Key.N; "O" -> Key.O
        "P" -> Key.P; "Q" -> Key.Q; "R" -> Key.R; "S" -> Key.S; "T" -> Key.T
        "U" -> Key.U; "V" -> Key.V; "W" -> Key.W; "X" -> Key.X; "Y" -> Key.Y
        "Z" -> Key.Z
        "0" -> Key.Zero; "1" -> Key.One; "2" -> Key.Two; "3" -> Key.Three
        "4" -> Key.Four; "5" -> Key.Five; "6" -> Key.Six; "7" -> Key.Seven
        "8" -> Key.Eight; "9" -> Key.Nine
        "F1" -> Key.F1; "F2" -> Key.F2; "F3" -> Key.F3; "F4" -> Key.F4
        "F5" -> Key.F5; "F6" -> Key.F6; "F7" -> Key.F7; "F8" -> Key.F8
        "F9" -> Key.F9; "F10" -> Key.F10; "F11" -> Key.F11; "F12" -> Key.F12
        "DELETE", "DEL" -> Key.Delete
        "ENTER", "RETURN" -> Key.Enter
        "SPACE" -> Key.Spacebar
        "TAB" -> Key.Tab
        "ESCAPE", "ESC" -> Key.Escape
        "HOME" -> Key.MoveHome
        "END" -> Key.MoveEnd
        "INSERT" -> Key.Insert
        "PAGEUP" -> Key.PageUp
        "PAGEDOWN" -> Key.PageDown
        "UP" -> Key.DirectionUp
        "DOWN" -> Key.DirectionDown
        "LEFT" -> Key.DirectionLeft
        "RIGHT" -> Key.DirectionRight
        else -> null
    }
}

private fun parseShortcut(s: String): ParsedShortcut? {
    if (s.isBlank()) return null
    val parts = s.split("+").map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.isEmpty()) return null
    var ctrl = false
    var shift = false
    var alt = false
    val keyParts = mutableListOf<String>()
    for (p in parts) {
        when (p.lowercase()) {
            "ctrl", "control", "cmd", "meta", "command" -> ctrl = true
            "shift" -> shift = true
            "alt", "option" -> alt = true
            else -> keyParts.add(p)
        }
    }
    if (keyParts.size != 1) return null
    val key = keyFromName(keyParts[0]) ?: return null
    return ParsedShortcut(ctrl, shift, alt, key)
}

private fun matchesShortcut(ev: KeyEvent, sc: ParsedShortcut): Boolean {
    val ctrl = ev.isCtrlPressed || ev.isMetaPressed
    return ctrl == sc.ctrl &&
            ev.isShiftPressed == sc.shift &&
            ev.isAltPressed == sc.alt &&
            ev.key == sc.key
}

fun handleGlobalKey(state: AppState, ev: KeyEvent): Boolean {
    if (ev.type != KeyEventType.KeyDown) return false

    val inTextField = state.textFieldFocused || state.renamingNodeId != null
    val inModal = state.modalVisible || state.shortcutsEditorVisible

    // If typing in a text field: let the field handle ALL keys (Enter, Escape, arrows...)
    if (inTextField) return false
    if (inModal) return false

    // Global shortcuts
    if (true) {
        val shortcuts = state.meta.shortcuts
        val undoSc = parseShortcut(state.getShortcut("undo"))
        val redoSc = parseShortcut(state.getShortcut("redo"))
        val saveSc = parseShortcut(state.getShortcut("save"))
        val copySc = parseShortcut(state.getShortcut("copy"))
        val pasteSc = parseShortcut(state.getShortcut("paste"))
        val delSc = parseShortcut(state.getShortcut("delete"))
        val renSc = parseShortcut(state.getShortcut("rename"))
        val newSc = parseShortcut(state.getShortcut("newProject"))
        val impSc = parseShortcut(state.getShortcut("import"))

        if (undoSc != null && matchesShortcut(ev, undoSc)) {
            state.undo(); return true
        }
        if (redoSc != null && matchesShortcut(ev, redoSc)) {
            state.redo(); return true
        }
        // Keep Ctrl+Shift+Z as built-in redo for compatibility
        if (ev.isCtrlPressed && ev.isShiftPressed && ev.key == Key.Z) {
            state.redo(); return true
        }
        if (saveSc != null && matchesShortcut(ev, saveSc)) {
            state.saveWorkspaceFileDialog(); return true
        }
        if (copySc != null && matchesShortcut(ev, copySc) && state.selectedNodeId != null) {
            state.copySelected(); return true
        }
        if (pasteSc != null && matchesShortcut(ev, pasteSc) && state.hasClipboard()) {
            state.pasteIntoSelection(); return true
        }
        if (delSc != null && matchesShortcut(ev, delSc) && state.selectedNodeId != null) {
            deleteSelected(state); return true
        }
        if (renSc != null && matchesShortcut(ev, renSc) && state.selectedNodeId != null) {
            state.renamingNodeId = state.selectedNodeId; return true
        }
        if (newSc != null && matchesShortcut(ev, newSc)) {
            state.showModal("New Project", "Create") { name ->
                if (name.isBlank()) return@showModal
                val newName = TreeOps.uniqueName(
                    state.workspace.projects.map { Node(type = "folder", name = it.name) },
                    name
                )
                state.snapshot()
                val p = state.emptyProject(newName)
                state.mutate {
                    state.workspace.projects.add(p)
                    state.selectedProjectId = p.id
                    state.workspace.activeId = p.id
                    state.selectedNodeId = null
                }
            }
            return true
        }
        if (impSc != null && matchesShortcut(ev, impSc)) {
            state.importWorkspaceDialog(); return true
        }

        val selAllSc = parseShortcut(state.getShortcut("selectAll"))
        if (selAllSc != null && matchesShortcut(ev, selAllSc)) {
            state.selectAllNodes(); return true
        }
    }

    // Tree navigation
    val ids = state.visibleFlatIds
    val selected = state.selectedNodeId
    if (ev.key == Key.DirectionDown || ev.key == Key.DirectionUp) {
        println("[NAV] ids=${ids.size} selected=$selected key=${ev.key}")
    }

    if (selected == null) {
        when (ev.key) {
            Key.DirectionDown, Key.DirectionUp, Key.MoveHome, Key.MoveEnd -> {
                val target = if (ev.key == Key.MoveEnd) ids.lastOrNull() else ids.firstOrNull()
                if (target != null) {
                    state.selectOnly(target); return true
                }
            }

            else -> {}
        }
        return false
    }

    val idx = ids.indexOf(selected)
    val tree = state.currentTree() ?: return false
    val node = TreeOps.findNode(tree, selected) ?: return false

    return when (ev.key) {
        Key.DirectionDown -> {
            if (idx >= 0 && idx < ids.size - 1) state.selectOnly(ids[idx + 1])
            true
        }

        Key.DirectionUp -> {
            if (idx > 0) state.selectOnly(ids[idx - 1])
            true
        }

        Key.MoveHome -> {
            ids.firstOrNull()?.let { state.selectOnly(it) }; true
        }

        Key.MoveEnd -> {
            ids.lastOrNull()?.let { state.selectOnly(it) }; true
        }

        Key.DirectionRight -> {
            if (node.isFolder()) {
                if (!node.open && node.children.isNotEmpty()) state.mutate { node.open = true }
                else if (node.open && node.children.isNotEmpty()) state.selectOnly(node.children.first().id)
            }
            true
        }

        Key.DirectionLeft -> {
            if (node.isFolder() && node.open) state.mutate { node.open = false }
            else {
                val parent = TreeOps.findParent(tree, selected)
                if (parent != null) state.selectOnly(parent.id)
            }
            true
        }

        Key.Enter -> {
            if (node.isFolder() && node.children.isNotEmpty()) state.mutate {
                node.open = !node.open
            }
            true
        }

        else -> false
    }
}

@Composable
fun Btn(
    text: String,
    primary: Boolean = false,
    danger: Boolean = false,
    iconSize: Boolean = false,
    onClick: () -> Unit
) {
    var hovered by remember { mutableStateOf(false) }
    val bg = when {
        primary -> if (hovered) Theme.accentHover else Theme.accentPrimary
        hovered -> Theme.buttonHover
        else -> Theme.buttonBg
    }
    val borderCol = if (primary) Theme.accentPrimaryBorder else Theme.buttonBorder
    val txtColor = when {
        primary -> Theme.onPrimary
        danger -> Theme.danger
        else -> Theme.text
    }
    val fontSize = if (iconSize) 18.sp else 13.sp
    val hPad = if (iconSize) 12.dp else 16.dp
    val vPad = if (iconSize) 6.dp else 9.dp
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(20.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .onPointerEvent(PointerEventType.Enter) { hovered = true }
            .onPointerEvent(PointerEventType.Exit) { hovered = false }
            .clickable { onClick() }
            .padding(horizontal = hPad, vertical = vPad)
    ) {
        Text(
            text,
            color = txtColor,
            fontSize = fontSize,
            fontWeight = if (primary) FontWeight.Medium else null
        )
    }
}

@Composable
fun TopBar(state: AppState) {
    @Suppress("UNUSED_EXPRESSION") state.revision
    Row(
        Modifier.fillMaxWidth().height(56.dp).background(Theme.panel2).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "⚡ Axon Workspace",
            color = Theme.titleColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Spacer(Modifier.width(4.dp))
        Btn("＋ New Project", primary = true) {
            state.showModal("New Project", "Create") { name ->
                if (name.isBlank()) return@showModal
                val newName = TreeOps.uniqueName(
                    state.workspace.projects.map { Node(type = "folder", name = it.name) },
                    name
                )
                state.snapshot()
                val p = state.emptyProject(newName)
                state.mutate {
                    state.workspace.projects.add(p)
                    state.selectedProjectId = p.id
                    state.workspace.activeId = p.id
                    state.selectedNodeId = null
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Btn("↶", iconSize = true) { state.undo() }
        Btn("↷", iconSize = true) { state.redo() }
        SearchField(state)
        SyncBtn { state.forceSync() }
        Btn("☰", iconSize = true) { state.hamburgerVisible = true }
    }
}

@Composable
private fun SearchField(state: AppState) {
    if (!state.searchExpanded) {
        // Icon only
        var hovered by remember { mutableStateOf(false) }
        Box(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (hovered) Theme.buttonHover else Theme.buttonBg)
                .border(1.dp, Theme.buttonBorder, RoundedCornerShape(20.dp))
                .pointerHoverIcon(PointerIcon.Hand)
                .onPointerEvent(PointerEventType.Enter) { hovered = true }
                .onPointerEvent(PointerEventType.Exit) { hovered = false }
                .clickable { state.searchExpanded = true }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("🔍", color = Theme.text, fontSize = 16.sp)
        }
    } else {
        // Expanded search field with X button
        Row(
            Modifier
                .width(260.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Theme.buttonBg)
                .border(1.dp, Theme.borderSoft, RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔍", color = Theme.muted, fontSize = 14.sp)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (state.searchQuery.isEmpty()) {
                    Text("Search...", color = Theme.muted, fontSize = 13.sp)
                }
                val fr = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    delay(50)
                    try {
                        fr.requestFocus()
                    } catch (_: Exception) {
                    }
                }
                BasicTextField(
                    value = state.searchQuery,
                    onValueChange = { state.searchQuery = it; state.revision++ },
                    singleLine = true,
                    textStyle = TextStyle(color = Theme.text, fontSize = 13.sp),
                    cursorBrush = SolidColor(Theme.accent),
                    modifier = Modifier.fillMaxWidth().focusRequester(fr)
                )
            }
            Spacer(Modifier.width(8.dp))
            var closeHovered by remember { mutableStateOf(false) }
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (closeHovered) Theme.hover else Color.Transparent)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .onPointerEvent(PointerEventType.Enter) { closeHovered = true }
                    .onPointerEvent(PointerEventType.Exit) { closeHovered = false }
                    .clickable {
                        state.searchExpanded = false
                        state.searchQuery = ""
                        state.textFieldFocused = false
                        state.revision++
                    }
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = Theme.muted, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun ProjectsPanel(state: AppState) {
    @Suppress("UNUSED_EXPRESSION") state.revision
    val collapsed = state.projectsCollapsed
    val targetWidth = if (collapsed) 46.dp else 280.dp
    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = tween(durationMillis = 220),
        label = "projectsWidth"
    )
    Column(
        Modifier
            .width(animatedWidth)
            .fillMaxHeight()
            .background(Theme.panelDark)
            .border(width = 1.dp, color = Theme.borderSoft)
    ) {
        Row(
            Modifier.fillMaxWidth().height(42.dp)
                .padding(horizontal = if (collapsed) 0.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.SpaceBetween
        ) {
            if (!collapsed) Text(
                "Projects",
                color = Theme.text,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Box(
                Modifier.clip(RoundedCornerShape(4.dp)).pointerHoverIcon(PointerIcon.Hand)
                    .clickable { state.projectsCollapsed = !state.projectsCollapsed }
                    .padding(4.dp)
            ) { Text(if (collapsed) "▶" else "◀", color = Theme.text, fontSize = 12.sp) }
        }
        HorizontalDivider(Theme.line)
        if (!collapsed) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)) {
                if (state.workspace.projects.isEmpty()) {
                    Text(
                        "No projects yet.",
                        color = Theme.muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(18.dp)
                    )
                } else {
                    state.workspace.projects.forEach { p ->
                        key(p.id, p.name) {
                            ProjectRow(state, p, p.name)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectRow(state: AppState, p: Project, projectName: String) {
    val active = p.id == state.selectedProjectId
    var hovered by remember { mutableStateOf(false) }
    val bg = when {
        active -> Theme.selected; hovered -> Theme.hover; else -> Color.Transparent
    }
    val borderCol = if (active) Theme.selectedBorder else Color.Transparent
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(10.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .onPointerEvent(PointerEventType.Enter) { hovered = true }
            .onPointerEvent(PointerEventType.Exit) { hovered = false }
            .clickable {
                state.selectedProjectId = p.id
                state.workspace.activeId = p.id
                state.clearSelection()
                state.persistLocalCache()
                state.revision++
            }
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("📦", fontSize = 16.sp)
        Text(
            projectName, color = Theme.text, fontSize = 13.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (hovered) {
            Box(Modifier.clip(RoundedCornerShape(4.dp)).clickable {
                state.showModal("Rename Project", "Save", p.name) { newName ->
                    val v = newName.trim()
                    if (v.isEmpty() || v == p.name) return@showModal
                    if (state.workspace.projects.any { it.id != p.id && it.name.equals(v, true) }) {
                        state.toast("Name already exists.", ToastType.Warning)
                        return@showModal
                    }
                    state.snapshot()
                    state.mutate { p.name = v }
                }
            }.padding(3.dp)) { Text("✏️", fontSize = 11.sp) }
            Box(Modifier.clip(RoundedCornerShape(4.dp)).clickable {
                val ok = javax.swing.JOptionPane.showConfirmDialog(
                    null, "Delete project ${p.name}?", "Confirm",
                    javax.swing.JOptionPane.YES_NO_OPTION
                )
                if (ok != javax.swing.JOptionPane.YES_OPTION) return@clickable
                state.snapshot()
                state.mutate {
                    state.workspace.projects.remove(p)
                    val next = state.workspace.projects.firstOrNull()
                    state.selectedProjectId = next?.id
                    state.workspace.activeId = next?.id
                    state.selectedNodeId = null
                    state.selectedNodeIds = emptySet()
                }
                state.toast("Deleted ${p.name}", ToastType.Error)
            }.padding(3.dp)) { Text("🗑️", fontSize = 11.sp) }
        }
    }
}

@Composable
private fun HorizontalDivider(color: Color) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(color))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExplorerPanel(state: AppState) {
    @Suppress("UNUSED_EXPRESSION") state.revision
    Column(
        Modifier
            .width(state.explorerWidth.dp)
            .fillMaxHeight()
            .background(Theme.panel)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            val p = state.currentProject()
            Text(
                if (p != null) "${p.name} / ${p.tree.name}" else "No project selected",
                color = Color(0xFFBBBBBB), fontSize = 12.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            if (p != null) {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SmallBtn("Expand") { expandAll(state) }
                    SmallBtn("Collapse") { collapseAll(state) }
                    SmallBtn("+ Folder") { addFolder(state) }
                    SmallBtn("+ File") { addFile(state) }
                    SmallBtnToggle("Hide Done", state.hideDone) { state.hideDone = !state.hideDone }
                }
            }
        }
        HorizontalDivider(Theme.line)
        TreeView(state)
    }
}

@Composable
private fun SmallBtnToggle(text: String, active: Boolean, onClick: () -> Unit) {
    var hovered by remember { mutableStateOf(false) }
    val bg = when {
        active -> Theme.accentPrimary
        hovered -> Theme.buttonHover
        else -> Theme.buttonBg
    }
    val borderCol = if (active) Theme.accentPrimaryBorder else Theme.buttonBorder
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(10.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .onPointerEvent(PointerEventType.Enter) { hovered = true }
            .onPointerEvent(PointerEventType.Exit) { hovered = false }
            .clickable { onClick() }
            .padding(horizontal = 11.dp, vertical = 5.dp)
    ) {
        Text(
            text,
            color = if (active) Theme.onPrimary else Theme.text,
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun SmallBtn(text: String, onClick: () -> Unit) {
    var hovered by remember { mutableStateOf(false) }
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (hovered) Theme.buttonHover else Theme.buttonBg)
            .border(1.dp, Theme.buttonBorder, RoundedCornerShape(12.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .onPointerEvent(PointerEventType.Enter) { hovered = true }
            .onPointerEvent(PointerEventType.Exit) { hovered = false }
            .clickable { onClick() }
            .padding(horizontal = 11.dp, vertical = 5.dp)
    ) {
        Text(text, color = Theme.text, fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
private fun TreeView(state: AppState) {
    val project = state.currentProject()
    if (project == null) {
        EmptyState(
            emoji = "📂",
            title = "No project selected",
            subtitle = "Create a new project or open an existing JSON",
            actionLabel = "＋ New Project",
            onAction = {
                state.showModal("New Project", "Create") { name ->
                    if (name.isBlank()) return@showModal
                    val newName = TreeOps.uniqueName(
                        state.workspace.projects.map { Node(type = "folder", name = it.name) },
                        name
                    )
                    state.snapshot()
                    val p = state.emptyProject(newName)
                    state.mutate {
                        state.workspace.projects.add(p)
                        state.selectedProjectId = p.id
                        state.workspace.activeId = p.id
                        state.selectedNodeId = null
                    }
                }
            }
        )
        return
    }
    val items =
        remember(state.revision, state.searchQuery, state.selectedProjectId, state.hideDone) {
            flattenTree(project.tree, state.searchQuery, state.hideDone)
        }
    val listState = rememberLazyListState()
    val flatIds = remember(items) { items.map { it.node.id } }
    SideEffect {
        state.visibleFlatIds = flatIds
        // Prune selection to only what is currently visible
        val valid = state.selectedNodeIds.filter { it in flatIds }.toSet()
        if (valid != state.selectedNodeIds) {
            state.selectedNodeIds = valid
            if (state.selectedNodeId !in valid) {
                state.selectedNodeId = valid.lastOrNull()
            }
        }
    }
    LaunchedEffect(state.selectedNodeId) {
        val id = state.selectedNodeId ?: return@LaunchedEffect
        val idx = state.visibleFlatIds.indexOf(id)
        if (idx < 0) return@LaunchedEffect
        val first = listState.firstVisibleItemIndex
        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: first
        if (idx < first || idx > last) listState.animateScrollToItem(idx)
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val availWidth = maxWidth
        val maxDepth = remember(items) { items.maxOfOrNull { it.depth } ?: 0 }
        val maxNameLen = remember(items) { items.maxOfOrNull { it.node.name.length } ?: 20 }
        val contentWidth = ((maxDepth * 18) + (maxNameLen * 8) + 120).dp
        val effectiveWidth = if (contentWidth > availWidth) contentWidth else availWidth
        val hScroll = rememberScrollState()
        val showBar = contentWidth > availWidth

        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize()
                    .padding(bottom = if (showBar) 10.dp else 0.dp)
                    .horizontalScroll(hScroll)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.width(effectiveWidth).padding(8.dp)
                ) {
                    items(
                        items = items,
                        key = { it.node.id }
                    ) { item ->
                        Box(Modifier.animateItem()) {
                            TreeRow(
                                state = state,
                                node = item.node,
                                depth = item.depth,
                                project = project,
                                isSelected = item.node.id in state.selectedNodeIds,
                                isDropTarget = item.node.id == state.dropTargetId,
                                nodeStatus = item.node.status,
                                nodeName = item.node.name
                            )
                        }
                    }
                }
            }
            if (showBar) {
                androidx.compose.foundation.HorizontalScrollbar(
                    adapter = androidx.compose.foundation.rememberScrollbarAdapter(hScroll),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(10.dp)
                )
            }
        }
    }
}

data class FlatItem(val node: Node, val depth: Int)

private fun flattenTree(root: Node, query: String, hideDone: Boolean): List<FlatItem> {
    val out = mutableListOf<FlatItem>()

    fun keep(n: Node): Boolean {
        return !(hideDone && n.status == "done")
    }

    fun visit(n: Node, depth: Int) {
        if (!keep(n)) return
        if (query.isNotEmpty() && !TreeOps.matchesSearch(n, query)) return
        out.add(FlatItem(n, depth))
        if (n.isFolder() && (n.open || query.isNotEmpty())) {
            n.children.sortedWith(
                compareBy(
                    { if (it.isFolder()) 0 else 1 },
                    { it.name.lowercase() })
            )
                .forEach { visit(it, depth + 1) }
        }
    }
    visit(root, 0)
    return out
}

@Composable
private fun TreeRow(
    state: AppState,
    node: Node,
    depth: Int,
    project: Project,
    isSelected: Boolean,
    isDropTarget: Boolean,
    nodeStatus: String,
    nodeName: String
) {
    var hovered by remember { mutableStateOf(false) }
    val bg = when {
        isDropTarget -> Theme.dragTargetBg
        isSelected -> Theme.selected
        hovered -> Theme.hover
        else -> Color.Transparent
    }

    Box(modifier = Modifier.fillMaxWidth().height(23.dp)) {
        // Indent guide lines
        for (k in 0 until depth) {
            Box(
                Modifier
                    .offset(x = (13 + 10 * k).dp)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF3F3F46))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(3.dp))
                .background(bg)
                .then(
                    if (isDropTarget) Modifier.border(
                        1.dp,
                        Theme.dragTarget,
                        RoundedCornerShape(3.dp)
                    ) else Modifier
                )
                .onGloballyPositioned { state.rowBounds[node.id] = it.boundsInWindow() }
                .onPointerEvent(PointerEventType.Enter) { hovered = true }
                .onPointerEvent(PointerEventType.Exit) { hovered = false }
                .pointerInput(node.id) {
                    awaitPointerEventScope {
                        while (true) {
                            val ev = awaitPointerEvent()
                            if (ev.type == PointerEventType.Press && ev.buttons.isSecondaryPressed) {
                                val localPos = ev.changes.firstOrNull()?.position ?: continue
                                ev.changes.forEach { it.consume() }
                                val rowOrigin = state.rowBounds[node.id]?.topLeft
                                    ?: androidx.compose.ui.geometry.Offset.Zero
                                val winPos = rowOrigin + localPos
                                val n = node
                                val p = project
                                val x = winPos.x.toInt()
                                val y = winPos.y.toInt()
                                java.awt.EventQueue.invokeLater {
                                    showTreeContextMenu(state, n, p, x, y)
                                }
                            }
                        }
                    }
                }
                .pointerInput(node.id) {
                    var lastTapTime = 0L
                    awaitEachGesture {
                        val downEvent = awaitPointerEvent()
                        if (downEvent.type != PointerEventType.Press) return@awaitEachGesture
                        if (!downEvent.buttons.isPrimaryPressed) return@awaitEachGesture
                        val down = downEvent.changes.firstOrNull() ?: return@awaitEachGesture
                        if (down.isConsumed) return@awaitEachGesture
                        val ctrl = downEvent.keyboardModifiers.isCtrlPressed
                        val shift = downEvent.keyboardModifiers.isShiftPressed
                        var up = false
                        while (true) {
                            val ev = awaitPointerEvent()
                            val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                            if (ch.isConsumed) return@awaitEachGesture
                            if (!ch.pressed) {
                                up = true; break
                            }
                        }
                        if (!up) return@awaitEachGesture
                        state.textFieldFocused = false
                        when {
                            ctrl -> state.toggleSelected(node.id)
                            shift -> state.selectRangeTo(node.id)
                            else -> state.selectOnly(node.id)
                        }
                        val now = System.currentTimeMillis()
                        val dt = now - lastTapTime
                        if (dt in 1L..400L) {
                            if (node.isFolder() && node.children.isNotEmpty()) {
                                state.mutate { node.open = !node.open }
                            }
                            lastTapTime = 0L
                        } else {
                            lastTapTime = now
                        }
                    }
                }
                .pointerHoverIcon(if (node.id == project.tree.id) PointerIcon.Default else PointerIcon.Hand)
                .padding(horizontal = 4.dp)
                .padding(start = (10 * depth).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Twisty
            Box(Modifier.width(18.dp).height(23.dp), contentAlignment = Alignment.Center) {
                if (node.isFolder() && node.children.isNotEmpty()) {
                    Box(
                        Modifier.fillMaxSize().clickable {
                            state.mutate { node.open = !node.open }
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (node.open) "▾" else "▸",
                            color = Color(0xFFAAAAAA),
                            fontSize = 11.sp
                        )
                    }
                }
            }
            IconBox(state, node, project)
            NameBox(state, node, project, nodeName, Modifier.weight(1f))
            Box(
                Modifier.width(24.dp).height(27.dp)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .pointerInput(node.id) {
                        awaitEachGesture {
                            // Get down event on Initial pass (before parent row tap)
                            val downEvent = awaitPointerEvent(PointerEventPass.Initial)
                            if (downEvent.type != PointerEventType.Press) return@awaitEachGesture
                            if (!downEvent.buttons.isPrimaryPressed) return@awaitEachGesture
                            val down = downEvent.changes.firstOrNull() ?: return@awaitEachGesture
                            // Immediately consume so parent tap gesture doesn't see it
                            down.consume()

                            // Wait for release
                            while (true) {
                                val ev = awaitPointerEvent(PointerEventPass.Initial)
                                val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                                ch.consume()
                                if (!ch.pressed) break
                            }

                            // Fire toggle
                            state.snapshot()
                            val sel = state.selectedNodeIds
                            val tree = state.currentTree()
                            val targets: List<Node> =
                                if (node.id in sel && sel.size > 1 && tree != null) {
                                    sel.mapNotNull { TreeOps.findNode(tree, it) }
                                } else listOf(node)
                            state.mutate {
                                for (t in targets) {
                                    t.status = when (t.status) {
                                        "" -> "progress"
                                        "progress" -> "done"
                                        else -> ""
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(statusIcon(nodeStatus), fontSize = 14.sp)
            }
        }

        if (nodeStatus == "done") {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.align(Alignment.CenterStart).fillMaxWidth().height(1.dp)
                        .background(Theme.doneLine.copy(alpha = 0.72f))
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun IconBox(state: AppState, node: Node, project: Project) {
    val isRoot = node.id == project.tree.id
    var iconBounds by remember { mutableStateOf(Rect.Zero) }

    val dragModifier = if (isRoot) {
        Modifier
    } else {
        Modifier
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
            .onGloballyPositioned { iconBounds = it.boundsInWindow() }
            .pointerInput(node.id) {
                awaitPointerEventScope {
                    while (true) {
                        val downEvent = awaitPointerEvent(PointerEventPass.Initial)
                        if (downEvent.type != PointerEventType.Press) continue
                        if (!downEvent.buttons.isPrimaryPressed) continue
                        val down = downEvent.changes.firstOrNull() ?: continue
                        if (!down.pressed) continue

                        // Decide drag set: multi if part of selection, else single
                        val sel = state.selectedNodeIds
                        val dragSet: Set<String> =
                            if (node.id in sel && sel.size > 1) sel else setOf(node.id)

                        var isDragging = false
                        val startPos = down.position

                        while (true) {
                            val ev = awaitPointerEvent(PointerEventPass.Initial)
                            val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                            if (!ch.pressed) break

                            if (!isDragging) {
                                val dist = (ch.position - startPos).getDistance()
                                if (dist > 4f) {
                                    isDragging = true
                                    state.draggingNodeId = node.id
                                    state.draggingNodeIds = dragSet
                                }
                            }
                            if (isDragging) {
                                ch.consume()
                                val winPos = iconBounds.topLeft + ch.position
                                val found = state.findRowAt(winPos)
                                val target = found ?: state.currentTree()?.id
                                state.dropTargetId = if (target == node.id) null else target
                            }
                        }

                        if (isDragging) {
                            val target = state.dropTargetId
                            if (target != null) {
                                val root = state.currentTree()
                                if (root != null) {
                                    state.snapshot()
                                    val moved = when {
                                        dragSet.size > 1 -> state.moveMultipleTo(dragSet, target)
                                        target == root.id -> moveToRoot(root, node.id)
                                        else -> TreeOps.moveNode(root, node.id, target)
                                    }
                                    if (!moved) state.undo()
                                    else {
                                        state.selectedNodeId = node.id
                                        state.revision++
                                    }
                                }
                            }
                            state.draggingNodeId = null
                            state.draggingNodeIds = emptySet()
                            state.dropTargetId = null
                        }
                    }
                }
            }
    }

    Box(
        dragModifier.width(22.dp).height(23.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(icon(node), fontSize = 16.sp)
    }
}

private fun showTreeContextMenu(state: AppState, node: Node, project: Project, x: Int, y: Int) {
    // If node is part of the current multi-selection, keep it; otherwise select only it
    if (node.id !in state.selectedNodeIds) {
        state.selectOnly(node.id)
    } else {
        state.selectedNodeId = node.id
    }
    state.showContextMenuAtScreen(x, y, node.id)
}

@Composable
private fun NameBox(
    state: AppState,
    node: Node,
    project: Project,
    nodeName: String,
    modifier: Modifier = Modifier
) {
    val editing = state.renamingNodeId == node.id
    if (editing) {
        val fr = remember { FocusRequester() }
        var text by remember(node.id) { mutableStateOf(node.name) }
        LaunchedEffect(Unit) { fr.requestFocus() }
        Box(
            modifier.height(22.dp)
                .background(Color(0xFF111111), RoundedCornerShape(2.dp))
                .border(1.dp, Theme.accent, RoundedCornerShape(2.dp))
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = text, onValueChange = { text = it }, singleLine = true,
                textStyle = TextStyle(
                    color = Theme.text,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(Theme.accent),
                modifier = Modifier.fillMaxWidth().focusRequester(fr)
                    .onPreviewKeyEvent { ev ->
                        if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (ev.key) {
                            Key.Enter -> {
                                commitRename(state, node, project, text); true
                            }

                            Key.Escape -> {
                                state.renamingNodeId = null; true
                            }

                            else -> false
                        }
                    }
            )
        }
    } else {
        Text(
            text = nodeName,
            modifier = modifier.padding(horizontal = 3.dp),
            color = if (node.isFolder()) Theme.folderText else Theme.fileText,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun commitRename(state: AppState, node: Node, project: Project, newName: String) {
    val v = newName.trim()
    if (v.isEmpty() || v == node.name) {
        state.renamingNodeId = null; return
    }
    val parent = if (node.id == project.tree.id) null else TreeOps.findParent(project.tree, node.id)
    if (parent != null && !TreeOps.canName(parent, v, node.id)) {
        state.toast("An item with this name already exists here.", ToastType.Warning)
        state.renamingNodeId = null
        return
    }
    state.snapshot()
    state.mutate { node.name = v }
    state.renamingNodeId = null
    state.toast("Renamed to $v", ToastType.Info)
}

private fun moveToRoot(root: Node, nodeId: String): Boolean {
    if (root.id == nodeId) return false
    val node = TreeOps.findNode(root, nodeId) ?: return false
    if (node.id == root.id) return false
    // Already a direct child of root?
    if (root.children.any { it.id == nodeId }) return false
    // Find source parent
    val source = TreeOps.findParent(root, nodeId) ?: return false
    if (source.id == root.id) return false
    // Rename if conflict
    if (!TreeOps.canName(root, node.name)) {
        node.name = TreeOps.uniqueName(root.children, node.name)
    }
    source.children.remove(node)
    root.children.add(node)
    root.open = true
    return true
}

private fun icon(n: Node) = if (n.isFolder()) (if (n.open) "📂" else "📁") else "📄"
private fun statusIcon(s: String) = when (s) {
    "done" -> "✅"; "progress" -> "🟡"; else -> "⬜"
}

private fun parentForNewItem(state: AppState): Node? {
    val p = state.currentProject() ?: return null
    val selId = state.selectedNodeId
    val sel = if (selId != null) TreeOps.findNode(p.tree, selId) else null
    return when {
        sel == null -> p.tree
        sel.isFolder() -> sel
        else -> TreeOps.findParent(p.tree, sel.id) ?: p.tree
    }
}

private fun addFolder(state: AppState) {
    val parent = parentForNewItem(state) ?: return
    state.snapshot()
    var newId: String? = null
    state.mutate {
        val child = Node(
            type = "folder",
            name = TreeOps.uniqueName(parent.children, "NewFolder"),
            open = true
        )
        parent.children.add(child)
        parent.open = true
        newId = child.id
    }
    newId?.let { state.selectOnly(it) }
    state.toast("Added folder", ToastType.Success)
}

private fun addFile(state: AppState) {
    val parent = parentForNewItem(state) ?: return
    state.snapshot()
    var newId: String? = null
    state.mutate {
        val child = Node(type = "file", name = TreeOps.uniqueName(parent.children, "NewFile.kt"))
        parent.children.add(child)
        parent.open = true
        newId = child.id
    }
    newId?.let { state.selectOnly(it) }
    state.toast("Added file", ToastType.Success)
}

private fun expandAll(state: AppState) {
    val p = state.currentProject() ?: return
    state.mutate { TreeOps.walk(p.tree) { if (it.isFolder()) it.open = true } }
}

private fun collapseAll(state: AppState) {
    val p = state.currentProject() ?: return
    state.mutate {
        TreeOps.walk(p.tree) { if (it.isFolder()) it.open = false }
        p.tree.open = true
    }
}

private fun deleteSelected(state: AppState) {
    val p = state.currentProject() ?: return
    val ids = state.selectedNodeIds.ifEmpty { setOfNotNull(state.selectedNodeId) }
    val nodes = ids
        .filter { it != p.tree.id }
        .mapNotNull { TreeOps.findNode(p.tree, it) }
    if (nodes.isEmpty()) return
    val savedNames = nodes.map { it.name }
    val msg = if (nodes.size == 1) "Delete ${savedNames[0]}?"
    else "Delete ${nodes.size} items?"
    val ok = javax.swing.JOptionPane.showConfirmDialog(
        null, msg, "Confirm", javax.swing.JOptionPane.YES_NO_OPTION
    )
    if (ok != javax.swing.JOptionPane.YES_OPTION) return
    val idList = nodes.map { it.id }
    state.snapshot()
    state.mutate {
        for (id in idList) {
            val n = TreeOps.findNode(p.tree, id) ?: continue
            val parent = TreeOps.findParent(p.tree, id) ?: continue
            parent.children.remove(n)
        }
        state.selectedNodeIds = emptySet()
        state.selectedNodeId = null
    }
    val okMsg =
        if (savedNames.size == 1) "Deleted ${savedNames[0]}" else "Deleted ${savedNames.size} items"
    state.toast(okMsg, ToastType.Error)
}

@Composable
private fun Splitter(state: AppState) {
    var dragging by remember { mutableStateOf(false) }
    Box(
        Modifier.width(4.dp).fillMaxHeight()
            .background(if (dragging) Theme.accent else Theme.line.copy(alpha = 0.4f))
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDrag = { change, delta ->
                        change.consume()
                        state.explorerWidth = (state.explorerWidth + delta.x).coerceIn(280f, 700f)
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false }
                )
            }
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { state.explorerWidth = 390f }) }
    )
}

@Composable
fun PreviewPanel(state: AppState, modifier: Modifier = Modifier) {
    @Suppress("UNUSED_EXPRESSION") state.revision
    val project = state.currentProject()
    val node = state.selectedNode()
    Column(modifier.background(Theme.bg)) {
        if (project != null) {
            Breadcrumb(state, project, node)
        } else {
            Box(
                Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    "Select a file or folder",
                    color = Color(0xFFAAAAAA),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        HorizontalDivider(Theme.line)
        when {
            project == null -> EmptyState(
                emoji = "📂",
                title = "No project selected",
                subtitle = "Create a new project to get started",
                actionLabel = "＋ New Project",
                onAction = {
                    state.showModal("New Project", "Create") { name ->
                        if (name.isBlank()) return@showModal
                        val newName = TreeOps.uniqueName(
                            state.workspace.projects.map { Node(type = "folder", name = it.name) },
                            name
                        )
                        state.snapshot()
                        val p = state.emptyProject(newName)
                        state.mutate {
                            state.workspace.projects.add(p)
                            state.selectedProjectId = p.id
                            state.workspace.activeId = p.id
                            state.selectedNodeId = null
                        }
                    }
                }
            )

            node == null -> EmptyState(
                emoji = "👈",
                title = "Nothing selected",
                subtitle = "Pick a file or folder from the explorer to see its details"
            )

            else -> PreviewForm(state, node, project)
        }
    }
}

@Composable
private fun Welcome(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = Color(0xFF666666), fontSize = 14.sp)
    }
}

@Composable
private fun PreviewForm(state: AppState, node: Node, project: Project) {
    var activeTab by remember(node.id) { mutableStateOf(0) } // 0=Details, 1=Notes, 2=History

    Column(Modifier.fillMaxSize().padding(0.dp)) {
        // Header
        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 20.dp)) {
            Text(
                node.name,
                color = Theme.textBright,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (node.isFolder()) "Folder" else "File",
                color = Color(0xFF888888),
                fontSize = 12.sp
            )
        }

        // Tabs row
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PreviewTabButton("Details", activeTab == 0) { activeTab = 0 }
            PreviewTabButton("Notes", activeTab == 1) { activeTab = 1 }
            PreviewTabButton(
                "History" + if (node.history.isNotEmpty()) " (${node.history.size})" else "",
                activeTab == 2
            ) { activeTab = 2 }
        }
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(Theme.line)

        // Content
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            when (activeTab) {
                0 -> DetailsTab(state, node, project)
                1 -> NotesTab(state, node)
                2 -> HistoryTab(node)
            }
        }
    }
}

@Composable
private fun PreviewTabButton(label: String, active: Boolean, onClick: () -> Unit) {
    var hovered by remember { mutableStateOf(false) }
    Box(
        Modifier
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
            .background(if (active) Theme.panel else if (hovered) Theme.hover else Color.Transparent)
            .border(
                1.dp,
                if (active) Theme.border else Color.Transparent,
                RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
            )
            .pointerHoverIcon(PointerIcon.Hand)
            .onPointerEvent(PointerEventType.Enter) { hovered = true }
            .onPointerEvent(PointerEventType.Exit) { hovered = false }
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            color = if (active) Theme.text else Theme.muted,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.Medium else null
        )
    }
}

@Composable
private fun DetailsTab(state: AppState, node: Node, project: Project) {
    var nameField by remember(node.id) { mutableStateOf(node.name) }
    var statusField by remember(node.id) { mutableStateOf(node.status) }
    var descField by remember(node.id) { mutableStateOf(node.description) }
    var descLastSaved by remember(node.id) { mutableStateOf(node.description) }
    var descSnapTaken by remember(node.id) { mutableStateOf(false) }

    LaunchedEffect(descField, node.id) {
        if (descField != descLastSaved) {
            delay(250)
            if (descField != descLastSaved) {
                node.description = descField
                descLastSaved = descField
                descSnapTaken = false
                state.mutate { }
                state.logNodeHistory(node.id, "Description updated")
            }
        }
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 20.dp)) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(Modifier.width(120.dp).padding(top = 10.dp)) {
                Text(
                    "Name",
                    color = Theme.muted,
                    fontSize = 14.sp
                )
            }
            Box(Modifier.weight(1f)) { Field(state, nameField, { nameField = it }) }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(Modifier.width(120.dp).padding(top = 10.dp)) {
                Text(
                    "Status",
                    color = Theme.muted,
                    fontSize = 14.sp
                )
            }
            Box(Modifier.weight(1f)) {
                StatusSelect(statusField) { newStatus ->
                    state.snapshot()
                    val old = node.status
                    node.status = newStatus
                    statusField = newStatus
                    state.logNodeHistory(
                        node.id,
                        "Status: ${statusLabel(old)} → ${statusLabel(newStatus)}"
                    )
                    state.mutate { }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(Modifier.width(120.dp).padding(top = 10.dp)) {
                Text(
                    "Description",
                    color = Theme.muted,
                    fontSize = 14.sp
                )
            }
            Box(Modifier.weight(1f)) {
                Field(
                    state = state,
                    value = descField,
                    onValueChange = { newVal ->
                        if (!descSnapTaken) {
                            state.snapshot()
                            descSnapTaken = true
                        }
                        descField = newVal
                    },
                    placeholder = "Write a description...",
                    singleLine = false,
                    minHeight = 150.dp
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn("Save Changes", primary = true) {
                val v = nameField.trim()
                if (v.isEmpty()) return@Btn
                val parent = TreeOps.findParent(project.tree, node.id)
                if (parent != null && !TreeOps.canName(parent, v, node.id)) {
                    state.toast("An item with this name already exists here.", ToastType.Warning)
                    return@Btn
                }
                val oldName = node.name
                state.snapshot()
                state.mutate {
                    node.name = v; node.status = statusField; node.description = descField
                }
                if (oldName != v) state.logNodeHistory(node.id, "Renamed: $oldName → $v")
                descLastSaved = descField
                state.toast("Changes saved", ToastType.Info)
            }
            if (node.id != project.tree.id) Btn("🗑 Delete", danger = true) { deleteSelected(state) }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Changes save locally immediately and sync to JSON when linked.",
            color = Color(0xFF777777), fontSize = 11.sp
        )
    }
}

@Composable
private fun NotesTab(state: AppState, node: Node) {
    var notesField by remember(node.id) { mutableStateOf(node.notes) }
    var lastSaved by remember(node.id) { mutableStateOf(node.notes) }
    var snapTaken by remember(node.id) { mutableStateOf(false) }

    LaunchedEffect(notesField, node.id) {
        if (notesField != lastSaved) {
            delay(300)
            if (notesField != lastSaved) {
                node.notes = notesField
                lastSaved = notesField
                snapTaken = false
                state.mutate { }
            }
        }
    }

    Column(Modifier.fillMaxWidth().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📝  Notes", color = Theme.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text(
                if (notesField.isEmpty()) "Empty" else "${notesField.length} chars",
                color = Theme.muted, fontSize = 11.sp
            )
        }
        Spacer(Modifier.height(10.dp))
        Field(
            state = state,
            value = notesField,
            onValueChange = { newVal ->
                if (!snapTaken) {
                    state.snapshot()
                    snapTaken = true
                }
                notesField = newVal
            },
            placeholder = "Write notes, TODOs, references...",
            singleLine = false,
            minHeight = 320.dp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Notes auto-save after 300ms.",
            color = Color(0xFF777777), fontSize = 11.sp
        )
    }
}

@Composable
private fun HistoryTab(node: Node) {
    Column(Modifier.fillMaxWidth().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🕐  History", color = Theme.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text("${node.history.size} entries", color = Theme.muted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(14.dp))
        if (node.history.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No history yet. Actions will appear here.",
                    color = Theme.muted, fontSize = 12.sp
                )
            }
        } else {
            // Show newest first
            node.history.reversed().forEach { entry ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        Modifier
                            .padding(top = 6.dp)
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Theme.accent)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(entry.action, color = Theme.text, fontSize = 13.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            formatTimestamp(entry.timestamp),
                            color = Theme.muted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

private fun statusLabel(s: String): String = when (s) {
    "" -> "Not Started"
    "progress" -> "In Progress"
    "done" -> "Done"
    else -> s
}

private fun formatTimestamp(ts: Long): String {
    val fmt = java.text.SimpleDateFormat("MMM d, yyyy HH:mm:ss", java.util.Locale.getDefault())
    return fmt.format(java.util.Date(ts))
}

@Composable
private fun Field(
    state: AppState,
    value: String, onValueChange: (String) -> Unit,
    placeholder: String = "", singleLine: Boolean = true,
    minHeight: androidx.compose.ui.unit.Dp = 36.dp
) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = minHeight)
            .background(Theme.buttonBg, RoundedCornerShape(12.dp))
            .border(1.dp, Theme.borderSoft, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        if (value.isEmpty() && placeholder.isNotEmpty()) Text(
            placeholder,
            color = Theme.muted,
            fontSize = 13.sp
        )
        BasicTextField(
            value = value, onValueChange = onValueChange, singleLine = singleLine,
            textStyle = TextStyle(
                color = Theme.text,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            ),
            cursorBrush = SolidColor(Theme.accent),
            modifier = Modifier.fillMaxWidth().onFocusChanged {
                state.textFieldFocused = it.isFocused
            }
        )
    }
}

@Composable
private fun StatusSelect(value: String, onValueChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (value) {
        "" -> "⬜ Not Started"; "progress" -> "🟡 In Progress"; "done" -> "✅ Done"; else -> "⬜ Not Started"
    }
    Box {
        Box(
            Modifier.fillMaxWidth().background(Theme.buttonBg, RoundedCornerShape(12.dp))
                .border(1.dp, Theme.borderSoft, RoundedCornerShape(12.dp))
                .pointerHoverIcon(PointerIcon.Hand).clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) { Text(label, color = Theme.text, fontSize = 13.sp) }
        if (expanded) {
            Popup(
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true)
            ) {
                Column(
                    Modifier.background(Theme.listBg)
                        .border(1.dp, Theme.listBorder, RoundedCornerShape(12.dp))
                        .width(220.dp).padding(vertical = 6.dp)
                ) {
                    listOf("" to "⬜ Not Started", "progress" to "🟡 In Progress", "done" to "✅ Done")
                        .forEach { (k, v) ->
                            Box(
                                Modifier.fillMaxWidth().pointerHoverIcon(PointerIcon.Hand)
                                    .clickable { onValueChange(k); expanded = false }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) { Text(v, color = Theme.text, fontSize = 13.sp) }
                        }
                }
            }
        }
    }
}

@Composable
private fun ModalOverlay(state: AppState) {
    Box(
        Modifier.fillMaxSize().background(Theme.overlayDim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { state.closeModal() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.width(460.dp).background(Theme.popupBg, RoundedCornerShape(24.dp))
                .border(1.dp, Theme.popupBorder, RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { }
                .padding(18.dp)
        ) {
            Text(
                state.modalTitle,
                color = Theme.titleColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(14.dp))
            val fr = remember { FocusRequester() }
            Box(
                Modifier.fillMaxWidth().background(Theme.bg, RoundedCornerShape(4.dp))
                    .border(1.dp, Theme.border, RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                BasicTextField(
                    value = state.modalValue, onValueChange = { state.modalValue = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Theme.text,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    cursorBrush = SolidColor(Theme.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { state.confirmModal() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(fr)
                        .onPreviewKeyEvent { ev ->
                            if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (ev.key) {
                                Key.Enter -> {
                                    state.confirmModal(); true
                                }

                                Key.Escape -> {
                                    state.closeModal(); true
                                }

                                else -> false
                            }
                        }
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Btn("Cancel") { state.closeModal() }
                Spacer(Modifier.width(8.dp))
                Btn(state.modalButtonText, primary = true) { state.confirmModal() }
            }
            LaunchedEffect(Unit) { fr.requestFocus() }
        }
    }
}


@Composable
private fun HideDoneToggle(state: AppState) {
    if (state.currentProject() == null) return
    var hovered by remember { mutableStateOf(false) }
    val active = state.hideDone
    val bg = when {
        active -> Theme.accentPrimary
        hovered -> Theme.buttonHover
        else -> Theme.buttonBg
    }
    val borderCol = if (active) Theme.accentPrimaryBorder else Theme.buttonBorder
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                .pointerHoverIcon(PointerIcon.Hand)
                .onPointerEvent(PointerEventType.Enter) { hovered = true }
                .onPointerEvent(PointerEventType.Exit) { hovered = false }
                .clickable { state.hideDone = !state.hideDone }
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                if (active) "☑ Hide Done" else "☐ Hide Done",
                color = if (active) Color.White else Theme.text,
                fontSize = 12.sp
            )
        }
    }
}

data class WorkspaceStats(
    val totalFolders: Int = 0,
    val totalFiles: Int = 0,
    val done: Int = 0,
    val progress: Int = 0,
    val notStarted: Int = 0
) {
    val total: Int get() = totalFolders + totalFiles
}

private fun computeStats(root: Node?): WorkspaceStats {
    if (root == null) return WorkspaceStats()
    var folders = 0
    var files = 0
    var done = 0
    var progress = 0
    var notStarted = 0
    fun visit(n: Node) {
        if (n.isFolder()) folders++ else files++
        when (n.status) {
            "done" -> done++
            "progress" -> progress++
            else -> notStarted++
        }
        n.children.forEach { visit(it) }
    }
    visit(root)
    return WorkspaceStats(folders, files, done, progress, notStarted)
}

@Composable
private fun StatusBar(state: AppState) {
    val rev = state.revision
    val proj = state.currentProject()
    val stats = remember(rev, state.selectedProjectId) {
        computeStats(proj?.tree)
    }
    val selectedCount = state.selectedNodeIds.size

    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Theme.borderSoft)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .height(26.dp)
                .background(Theme.panel2)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (proj == null) {
                Text("No project open", color = Theme.muted, fontSize = 11.sp)
            } else {
                Text(
                    "${stats.totalFolders}📁 · ${stats.totalFiles}📄",
                    color = Theme.muted, fontSize = 11.sp
                )
                Text("·", color = Theme.muted, fontSize = 11.sp)
                Text("⬜ ${stats.notStarted}", color = Theme.muted, fontSize = 11.sp)
                Text("🟡 ${stats.progress}", color = Theme.muted, fontSize = 11.sp)
                Text("✅ ${stats.done}", color = Theme.muted, fontSize = 11.sp)
                if (stats.total > 0) {
                    Text("·", color = Theme.muted, fontSize = 11.sp)
                    val pct = (stats.done * 100) / stats.total
                    Text(
                        "${pct}%",
                        color = Theme.text,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (selectedCount > 1) {
                Text("$selectedCount selected", color = Theme.accent, fontSize = 11.sp)
                Text("·", color = Theme.muted, fontSize = 11.sp)
            }
            Text("📄 ${state.fileState}", color = Theme.muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ToastHost(state: AppState) {
    if (state.toasts.isEmpty()) return
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 44.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            state.toasts.forEach { t ->
                key(t.id) {
                    ToastItem(t)
                    LaunchedEffect(t.id) {
                        delay(t.durationMs)
                        state.dismissToast(t.id)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToastItem(t: ToastMessage) {
    val (bg, border, textCol) = when (t.type) {
        ToastType.Info -> Triple(Color(0xFF1E3A5F), Color(0xFF3B82F6), Color(0xFFDBEAFE))
        ToastType.Success -> Triple(Color(0xFF14361F), Color(0xFF22C55E), Color(0xFFDCFCE7))
        ToastType.Warning -> Triple(Color(0xFF3D2E0D), Color(0xFFEAB308), Color(0xFFFEF9C3))
        ToastType.Error -> Triple(Color(0xFF3D1616), Color(0xFFEF4444), Color(0xFFFEE2E2))
    }
    Box(
        Modifier
            .widthIn(min = 220.dp, max = 400.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(t.text, color = textCol, fontSize = 14.sp)
    }
}

@Composable
private fun ContextMenuView(state: AppState, project: Project) {
    if (!state.contextMenuVisible) return
    val targetId = state.contextMenuTargetId
    if (targetId == null) {
        state.contextMenuVisible = false; return
    }
    val node = TreeOps.findNode(project.tree, targetId)
    if (node == null) {
        state.contextMenuVisible = false; return
    }
    val isRoot = node.id == project.tree.id

    Popup(
        onDismissRequest = { state.contextMenuVisible = false },
        offset = androidx.compose.ui.unit.IntOffset(state.contextMenuX, state.contextMenuY),
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            Modifier
                .width(220.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Theme.listBg)
                .border(1.dp, Theme.popupBorder, RoundedCornerShape(14.dp))
                .padding(vertical = 6.dp)
        ) {
            CtxItem(if (state.selectedNodeIds.size > 1) "✏  Rename (${state.selectedNodeIds.size})" else "✏  Rename") {
                state.contextMenuVisible = false
                state.renamingNodeId = node.id
            }
            if (!isRoot) {
                CtxItem(if (state.selectedNodeIds.size > 1) "🗑  Delete (${state.selectedNodeIds.size})" else "🗑  Delete") {
                    state.contextMenuVisible = false
                    deleteSelected(state)
                }
            }
            if (node.isFolder()) {
                CtxItem("📁  Add Folder") {
                    state.contextMenuVisible = false
                    state.snapshot()
                    state.mutate {
                        val child = Node(
                            type = "folder",
                            name = TreeOps.uniqueName(node.children, "NewFolder"),
                            open = true
                        )
                        node.children.add(child)
                        node.open = true
                        state.selectedNodeId = child.id
                    }
                }
                CtxItem("📄  Add File") {
                    state.contextMenuVisible = false
                    state.snapshot()
                    state.mutate {
                        val child = Node(
                            type = "file",
                            name = TreeOps.uniqueName(node.children, "NewFile.kt")
                        )
                        node.children.add(child)
                        node.open = true
                        state.selectedNodeId = child.id
                    }
                }
                CtxItem(if (node.open) "▾  Collapse" else "▸  Expand") {
                    state.contextMenuVisible = false
                    state.mutate { node.open = !node.open }
                }
            }
        }
    }
}

@Composable
private fun CtxItem(label: String, onClick: () -> Unit) {
    var hovered by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (hovered) Theme.hover else Color.Transparent)
            .onPointerEvent(PointerEventType.Enter) { hovered = true }
            .onPointerEvent(PointerEventType.Exit) { hovered = false }
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(label, color = Theme.text, fontSize = 14.sp)
    }
}

data class BreadcrumbSegment(val nodeId: String, val label: String)

private fun buildBreadcrumbSegments(project: Project, target: Node?): List<BreadcrumbSegment> {
    val out = mutableListOf<BreadcrumbSegment>()
    out.add(BreadcrumbSegment(project.tree.id, project.name))
    if (target == null) return out
    fun find(n: Node, path: MutableList<Node>): Boolean {
        path.add(n)
        if (n.id == target.id) return true
        for (child in n.children) if (find(child, path)) return true
        path.removeAt(path.size - 1)
        return false
    }

    val path = mutableListOf<Node>()
    if (find(project.tree, path)) {
        for (i in 1 until path.size) out.add(BreadcrumbSegment(path[i].id, path[i].name))
    }
    return out
}

@Composable
private fun Breadcrumb(state: AppState, project: Project, selectedNode: Node?) {
    val segments = remember(project.id, selectedNode?.id, state.revision) {
        buildBreadcrumbSegments(project, selectedNode)
    }
    var menuVisible by remember { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxWidth().height(44.dp)) {
        val widthDp = maxWidth.value
        val budget = ((widthDp - 30f) / 7.5f).toInt()
        val n = segments.size
        val keepTail = mutableListOf<Int>()
        if (n >= 1) {
            keepTail.add(n - 1)
            var used = segments[0].label.length + segments[n - 1].label.length + 20
            if (n > 2) {
                for (i in (n - 2) downTo 1) {
                    val addLen = segments[i].label.length + 3
                    if (used + addLen <= budget) {
                        keepTail.add(0, i); used += addLen
                    } else break
                }
            }
        }
        val hiddenIndices =
            if (keepTail.isEmpty()) emptyList() else (1 until keepTail.first()).toList()
        val collapsed = hiddenIndices.isNotEmpty()

        Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.CenterStart) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (!collapsed) {
                    segments.forEachIndexed { idx, seg ->
                        if (idx > 0) BreadcrumbSep()
                        BreadcrumbSegmentItem(
                            label = seg.label,
                            isLast = idx == n - 1,
                            onClick = { state.selectOnly(seg.nodeId) }
                        )
                    }
                } else {
                    BreadcrumbSegmentItem(
                        label = segments[0].label, isLast = false, maxChars = 20,
                        onClick = { state.selectOnly(segments[0].nodeId) }
                    )
                    BreadcrumbSep()
                    BreadcrumbEllipsis(
                        state = state,
                        segments = segments,
                        hiddenIndices = hiddenIndices,
                        onClose = { menuVisible = false }
                    )
                    BreadcrumbSep()
                    keepTail.forEachIndexed { i, idx ->
                        if (i > 0) BreadcrumbSep()
                        BreadcrumbSegmentItem(
                            label = segments[idx].label, isLast = idx == n - 1, maxChars = 25,
                            onClick = { state.selectOnly(segments[idx].nodeId) }
                        )
                    }
                }
            }


        }
    }
}

@Composable
private fun BreadcrumbSep() {
    Text("/", color = Color(0xFF666666), fontSize = 13.sp)
}

@Composable
private fun BreadcrumbSegmentItem(
    label: String,
    isLast: Boolean,
    maxChars: Int = Int.MAX_VALUE,
    onClick: () -> Unit
) {
    var hovered by remember { mutableStateOf(false) }
    val display =
        if (label.length > maxChars && maxChars >= 4) label.take(maxChars - 1) + "…" else label
    Box(
        Modifier.clip(RoundedCornerShape(3.dp))
            .background(if (hovered) Theme.hover else Color.Transparent)
            .pointerHoverIcon(PointerIcon.Hand)
            .onPointerEvent(PointerEventType.Enter) { hovered = true }
            .onPointerEvent(PointerEventType.Exit) { hovered = false }
            .clickable { onClick() }
            .padding(horizontal = 5.dp, vertical = 3.dp)
    ) {
        Text(
            display,
            color = if (isLast) Theme.text else Color(0xFFAAAAAA),
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun BreadcrumbEllipsis(
    state: AppState,
    segments: List<BreadcrumbSegment>,
    hiddenIndices: List<Int>,
    onClose: () -> Unit
) {
    var hovered by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var ellipsisHeightPx by remember { mutableStateOf(30) }

    Box {
        Box(
            Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(if (hovered) Theme.hover else Color.Transparent)
                .pointerHoverIcon(PointerIcon.Hand)
                .onGloballyPositioned { ellipsisHeightPx = it.size.height }
                .onPointerEvent(PointerEventType.Enter) { hovered = true }
                .onPointerEvent(PointerEventType.Exit) { hovered = false }
                .clickable { menuOpen = true }
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text("...", color = Color(0xFFAAAAAA), fontSize = 13.sp)
        }
        if (menuOpen) {
            Popup(
                onDismissRequest = { menuOpen = false },
                offset = androidx.compose.ui.unit.IntOffset(0, ellipsisHeightPx + 4),
                properties = PopupProperties(focusable = true)
            ) {
                Column(
                    Modifier
                        .width(260.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Theme.listBg)
                        .border(1.dp, Theme.popupBorder, RoundedCornerShape(14.dp))
                        .padding(vertical = 6.dp)
                ) {
                    hiddenIndices.forEach { idx ->
                        val seg = segments[idx]
                        var itemHovered by remember(seg.nodeId) { mutableStateOf(false) }
                        Box(
                            Modifier.fillMaxWidth()
                                .background(if (itemHovered) Theme.hover else Color.Transparent)
                                .onPointerEvent(PointerEventType.Enter) { itemHovered = true }
                                .onPointerEvent(PointerEventType.Exit) { itemHovered = false }
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable {
                                    state.selectOnly(seg.nodeId)
                                    menuOpen = false
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📁", fontSize = 12.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    seg.label, color = Theme.text, fontSize = 13.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
// ============ Empty State ============

@Composable
private fun EmptyState(
    emoji: String,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Theme.panel)
                    .border(1.dp, Theme.border, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 42.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                title,
                color = Theme.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                subtitle,
                color = Theme.muted,
                fontSize = 12.sp
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Theme.accentPrimary)
                        .border(1.dp, Theme.accentPrimaryBorder, RoundedCornerShape(6.dp))
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable { onAction() }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(actionLabel, color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}

// ============ Shortcuts Viewer ============

@Composable
private fun ShortcutsEditor(state: AppState) {
    val shortcuts = state.meta.shortcuts

    Box(
        Modifier.fillMaxSize().background(Color(0x88000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { state.shortcutsEditorVisible = false },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .width(460.dp)
                .heightIn(max = 600.dp)
                .background(Theme.popupBg, RoundedCornerShape(24.dp))
                .border(1.dp, Theme.popupBorder, RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { }
        ) {
            // Header with X button
            Row(
                Modifier.fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "⌨️  Keyboard Shortcuts", color = Theme.titleColor,
                    fontWeight = FontWeight.Bold, fontSize = 16.sp
                )
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable { state.shortcutsEditorVisible = false }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("✕", color = Theme.muted, fontSize = 16.sp)
                }
            }
            HorizontalDivider(Theme.line)

            // List
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 10.dp)
            ) {
                shortcutLabels.forEach { (actionKey, label) ->
                    val sc = shortcuts[actionKey] ?: state.getShortcut(actionKey)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            label, color = Theme.text, fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Theme.bg)
                                .border(1.dp, Theme.border, RoundedCornerShape(4.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                if (sc.isBlank()) "—" else sc,
                                color = if (sc.isBlank()) Theme.muted else Theme.text,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
            HorizontalDivider(Theme.line)

            // Footer with Close
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Btn("Close", primary = true) {
                    state.shortcutsEditorVisible = false
                }
            }
        }
    }
}

private fun keyNameOf(k: Key): String? {
    return when (k) {
        Key.A -> "A"; Key.B -> "B"; Key.C -> "C"; Key.D -> "D"; Key.E -> "E"
        Key.F -> "F"; Key.G -> "G"; Key.H -> "H"; Key.I -> "I"; Key.J -> "J"
        Key.K -> "K"; Key.L -> "L"; Key.M -> "M"; Key.N -> "N"; Key.O -> "O"
        Key.P -> "P"; Key.Q -> "Q"; Key.R -> "R"; Key.S -> "S"; Key.T -> "T"
        Key.U -> "U"; Key.V -> "V"; Key.W -> "W"; Key.X -> "X"; Key.Y -> "Y"
        Key.Z -> "Z"
        Key.Zero -> "0"; Key.One -> "1"; Key.Two -> "2"; Key.Three -> "3"
        Key.Four -> "4"; Key.Five -> "5"; Key.Six -> "6"; Key.Seven -> "7"
        Key.Eight -> "8"; Key.Nine -> "9"
        Key.F1 -> "F1"; Key.F2 -> "F2"; Key.F3 -> "F3"; Key.F4 -> "F4"
        Key.F5 -> "F5"; Key.F6 -> "F6"; Key.F7 -> "F7"; Key.F8 -> "F8"
        Key.F9 -> "F9"; Key.F10 -> "F10"; Key.F11 -> "F11"; Key.F12 -> "F12"
        Key.Delete -> "Delete"
        Key.Enter -> "Enter"
        Key.Spacebar -> "Space"
        Key.Tab -> "Tab"
        Key.MoveHome -> "Home"
        Key.MoveEnd -> "End"
        Key.Insert -> "Insert"
        Key.PageUp -> "PageUp"
        Key.PageDown -> "PageDown"
        Key.DirectionUp -> "Up"
        Key.DirectionDown -> "Down"
        Key.DirectionLeft -> "Left"
        Key.DirectionRight -> "Right"
        else -> null
    }
}

@Composable
private fun SyncBtn(onClick: () -> Unit) {
    var hovered by remember { mutableStateOf(false) }
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (hovered) Theme.buttonHover else Theme.buttonBg)
            .border(1.dp, Theme.buttonBorder, RoundedCornerShape(20.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .onPointerEvent(PointerEventType.Enter) { hovered = true }
            .onPointerEvent(PointerEventType.Exit) { hovered = false }
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("↻", color = Theme.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text("Sync", color = Theme.text, fontSize = 14.sp)
        }
    }
}

@Composable
private fun HamburgerMenu(state: AppState) {
    if (!state.hamburgerVisible) return
    Box(
        Modifier.fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { state.hamburgerVisible = false }
    ) {
        Column(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 62.dp, end = 16.dp)
                .width(280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Theme.listBg)
                .border(1.dp, Theme.popupBorder, RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { }
                .padding(vertical = 8.dp)
        ) {
            MenuRow("📂", "Open JSON", null) {
                state.hamburgerVisible = false
                state.openWorkspaceFileDialog()
            }
            MenuRow("💾", "Save JSON", null) {
                state.hamburgerVisible = false
                state.saveWorkspaceFileDialog()
            }
            MenuRow("📥", "Import", null) {
                state.hamburgerVisible = false
                state.importWorkspaceDialog()
            }
            MenuDivider()
            MenuRow("⌨️", "Keyboard Shortcuts", null) {
                state.hamburgerVisible = false
                state.shortcutsEditorVisible = true
            }
            MenuRow(
                emoji = when (Theme.mode) {
                    ThemeMode.Dark -> "🌙"
                    ThemeMode.Light -> "☀️"
                    ThemeMode.Auto -> "🌓"
                },
                label = "Theme",
                trailing = when (Theme.mode) {
                    ThemeMode.Dark -> "Dark"
                    ThemeMode.Light -> "Light"
                    ThemeMode.Auto -> "Auto"
                }
            ) {
                state.cycleTheme()
            }
        }
    }
}

@Composable
private fun MenuRow(emoji: String, label: String, trailing: String?, onClick: () -> Unit) {
    var hovered by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (hovered) Theme.hover else Color.Transparent)
            .pointerHoverIcon(PointerIcon.Hand)
            .onPointerEvent(PointerEventType.Enter) { hovered = true }
            .onPointerEvent(PointerEventType.Exit) { hovered = false }
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 18.sp)
        Spacer(Modifier.width(14.dp))
        Text(label, color = Theme.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, color = Theme.muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun MenuDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 12.dp)
            .height(1.dp)
            .background(Theme.borderSoft)
    )
}

private fun buildFullPath(project: Project, target: Node): String {
    val parts = mutableListOf<String>()
    fun find(n: Node, path: List<String>): Boolean {
        if (n.id == target.id) {
            parts.addAll(path); parts.add(n.name); return true
        }
        for (c in n.children) if (find(c, path + n.name)) return true
        return false
    }
    find(project.tree, emptyList())
    return (listOf(project.name) + parts).joinToString(" / ")
}