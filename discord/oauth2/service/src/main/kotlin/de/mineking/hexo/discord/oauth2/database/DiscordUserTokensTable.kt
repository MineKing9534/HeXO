package de.mineking.hexo.discord.oauth2.database

import de.mineking.hexo.discord.core.DiscordUserIdTable
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.datetime.timestamp

internal object DiscordUserTokensTable : DiscordUserIdTable("discord_user_tokens", "discord_id") {
    val accessToken = blob("access_token")
    val refreshToken = blob("refresh_token")
    val expiresAt = timestamp("expires_at")
    val scopes = array("scopes", TextColumnType())
}
