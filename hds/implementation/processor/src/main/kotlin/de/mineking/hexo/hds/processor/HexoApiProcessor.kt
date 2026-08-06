package de.mineking.hexo.hds.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import kotlin.reflect.KClass

private val SOCKET_EVENT_INTERFACE = ClassName("de.mineking.hexo.hds.implementation.socket", "SocketEvent")
private val SOCKET_REQUEST_INTERFACE = ClassName("de.mineking.hexo.hds.implementation.socket", "SocketRequest")
private val SOCKET_EVENT_NAME_ANNOTATION = ClassName("de.mineking.hexo.hds.implementation.socket", "SocketEventName")
private val SERIALIZABLE_ANNOTATION = ClassName("kotlinx.serialization", "Serializable")

class HexoApiProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {
    private var generated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (generated) return emptyList()
        generateSocketEventRegistry(resolver)
        generateSocketRequestRegistry(resolver)
        generated = true

        return emptyList()
    }

    private fun generateSocketEventRegistry(resolver: Resolver) {
        val socketEventInterface = resolver
            .getClassDeclarationByName(SOCKET_EVENT_INTERFACE.canonicalName)
            ?: error("Could not find marker interface '${SOCKET_EVENT_INTERFACE.canonicalName}'")

        val concreteEvents = socketEventInterface.findNamedSubclasses()

        val dependencies = Dependencies(aggregating = true)
        FileSpec.builder("de.mineking.hexo.hds.socket", "SocketEventRegistry")
            .addType(
                TypeSpec.objectBuilder("SocketEventRegistry")
                    .addModifiers(KModifier.INTERNAL)
                    .addProperty(createNameMappingsProperty("eventNames", SOCKET_EVENT_INTERFACE, concreteEvents))
                    .build(),
            )
            .build()
            .writeTo(codeGenerator, dependencies)
    }

    private fun generateSocketRequestRegistry(resolver: Resolver) {
        val socketRequestInterface = resolver
            .getClassDeclarationByName(SOCKET_REQUEST_INTERFACE.canonicalName)
            ?: error("Could not find marker interface '${SOCKET_REQUEST_INTERFACE.canonicalName}'")

        val concreteRequests = socketRequestInterface.findNamedSubclasses()

        val dependencies = Dependencies(aggregating = true)
        FileSpec.builder("de.mineking.hexo.hds.socket", "SocketRequestRegistry")
            .addType(
                TypeSpec.objectBuilder("SocketRequestRegistry")
                    .addModifiers(KModifier.INTERNAL)
                    .addProperty(createNameMappingsProperty("requestNames", SOCKET_REQUEST_INTERFACE, concreteRequests))
                    .build(),
            )
            .build()
            .writeTo(codeGenerator, dependencies)
    }

    private fun KSClassDeclaration.findNamedSubclasses() = this
        .getAllSealedSubclasses()
        .filterNot { it.classKind == ClassKind.INTERFACE }
        .associateBy { event ->
            val annotations = event.annotations.associateBy { it.annotationType.resolve().declaration.qualifiedName!!.asString() }
            require(SERIALIZABLE_ANNOTATION.canonicalName in annotations) {
                "Expected request type ${event.qualifiedName?.asString()} to be @${SERIALIZABLE_ANNOTATION.canonicalName}"
            }

            val annotation = annotations[SOCKET_EVENT_NAME_ANNOTATION.canonicalName]
            require(annotation != null) {
                "Expected event type ${event.qualifiedName?.asString()} to have the @${SOCKET_EVENT_NAME_ANNOTATION.canonicalName} annotation"
            }

            annotation.arguments[0].value as String
        }

    private fun createNameMappingsProperty(propertyName: String, type: ClassName, entries: Map<String, KSClassDeclaration>): PropertySpec {
        val type = Map::class.asClassName().parameterizedBy(
            KClass::class.asClassName().parameterizedBy(WildcardTypeName.producerOf(type)),
            String::class.asClassName(),
        )

        return PropertySpec.builder(propertyName, type)
            .initializer(
                CodeBlock.builder()
                    .add("mapOf(\n")
                    .apply {
                        entries.forEach { (name, type) ->
                            add("%T::class to %S,\n", type.toClassName(), name)
                        }
                    }
                    .add(")")
                    .build(),
            )
            .build()
    }
}

private fun KSClassDeclaration.getAllSealedSubclasses(): Sequence<KSClassDeclaration> = getSealedSubclasses()
    .flatMap { sequenceOf(it) + it.getAllSealedSubclasses() }
