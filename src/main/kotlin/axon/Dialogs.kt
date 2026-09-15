package axon

import javafx.application.Platform
import javafx.stage.FileChooser
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

object Dialogs {
    private val fxStarted = AtomicBoolean(false)

    private fun ensureFx() {
        if (fxStarted.get()) return
        synchronized(this) {
            if (fxStarted.get()) return
            try {
                val latch = CountDownLatch(1)
                Platform.startup { latch.countDown() }
                latch.await(5, TimeUnit.SECONDS)
            } catch (e: IllegalStateException) {
                // JavaFX already initialized
            } catch (e: Exception) {
                e.printStackTrace()
            }
            fxStarted.set(true)
        }
    }

    private fun defaultDir(): File {
        val appRoot = Persistence.appRootDir()
        if (appRoot != null && appRoot.exists() && appRoot.canWrite()) {
            return appRoot
        }
        return File(System.getProperty("user.home"))
    }

    fun chooseOpenJson(): File? {
        ensureFx()
        val ref = AtomicReference<File?>(null)
        val latch = CountDownLatch(1)
        Platform.runLater {
            try {
                val d = defaultDir()
                val fc = FileChooser()
                fc.title = "Open Axon Workspace JSON"
                fc.extensionFilters.add(
                    FileChooser.ExtensionFilter("JSON files", "*.json")
                )
                fc.initialDirectory = d
                ref.set(fc.showOpenDialog(null))
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                latch.countDown()
            }
        }
        latch.await(60, TimeUnit.SECONDS)
        return ref.get()
    }

    fun chooseSaveJson(suggested: File): File? {
        ensureFx()
        val ref = AtomicReference<File?>(null)
        val latch = CountDownLatch(1)

        Platform.runLater {
            try {
                val d = defaultDir()
                val fc = FileChooser()
                fc.title = "Save Axon Workspace"
                fc.extensionFilters.add(
                    FileChooser.ExtensionFilter("JSON files", "*.json")
                )
                fc.initialDirectory = d
                fc.initialFileName = suggested.name

                ref.set(fc.showSaveDialog(null))
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                latch.countDown()
            }
        }

        latch.await(60, TimeUnit.SECONDS)
        var result = ref.get()
        if (result != null && !result.name.lowercase().endsWith(".json")) {
            result = File(result.parentFile, result.name + ".json")
        }
        return result
    }
}