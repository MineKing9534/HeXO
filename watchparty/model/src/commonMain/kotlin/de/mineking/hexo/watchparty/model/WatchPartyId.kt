package de.mineking.hexo.watchparty.model

import de.mineking.hexo.utils.types.EntityId
import de.mineking.hexo.utils.types.IError
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

@JvmInline
@Serializable
value class WatchPartyId(override val value: String) : EntityId

@JvmInline
@Serializable
value class WatchPartyConnectionId(val value: String) {
    companion object {
        fun generate() = WatchPartyConnectionId(Uuid.random().toString())
    }
}

@Serializable
data object WatchPartyNotFoundError : IError
