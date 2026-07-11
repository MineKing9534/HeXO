package cc.tyto

import com.sun.jna.FunctionMapper
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal interface StrixSolverLib : Library {
    private annotation class ExternalName(val name: String)

    @ExternalName("hexo_solve_json")
    fun solve(input: String): Pointer

    @ExternalName("hexo_solve_defense_json")
    fun solveDefense(input: String): Pointer

    @ExternalName("hexo_free_string")
    fun freeString(ptr: Pointer)

    companion object {
        private val functionMapper = FunctionMapper { _, method ->
            method.getAnnotation(ExternalName::class.java).name
        }

        private fun extractNativeLibrary(): String {
            val os = System.getProperty("os.name").lowercase()
            val arch = System.getProperty("os.arch").lowercase()

            val platform = when {
                os.contains("win") && arch.contains("64") -> "windows-x86_64"
                os.contains("linux") && arch.contains("64") -> "linux-x86_64"
                os.contains("mac") && arch == "aarch64" -> "macos-aarch64"
                os.contains("mac") && arch.contains("64") -> "macos-x86_64"
                else -> error("Unknown platform: os=$os arch=$arch")
            }

            val filename = when {
                platform.startsWith("windows") -> "hexo_solver_ffi.dll"
                platform.startsWith("macos") -> "libhexo_solver_ffi.dylib"
                else -> "libhexo_solver_ffi.so"
            }

            val resourcePath = "/native/$platform/$filename"
            val input = javaClass.getResourceAsStream(resourcePath)
                ?: error("Native library not found in resources: $resourcePath")

            val tempFile = Files.createTempFile("hexo_solver_ffi-", "-$filename")
            tempFile.toFile().deleteOnExit()

            input.use {
                Files.copy(it, tempFile, StandardCopyOption.REPLACE_EXISTING)
            }

            return tempFile.toAbsolutePath().toString()
        }

        val INSTANCE = Native.load(
            extractNativeLibrary(),
            StrixSolverLib::class.java,
            mapOf(Library.OPTION_FUNCTION_MAPPER to functionMapper),
        )
    }
}
