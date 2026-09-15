package axon

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

@Serializable
data class HistoryEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val action: String = ""
)

@Serializable
data class Node(
    val id: String = newId(),
    var type: String,
    var name: String,
    var description: String = "",
    var status: String = "",
    var open: Boolean = true,
    var children: MutableList<Node> = mutableListOf(),
    var notes: String = "",
    var history: MutableList<HistoryEntry> = mutableListOf()
) {
    fun isFolder() = type == "folder"
    fun isFile() = type == "file"
}

@Serializable
data class Project(
    val id: String = newId(),
    var name: String,
    var tree: Node
)

@Serializable
data class Workspace(
    var projects: MutableList<Project> = mutableListOf(),
    var activeId: String? = null
)

@Serializable
data class Meta(
    var localUpdatedAt: Long = 0L,
    var lastSyncedLocalUpdatedAt: Long = 0L,
    var lastSyncedRemoteModified: Long = 0L,
    var linkedFilePath: String? = null,
    var themeMode: String = "auto",
    var windowX: Int = -1,
    var windowY: Int = -1,
    var windowWidth: Int = 1400,
    var windowHeight: Int = 900,
    var explorerWidth: Float = 390f,
    var projectsCollapsed: Boolean = false,
    var shortcuts: Map<String, String> = defaultShortcuts()
)

fun defaultShortcuts(): Map<String, String> = mapOf(
    "undo" to "Ctrl+Z",
    "redo" to "Ctrl+Y",
    "save" to "Ctrl+S",
    "copy" to "Ctrl+C",
    "paste" to "Ctrl+V",
    "delete" to "Delete",
    "rename" to "F2",
    "newProject" to "Ctrl+N",
    "import" to "Ctrl+I",
    "selectAll" to "Ctrl+A"
)

val shortcutLabels: Map<String, String> = mapOf(
    "undo" to "Undo",
    "redo" to "Redo",
    "save" to "Save JSON",
    "copy" to "Copy",
    "paste" to "Paste",
    "delete" to "Delete Selected",
    "rename" to "Rename",
    "newProject" to "New Project",
    "import" to "Import JSON",
    "selectAll" to "Select All"
)

// ---------------- Toast ----------------

enum class ToastType { Info, Success, Warning, Error }

data class ToastMessage(
    val id: Long = System.nanoTime() + (0..999).random(),
    val text: String,
    val type: ToastType = ToastType.Info,
    val durationMs: Long = 2500L
)

// ---------------- Theme ----------------

enum class ThemeMode { Dark, Light, Auto }

object Theme {
    var mode by mutableStateOf(ThemeMode.Auto)
        private set
    var effective by mutableStateOf(ThemeMode.Dark)
        private set

    // === Vivid gradient background (للشفافية تشتغل) ===
    var bg by mutableStateOf(Color(0xFF08080C)); private set
    var bgGradientTop by mutableStateOf(Color(0xFF050508)); private set
    var bgGradientMid by mutableStateOf(Color(0xFF0C0C10)); private set
    var bgGradientBottom by mutableStateOf(Color(0xFF050508)); private set

    // === Panels = WHITE alpha (glass حقيقي) ===
    var panel by mutableStateOf(Color(0x1FFFFFFF)); private set       // 12% white
    var panelDark by mutableStateOf(Color(0x14FFFFFF)); private set   // 8% white
    var panel2 by mutableStateOf(Color(0x0FFFFFFF)); private set      // 6% white

    // === Lines / Borders (white alpha) ===
    var line by mutableStateOf(Color(0x1AFFFFFF)); private set        // 10%
    var border by mutableStateOf(Color(0x33FFFFFF)); private set      // 20%
    var borderSoft by mutableStateOf(Color(0x26FFFFFF)); private set  // 15%
    var borderGlass by mutableStateOf(Color(0x40FFFFFF)); private set // 25% top highlight

    // === Text ===
    var text by mutableStateOf(Color(0xFFEAEAF2)); private set
    var textBright by mutableStateOf(Color(0xFFFFFFFF)); private set
    var muted by mutableStateOf(Color(0xFF9A9AB0)); private set
    var titleColor by mutableStateOf(Color(0xFFFFFFFF)); private set

    // === Accent (vibrant glass blue) ===
    var accent by mutableStateOf(Color(0xFF6FA8FF)); private set
    var accentHover by mutableStateOf(Color(0xFF9BC1FF)); private set
    var accentPrimary by mutableStateOf(Color(0xFF6FA8FF)); private set
    var accentPrimaryBorder by mutableStateOf(Color(0xFF9BC1FF)); private set
    var onPrimary by mutableStateOf(Color(0xFF001233)); private set

    // === States ===
    var hover by mutableStateOf(Color(0x1FFFFFFF)); private set
    var selected by mutableStateOf(Color(0x336FA8FF)); private set
    var selectedBorder by mutableStateOf(Color(0x556FA8FF)); private set
    var dragTarget by mutableStateOf(Color(0xFF6FA8FF)); private set
    var dragTargetBg by mutableStateOf(Color(0x407AA7FF)); private set
    var overlayDim by mutableStateOf(Color(0xCC000000)); private set

    // === Buttons (glass white) ===
    var buttonBg by mutableStateOf(Color(0x1FFFFFFF)); private set
    var buttonBorder by mutableStateOf(Color(0x33FFFFFF)); private set
    var buttonHover by mutableStateOf(Color(0x33FFFFFF)); private set

    // === Status ===
    var danger by mutableStateOf(Color(0xFFFF8A95)); private set
    var folderText by mutableStateOf(Color(0xFFE5CFA0)); private set
    var fileText by mutableStateOf(Color(0xFF9BC1FF)); private set
    var doneLine by mutableStateOf(Color(0xFF6A6A78)); private set
    var scrollBarBg by mutableStateOf(Color(0xFF050508)); private set
    var popupBg by mutableStateOf(Color(0xFA0A0A12)); private set
    var popupBorder by mutableStateOf(Color(0x40FFFFFF)); private set
    var listBg by mutableStateOf(Color(0xFF0A0A12)); private set
    var listBorder by mutableStateOf(Color(0x33FFFFFF)); private set

    fun apply(m: ThemeMode) {
        mode = m
        val eff = when (m) {
            ThemeMode.Dark -> ThemeMode.Dark
            ThemeMode.Light -> ThemeMode.Light
            ThemeMode.Auto -> if (detectWindowsTheme()) ThemeMode.Light else ThemeMode.Dark
        }
        effective = eff
        when (eff) {
            ThemeMode.Light -> applyLight()
            else -> applyDark()
        }
    }

    fun detectWindowsTheme(): Boolean {
        return try {
            val proc = ProcessBuilder(
                "reg", "query",
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                "/v", "AppsUseLightTheme"
            ).redirectErrorStream(true).start()
            val out = proc.inputStream.bufferedReader().readText()
            proc.waitFor()
            out.contains("0x1")
        } catch (e: Exception) {
            false
        }
    }

    private fun applyDark() {
        // Black-centered gradient (subtle shade shifts)
        bgGradientTop = Color(0xFF050508)
        bgGradientMid = Color(0xFF0C0C10)
        bgGradientBottom = Color(0xFF050508)
        bg = Color(0xFF08080C)

        // Popups (nearly opaque)
        popupBg = Color(0xFA0A0A12)
        popupBorder = Color(0x40FFFFFF)

        // Lists (fully opaque)
        listBg = Color(0xFF0A0A12)
        listBorder = Color(0x33FFFFFF)

        // Glass panels (white alpha)
        panel = Color(0x1FFFFFFF)
        panelDark = Color(0x14FFFFFF)
        panel2 = Color(0x0FFFFFFF)

        // Glass borders
        line = Color(0x1AFFFFFF)
        border = Color(0x33FFFFFF)
        borderSoft = Color(0x26FFFFFF)
        borderGlass = Color(0x40FFFFFF)

        // Text
        text = Color(0xFFEAEAF2)
        textBright = Color(0xFFFFFFFF)
        muted = Color(0xFF9A9AB0)
        titleColor = Color(0xFFFFFFFF)

        // Accent
        accent = Color(0xFF6FA8FF)
        accentHover = Color(0xFF9BC1FF)
        accentPrimary = Color(0xFF6FA8FF)
        accentPrimaryBorder = Color(0xFF9BC1FF)
        onPrimary = Color(0xFF001233)

        // States
        hover = Color(0x1FFFFFFF)
        selected = Color(0x336FA8FF)
        selectedBorder = Color(0x556FA8FF)
        dragTarget = Color(0xFF6FA8FF)
        dragTargetBg = Color(0x407AA7FF)
        overlayDim = Color(0xCC000000)

        // Buttons
        buttonBg = Color(0x1FFFFFFF)
        buttonBorder = Color(0x33FFFFFF)
        buttonHover = Color(0x33FFFFFF)

        // Status
        danger = Color(0xFFFF8A95)
        folderText = Color(0xFFE5CFA0)
        fileText = Color(0xFF9BC1FF)
        doneLine = Color(0xFF6A6A78)
        scrollBarBg = Color(0xFF050508)
    }

    private fun applyLight() {
        // Soft gradient light
        bgGradientTop = Color(0xFFE5ECFF)
        bgGradientMid = Color(0xFFF0E5FF)
        bgGradientBottom = Color(0xFFE8EEFF)
        bg = Color(0xFFEEF1F7)

        // Popups (nearly opaque)
        popupBg = Color(0xFAFFFFFF)
        popupBorder = Color(0x33000000)

        // Lists (fully opaque)
        listBg = Color(0xFFFFFFFF)
        listBorder = Color(0x2E000000)

        // White glass panels
        panel = Color(0xB3FFFFFF)
        panelDark = Color(0x99FFFFFF)
        panel2 = Color(0x8CFFFFFF)

        line = Color(0x1A000000)
        border = Color(0x33000000)
        borderSoft = Color(0x26000000)
        borderGlass = Color(0x40FFFFFF)

        text = Color(0xFF1C1C1E)
        textBright = Color(0xFF000000)
        muted = Color(0xFF70707A)
        titleColor = Color(0xFF000000)

        accent = Color(0xFF1B62D9)
        accentHover = Color(0xFF1552B5)
        accentPrimary = Color(0xFF1B62D9)
        accentPrimaryBorder = Color(0xFF1552B5)
        onPrimary = Color(0xFFFFFFFF)

        hover = Color(0x0A000000)
        selected = Color(0x331B62D9)
        selectedBorder = Color(0x661B62D9)
        dragTarget = Color(0xFF1B62D9)
        dragTargetBg = Color(0x331B62D9)
        overlayDim = Color(0x66000000)

        buttonBg = Color(0x1A000000)
        buttonBorder = Color(0x26000000)
        buttonHover = Color(0x33000000)

        danger = Color(0xFFC8142D)
        folderText = Color(0xFF8E5A00)
        fileText = Color(0xFF1B62D9)
        doneLine = Color(0xFF9999A0)
        scrollBarBg = Color(0xFFEEF1F7)
    }
}