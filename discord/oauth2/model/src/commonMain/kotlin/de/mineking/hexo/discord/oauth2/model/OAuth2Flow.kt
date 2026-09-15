package de.mineking.hexo.discord.oauth2.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class OAuth2Flow(val value: String) {
    @SerialName("linked_roles")
    LinkedRoles("linked_roles"),
    ;

    companion object {
        fun of(value: String) = entries.firstOrNull { it.value == value }
    }
}
