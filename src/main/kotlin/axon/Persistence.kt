package axon

import kotlinx.serialization.json.Json
import java.io.File

object Persistence {
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Resolve data directory:
    // 1) next to exe/app (portable mode)
    // 2) fallback to USERPROFILE
    private val dir: File = resolveDataDir()
    private val cacheFile = File(dir, "cache.json")
    private val metaFile = File(dir, "meta.json")

    // Physical folder where the current exe is located
    fun appRootDir(): File? {
        return runCatching {
            val command = ProcessHandle.current()
                .info()
                .command()
                .orElse(null)
                ?: return null

            File(command).absoluteFile.parentFile
        }.getOrNull()
    }

    private fun resolveDataDir(): File {
        val candidates = mutableListOf<File>()

        appRootDir()?.let {
            candidates.add(File(it, "data/.axon-data"))
        }

        candidates.add(
            File(System.getProperty("user.home"), ".axon-workspace")
        )

        for (c in candidates) {
            runCatching {
                c.mkdirs()

                if (c.isDirectory && c.canWrite()) {
                    println("[Persistence] Using data dir: ${c.absolutePath}")
                    return c
                }
            }
        }

        val fallback = File(".axon-workspace").apply {
            mkdirs()
        }

        println("[Persistence] Using fallback: ${fallback.absolutePath}")
        return fallback
    }

    fun saveCache(ws: Workspace) {
        runCatching { cacheFile.writeText(json.encodeToString(Workspace.serializer(), ws)) }
            .onFailure { println("Cache save failed: ${it.message}") }
    }

    fun writeCacheRaw(text: String) {
        runCatching { cacheFile.writeText(text) }
            .onFailure { println("Cache save failed: ${it.message}") }
    }

    fun loadCache(): Workspace? = runCatching {
        if (!cacheFile.exists()) null
        else json.decodeFromString(Workspace.serializer(), cacheFile.readText())
    }.getOrNull()

    fun saveMeta(m: Meta) {
        runCatching { metaFile.writeText(json.encodeToString(Meta.serializer(), m)) }
    }

    fun writeMetaRaw(text: String) {
        runCatching { metaFile.writeText(text) }
    }

    fun loadMeta(): Meta = runCatching {
        if (!metaFile.exists()) Meta()
        else json.decodeFromString(Meta.serializer(), metaFile.readText())
    }.getOrDefault(Meta())

    fun dataDirPath(): String = dir.absolutePath
}
