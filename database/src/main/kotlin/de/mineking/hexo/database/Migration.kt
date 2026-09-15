package de.mineking.hexo.database

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class RegisterMigration

interface Migration {
    suspend fun Transaction.migrate()
}
