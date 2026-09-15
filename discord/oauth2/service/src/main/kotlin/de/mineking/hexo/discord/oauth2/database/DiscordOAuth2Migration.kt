package de.mineking.hexo.discord.oauth2.database

import de.mineking.hexo.database.Migration
import de.mineking.hexo.database.RegisterMigration
import de.mineking.hexo.database.Transaction

@RegisterMigration
class DiscordOAuth2Migration : Migration {
    override suspend fun Transaction.migrate() {
        migrate(DiscordUserTokensTable)
    }
}
