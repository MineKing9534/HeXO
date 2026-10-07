package de.mineking.hexo.game.implementation.service

import de.mineking.hexo.database.Migration
import de.mineking.hexo.database.RegisterMigration
import de.mineking.hexo.database.Transaction
import de.mineking.hexo.game.implementation.service.auth.AuthSessionTable
import de.mineking.hexo.game.implementation.service.auth.PrincipalTable
import de.mineking.hexo.game.implementation.service.profile.ProfileStatisticsTable
import de.mineking.hexo.game.implementation.service.profile.ProfileTable

@RegisterMigration
class GameServiceMigration : Migration {
    override suspend fun Transaction.migrate() {
        migrate(ProfileTable)
        migrate(ProfileStatisticsTable)

        migrate(PrincipalTable)
        migrate(AuthSessionTable)

        exec("create extension if not exists pg_trgm;")
    }
}
