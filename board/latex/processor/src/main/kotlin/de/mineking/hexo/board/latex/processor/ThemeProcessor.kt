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
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

private const val OUTPUT_PACKAGE = "de.mineking.hexo.board.latex"

private val OPT_IN_ANNOTATION = ClassName("kotlin", "OptIn")
private val INTERNAL_HEXO_LATEX_ANNOTATION = ClassName("de.mineking.hexo.board.latex", "InternalHexoLatexApi")
private val THEME_FACTORY_INTERFACE = ClassName("de.mineking.hexo.board.latex", "ThemeFactory")
private val THEME_REGISTRY = ClassName("de.mineking.hexo.board.latex", "HexoThemeRegistry")

private val LATEX_COMMAND_ANNOTATION = ClassName("de.mineking.kotlinlatex", "LatexCommand")
private val LATEX_TYPE = ClassName("de.mineking.kotlinlatex", "Latex")
private val LATEX_FUNCTION = MemberName("de.mineking.kotlinlatex", "latex")
private val THEME_TYPE = ClassName("de.mineking.hexo.board.render.image.theme", "Theme")
private val COLOR_TYPE = ClassName("de.mineking.hexo.board.render.image.theme", "Color")

class ThemeProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val registerThemesCommandName: String,
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
            .addAnnotation(
                AnnotationSpec.builder(OPT_IN_ANNOTATION)
                    .addMember("%T::class", INTERNAL_HEXO_LATEX_ANNOTATION)
                    .build(),
            )
            .apply {
                themeModels.forEach {
                    addFunction(createThemeCommand(it))
                }

                themeModels.forEach {
                    addType(createThemeFactory(it))
                }

                addFunction(createRegisterThemesCommand(themeModels))
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
        .addCode(buildCodeBlock {
            addStatement("return themeSpec(")
            indent()
            addStatement("%S,", theme.specName)
            addStatement("listOf(${theme.parameters.joinToString()}),")
            unindent()
            addStatement(")")
        })
        .build()

    private fun createThemeFactory(theme: ThemeModel) = TypeSpec.objectBuilder(theme.factoryName)
        .addSuperinterface(THEME_FACTORY_INTERFACE)
        .addFunction(
            FunSpec.builder("createTheme")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("args", List::class.parameterizedBy(String::class))
                .returns(THEME_TYPE)
                .addCode(buildCodeBlock {
                    addStatement("require(args.size == %L) { %S }", theme.parameters.size, "Invalid ${theme.specName} theme specification")
                    beginControlFlow("return %T.Default.let", theme.className)
                    addStatement("%T(", theme.className)
                    indent()
                    theme.parameters.forEachIndexed { index, param ->
                        addStatement("%N = args[%L].orDefault(it.%N),", param, index, param)
                    }
                    unindent()
                    addStatement(")")
                    endControlFlow()
                })
                .build(),
        )
        .build()

    private fun createRegisterThemesCommand(themes: List<ThemeModel>) = FunSpec.builder("registerThemes")
        .addAnnotation(
            AnnotationSpec.builder(LATEX_COMMAND_ANNOTATION)
                .addMember("%S", registerThemesCommandName)
                .build(),
        )
        .addCode(buildCodeBlock {
            themes.forEach {
                addStatement("%T.registerTheme(%S, %T)", THEME_REGISTRY, it.specName, it.factoryName)
            }
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
        factoryName = ClassName(OUTPUT_PACKAGE, "${className}Factory"),
        parameters = parameters,
    )
}

private data class ThemeModel(
    val className: ClassName,
    val specName: String,
    val commandName: String,
    val functionName: String,
    val factoryName: ClassName,
    val parameters: List<String>,
)
