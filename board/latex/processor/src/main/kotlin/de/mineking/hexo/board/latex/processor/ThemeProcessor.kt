package de.mineking.hexo.board.latex.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

private const val OUTPUT_PACKAGE = "de.mineking.hexo.board.latex"

private val LATEX_COMMAND_ANNOTATION = ClassName("de.mineking.kotlinlatex", "LatexCommand")
private val LATEX_TYPE = ClassName("de.mineking.kotlinlatex", "Latex")
private val LATEX_FUNCTION = MemberName("de.mineking.kotlinlatex", "latex")
private val THEME_TYPE = ClassName("de.mineking.hexo.board.render.image.theme", "Theme")
private val COLOR_TYPE = ClassName("de.mineking.hexo.board.render.image.theme", "Color")

class ThemeProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val themes: List<String>,
) : SymbolProcessor {
    private var generated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (generated) return emptyList()
        require(themes.isNotEmpty()) { "At least one theme class is required" }

        generate(themes.map { name ->
            resolver.getClassDeclarationByName(name)
                ?: return emptyList<KSAnnotated>().also { logger.error("Theme '$name' not found") }
        })

        generated = true
        return emptyList()
    }

    @Suppress("SpreadOperator")
    private fun generate(themes: List<KSClassDeclaration>) {
        val sourceFiles = themes.mapNotNull { it.containingFile }.toTypedArray()
        val dependencies = Dependencies(aggregating = true, *sourceFiles)

        val themeModels = themes.map { it.toModel() }

        FileSpec.builder(OUTPUT_PACKAGE, "GeneratedThemes")
            .apply {
                themeModels.forEach {
                    addFunction(createThemeCommand(it))
                }

                addFunction(createParseThemeFunction(themeModels))
            }
            .build()
            .writeTo(codeGenerator, dependencies)
    }

    private fun createThemeCommand(theme: ThemeModel) = FunSpec.builder(theme.functionName)
        .addAnnotation(
            AnnotationSpec.builder(LATEX_COMMAND_ANNOTATION)
                .addMember("%S", theme.commandName)
                .build(),
        )
        .addParameters(theme.parameters.map { name ->
            ParameterSpec.builder(name, LATEX_TYPE)
                .defaultValue("%M(\"\")", LATEX_FUNCTION)
                .build()
        })
        .returns(LATEX_TYPE)
        .addCode("return themeSpec(\n%S,\nlistOf(${theme.parameters.joinToString()}),\n)", theme.specName)
        .build()

    private fun createParseThemeFunction(themes: List<ThemeModel>) = FunSpec.builder("parseTheme")
        .addModifiers(KModifier.INTERNAL)
        .receiver(LATEX_TYPE)
        .returns(THEME_TYPE)
        .addCode(buildCodeBlock {
            addStatement("val values = source.split('|')")
            beginControlFlow("return when (values[0])")
            themes.forEach {
                beginControlFlow("%S -> %T.Default.let", it.specName, it.className)
                addStatement("require(values.size == %L) { %S }", it.parameters.size + 1, "Invalid ${it.specName} theme specification")
                addStatement("%T(", it.className)
                indent()
                it.parameters.forEachIndexed { index, param ->
                    addStatement("%N = values[%L].orDefault(it.%N),", param, index + 1, param)
                }
                unindent()
                addStatement(")")
                endControlFlow()
            }
            addStatement("else -> throw IllegalArgumentException(%S)", "Invalid theme specification")
            endControlFlow()
        })
        .build()
}

private fun KSClassDeclaration.toModel(): ThemeModel {
    val className = simpleName.asString()
    require(className.endsWith("Theme")) { "Configured theme '$className' must end in 'Theme'" }

    val specName = className.removeSuffix("Theme")
    val constructor = primaryConstructor ?: error("Configured theme '$className' must have a primary constructor")

    val parameters = constructor.parameters.map {
        val name = it.name?.asString() ?: error("All constructor parameters of '$className' must be named")
        val type = ClassName.bestGuess(it.type.resolve().declaration.qualifiedName!!.asString())
        require(type == Double::class.asClassName() || type == COLOR_TYPE) {
            "Unsupported parameter '$className.$name' of type '$type'; only Double and Color are supported"
        }

        name
    }

    return ThemeModel(
        className = toClassName(),
        specName = specName,
        commandName = specName.lowercase() + "theme",
        functionName = specName.lowercase() + "Theme",
        parameters = parameters,
    )
}

private data class ThemeModel(
    val className: ClassName,
    val specName: String,
    val commandName: String,
    val functionName: String,
    val parameters: List<String>,
)
