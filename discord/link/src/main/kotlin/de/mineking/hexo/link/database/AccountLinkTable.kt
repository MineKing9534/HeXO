package de.mineking.hexo.link.database

import de.mineking.hexo.discord.core.DiscordUserIdTable
import de.mineking.hexo.hds.model.profile.ProfileId

internal object AccountLinkTable : DiscordUserIdTable("linked_accounts", "discord_id") {
    val linkedProfileId = char("linked_profile_id", 24)
        .transform({ ProfileId(it) }, { it.value })
        .uniqueIndex("hds_profile_unique_index")
}
