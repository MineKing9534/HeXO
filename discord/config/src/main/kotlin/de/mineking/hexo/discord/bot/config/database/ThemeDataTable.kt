package de.mineking.hexo.discord.bot.config.database

import de.mineking.hexo.board.render.image.theme.DefaultTheme
import de.mineking.hexo.database.NanoId
import de.mineking.hexo.database.NanoIdTable
import de.mineking.hexo.discord.bot.config.CustomThemeId
import de.mineking.hexo.discord.bot.config.ThemeOverrideValue
import de.mineking.hexo.discord.core.discordUserIdColumn
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.json.json

internal object ThemeDataTable : NanoIdTable<CustomThemeId>("theme_data") {
    val owner = discordUserIdColumn("owner_id")
    val name = text("name")

    val base = enumerationByName<DefaultTheme>("base", 5)
    val overrides = json<Map<String, ThemeOverrideValue>>("overrides", Json)

    init {
        uniqueIndex("theme_data_owner_name_unique", owner, name)
    }

    override fun NanoId.wrapId() = CustomThemeId(this)
    override fun CustomThemeId.unwrapId() = value
}
