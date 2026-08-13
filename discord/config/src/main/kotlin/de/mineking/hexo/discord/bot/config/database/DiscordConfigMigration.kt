package de.mineking.hexo.discord.bot.config.database

import de.mineking.hexo.database.Migration
import de.mineking.hexo.database.RegisterMigration
import de.mineking.hexo.database.migrate
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

@RegisterMigration
class DiscordConfigMigration : Migration {
    override fun JdbcTransaction.migrate() {
        migrate(ThemeDataTable, UserThemeTable)
    }
}
