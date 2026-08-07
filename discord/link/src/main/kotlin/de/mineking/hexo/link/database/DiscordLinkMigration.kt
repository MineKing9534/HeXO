package de.mineking.hexo.link.database

import de.mineking.hexo.database.Migration
import de.mineking.hexo.database.RegisterMigration
import de.mineking.hexo.database.migrate
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

@RegisterMigration
class DiscordLinkMigration : Migration {
    override fun JdbcTransaction.migrate() {
        migrate(AccountLinkTable, DiscordUserTokensTable)
    }
}
