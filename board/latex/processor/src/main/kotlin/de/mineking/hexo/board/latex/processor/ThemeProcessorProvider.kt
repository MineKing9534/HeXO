package de.mineking.hexo.board.latex.processor

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class ThemeProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) = ThemeProcessor(
        codeGenerator = environment.codeGenerator,
        logger = environment.logger,
        themes = environment.options["themes"]
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty(),
    )
}
