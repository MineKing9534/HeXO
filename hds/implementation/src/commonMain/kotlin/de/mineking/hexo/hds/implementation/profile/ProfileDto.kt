@file:OptIn(ExperimentalSerializationApi::class)

package de.mineking.hexo.hds.implementation.profile

import de.mineking.hexo.hds.implementation.utils.JsonUnwrapSerializer
import de.mineking.hexo.hds.model.Instant
import de.mineking.hexo.hds.model.profile.ProfileId
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KeepGeneratedSerializer
import kotlinx.serialization.Serializable

internal object ProfileSerializer : JsonUnwrapSerializer<ProfileDto>(ProfileDto.generatedSerializer(), "user")

@KeepGeneratedSerializer
@Serializable(ProfileSerializer::class)
internal data class ProfileDto(
    val id: ProfileId,
    val username: String,
    val image: String?,
    val registeredAt: Instant,
    val lastActiveAt: Instant,
)
