package axon

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.delay

fun main() = application {
    val state = remember { AppState() }
    val meta = state.meta
    val initialPosition = if (meta.windowX >= 0 && meta.windowY >= 0)
        WindowPosition(meta.windowX.dp, meta.windowY.dp)
    else
        WindowPosition(Alignment.Center)
    val initialSize = DpSize(meta.windowWidth.dp, meta.windowHeight.dp)
    val windowState = rememberWindowState(position = initialPosition, size = initialSize)

    Window(
        onCloseRequest = {
            state.saveWindowState(
                windowState.position.x.value.toInt(),
                windowState.position.y.value.toInt(),
                windowState.size.width.value.toInt(),
                windowState.size.height.value.toInt()
            )
            state.saveUiState()
            exitApplication()
        },
        title = "Axon Workspace",
        state = windowState,
        onPreviewKeyEvent = { ev -> handleGlobalKey(state, ev) }
    ) {
        LaunchedEffect(windowState.size, windowState.position) {
            delay(600)
            state.saveWindowState(
                windowState.position.x.value.toInt(),
                windowState.position.y.value.toInt(),
                windowState.size.width.value.toInt(),
                windowState.size.height.value.toInt()
            )
        }
        LaunchedEffect(state.explorerWidth, state.projectsCollapsed) {
            delay(400)
            state.saveUiState()
        }
        AxonApp(state)
    }
}
