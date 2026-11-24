package de.mineking.hexo.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtProperty

class NonSerializableInterfaceProperty(config: Config) : Rule(
    config,
    "Interface types of serialized properties must be annotated with kotlinx.serialization.Serializable or use a type-use serializer.",
), RequiresAnalysisApi {
    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        super.visitClassOrObject(classOrObject)
        // Serializable objects encode their identity, not their properties.
        if (classOrObject !is KtClass) return
        analyze(classOrObject) {
            if (SERIALIZABLE !in classOrObject.symbol.annotations) return

            for (parameter in classOrObject.primaryConstructorParameters) {
                val property = (parameter.symbol as? KaValueParameterSymbol)?.generatedPrimaryConstructorProperty ?: continue
                checkProperty(parameter, property)
            }
            for (declaration in classOrObject.declarations.filterIsInstance<KtProperty>()) {
                val property = declaration.symbol as? KaPropertySymbol ?: continue
                if (property.hasBackingField && !property.isDelegatedProperty) checkProperty(declaration, property)
            }
        }
    }

    private fun KaSession.checkProperty(declaration: KtNamedDeclaration, property: KaPropertySymbol) {
        if (TRANSIENT in property.annotations) return
        val interfaces = unsupportedInterfaces(property.returnType).distinct()
        for (name in interfaces) {
            report(
                Finding(
                    Entity.atName(declaration),
                    "Property '${declaration.name}' uses interface '$name', which must be annotated with " +
                        "kotlinx.serialization.Serializable or have an explicit type-use serializer.",
                ),
            )
        }
    }

    private fun KaSession.unsupportedInterfaces(type: KaType): List<String> {
        val expandedType = type.fullyExpandedType
        // A type-use serializer handles this type, including its arguments and any alias expansion.
        if (type.hasCustomSerializer() || expandedType.hasCustomSerializer()) return emptyList()
        val classType = expandedType as? KaClassType ?: return emptyList()
        val symbol = classType.symbol as? KaClassSymbol
        val name = classType.classId.asSingleFqName().asString()
        val unsupported = symbol?.classKind == KaClassKind.INTERFACE &&
            SERIALIZABLE !in symbol.annotations && name !in BUILT_IN_COLLECTIONS
        return (if (unsupported) listOf(name) else emptyList()) +
            classType.typeArguments.flatMap { it.type?.let { argument -> unsupportedInterfaces(argument) }.orEmpty() }
    }

    private fun KaType.hasCustomSerializer(): Boolean = annotations[SERIALIZABLE].any { annotation ->
        val serializer = annotation.arguments.firstOrNull { it.name.asString() == "with" }
            ?.expression as? KaAnnotationValue.ClassLiteralValue
        serializer?.classId?.let { it != NOTHING } == true
    }

    private companion object {
        val SERIALIZABLE = ClassId.topLevel(FqName("kotlinx.serialization.Serializable"))
        val TRANSIENT = ClassId.topLevel(FqName("kotlinx.serialization.Transient"))
        val NOTHING = ClassId.topLevel(FqName("kotlin.Nothing"))

        // These interface types have built-in serializers; their type arguments still need checking.
        val BUILT_IN_COLLECTIONS = setOf(
            "kotlin.collections.Collection",
            "kotlin.collections.MutableCollection",
            "kotlin.collections.List",
            "kotlin.collections.MutableList",
            "kotlin.collections.Set",
            "kotlin.collections.MutableSet",
            "kotlin.collections.Map",
            "kotlin.collections.MutableMap",
            "kotlin.collections.Map.Entry",
            "kotlin.collections.MutableMap.MutableEntry",
        )
    }
}
