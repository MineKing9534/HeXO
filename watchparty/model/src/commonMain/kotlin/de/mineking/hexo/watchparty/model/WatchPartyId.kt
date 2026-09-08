package de.mineking.hexo.watchparty.model

import de.mineking.hexo.utils.types.EntityId
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class WatchPartyId(override val value: String) : EntityId
