package de.mineking.hexo.hds.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("mode")
@Serializable
sealed interface TimeControl {
    @Serializable
    @SerialName("unlimited")
    data object Unlimited : TimeControl

    @Serializable
    @SerialName("turn")
    data class Turn(
        @SerialName("turnTimeMs") val turnTime: Duration,
    ) : TimeControl

    @Serializable
    @SerialName("match")
    data class Match(
        @SerialName("mainTimeMs") val mainTime: Duration,
        @SerialName("incrementMs") val increment: Duration,
    ) : TimeControl
}
