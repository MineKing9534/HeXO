@file:Suppress("MatchingDeclarationName", "Filename")

package de.mineking.hexo.hds.implementation.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.jsonObject

internal abstract class JsonUnwrapSerializer<T>(
    delegate: KSerializer<T>,
    private val name: String,
) : JsonTransformingSerializer<T>(delegate) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return element.jsonObject[name] ?: element
    }
}
