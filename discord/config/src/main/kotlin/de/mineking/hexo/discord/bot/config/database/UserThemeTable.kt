package de.mineking.hexo.discord.bot.config.database

import de.mineking.hexo.discord.core.DiscordUserIdTable

internal object UserThemeTable : DiscordUserIdTable("user_themes", "discord_user_id") {
    val currentTheme = reference("current_theme", ThemeDataTable.id).nullable()
}
