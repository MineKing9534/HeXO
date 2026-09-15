package de.mineking.hexo.link.database

import de.mineking.hexo.database.Migration
import de.mineking.hexo.database.RegisterMigration
import de.mineking.hexo.database.Transaction

@RegisterMigration
class DiscordLinkMigration : Migration {
    override suspend fun Transaction.migrate() {
        migrate(AccountLinkTable)
    }
}
