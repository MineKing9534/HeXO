package de.mineking.hexo.discord.bot.config.database

import de.mineking.hexo.board.render.image.theme.DefaultTheme
import de.mineking.hexo.discord.core.DiscordUserIdTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.neq

internal object UserThemeTable : DiscordUserIdTable("user_themes", "discord_user_id") {
    val customTheme = reference("custom_theme", ThemeDataTable.id, onDelete = ReferenceOption.CASCADE).nullable()
    val defaultTheme = enumerationByName<DefaultTheme>("default_theme", 5).nullable()

    init {
        check("user_themes_single_theme") {
            customTheme.isNull() neq defaultTheme.isNull()
        }
    }
}
