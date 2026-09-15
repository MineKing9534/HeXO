package de.mineking.hexo.discord.bot.config.database

import de.mineking.hexo.database.Migration
import de.mineking.hexo.database.RegisterMigration
import de.mineking.hexo.database.Transaction

@RegisterMigration
class DiscordConfigMigration : Migration {
    override suspend fun Transaction.migrate() {
        migrate(ThemeDataTable)
        migrate(UserThemeTable)
    }
}
