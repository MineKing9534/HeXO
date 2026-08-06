package de.mineking.hexo.link.database

import de.mineking.hexo.hds.model.profile.ProfileId
import de.mineking.hexo.link.DiscordUserId
import org.jetbrains.exposed.v1.core.dao.id.IdTable

internal object AccountLinkTable : IdTable<DiscordUserId>("linked_accounts") {
    override val id = long("discord_id").transform({ DiscordUserId(it) }, { it.value }).entityId()
    val linkedProfileId = char("linked_profile_id", 24).transform({ ProfileId(it) }, { it.value }).uniqueIndex()

    override val primaryKey = PrimaryKey(id)
}
