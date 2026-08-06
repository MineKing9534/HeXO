package de.mineking.hexo.database.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate

class DatabaseProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {
    private var generated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (generated) return emptyList()

        val implementations = resolver
            .getSymbolsWithAnnotation("de.mineking.hexo.database.RegisterMigration")
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.validate() }
            .toList()

        if (implementations.isEmpty()) return emptyList()

        val dependencies = Dependencies(
            aggregating = true,
            sources = implementations
                .mapNotNull { it.containingFile }
                .toTypedArray(),
        )

        codeGenerator.createNewFileByPath(
            dependencies = dependencies,
            path = "META-INF/services/de.mineking.hexo.database.Migration",
            extensionName = "",
        ).bufferedWriter().use { writer ->
            implementations.forEach { implementation ->
                writer.appendLine(implementation.qualifiedName!!.asString())
            }
        }

        generated = true
        return emptyList()
    }
}
