package de.mineking.hexo.discord.oauth2.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class OAuth2Flow {
    @SerialName("linked_roles")
    LinkedRoles,

    @SerialName("web_login")
    WebLogin,
    ;

    val value by lazy { serializer().descriptor.getElementDescriptor(ordinal).serialName }

    companion object {
        fun of(value: String) = entries.firstOrNull { it.value == value }
    }
}
