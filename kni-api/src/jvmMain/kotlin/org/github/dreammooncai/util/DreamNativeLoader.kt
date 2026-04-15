package org.github.dreammooncai.util

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

object DreamNativeLoader {

    private val loaded = HashSet<String>()
    private val lock = Any()

    /**
     * 定义缓存根目录，这里使用系统临时目录下的固定文件夹
     */
    private val cacheRoot: Path by lazy {
        Path.of(System.getProperty("java.io.tmpdir"), "dream-native-cache").apply {
            if (!exists()) createDirectories()
        }
    }

    @Suppress("UnsafeDynamicallyLoadedCode")
    fun loadLibrary(name: String) {
        synchronized(lock) {
            if (loaded.contains(name)) return

            runCatching {
                // 1. 尝试从系统路径加载 (java.library.path)
                runCatching { System.loadLibrary(name) }.onFailure {

                    val abi = getABI()
                    val libName = when {
                        isWindows() -> "$name.dll"
                        isMac() -> "lib$name.dylib"
                        else -> "lib$name.so"
                    }

                    // 2. 准备资源路径和缓存路径
                    val resourcePath = "/native/$abi/$libName"
                    val stream = DreamNativeLoader::class.java.getResourceAsStream(resourcePath)
                        ?: error("Library not found in resources: $resourcePath")

                    // 3. 确定缓存目标：cache/abi/libName
                    val abiDir = cacheRoot.resolve(abi).apply { if (!exists()) createDirectories() }
                    val targetFile = abiDir.resolve(libName)

                    // 4. 释放文件 (如果文件不存在，或者你想每次强制覆盖则删掉 exists() 判断)
                    // 注意：在 Windows 上，如果 DLL 已被加载，此处覆盖可能会失败
                    runCatching {
                        Files.copy(stream, targetFile, StandardCopyOption.REPLACE_EXISTING)
                    }

                    // 5. 加载缓存的物理路径文件
                    System.load(targetFile.absolutePathString())
                }
            }.onSuccess {
                loaded.add(name)
            }.onFailure {
                throw it
            }
        }
    }

    fun getABI(): String {
        val arch = System.getProperty("os.arch")?.lowercase() ?: return "x86_64"
        return when {
            arch in listOf("x86", "i386", "i486", "i586", "i686") -> "x86"
            arch in listOf("x86_64", "amd64") -> "x86_64"
            arch.startsWith("armv7") || arch == "arm" -> "armeabi-v7a"
            arch == "aarch64" || arch == "arm64" -> "arm64-v8a"
            else -> "x86_64"
        }
    }

    private fun isWindows() = System.getProperty("os.name")?.lowercase()?.contains("win") ?: false
    private fun isMac() = System.getProperty("os.name")?.lowercase()?.contains("mac") ?: false
}