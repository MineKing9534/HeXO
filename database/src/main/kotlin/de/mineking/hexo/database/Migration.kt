package de.mineking.hexo.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class RegisterMigration

interface Migration {
    fun JdbcTransaction.migrate()
}

context(_: Migration)
fun JdbcTransaction.migrate(vararg tables: Table) {
    val statements = MigrationUtils.statementsRequiredForDatabaseMigration(tables = tables, withLogs = false)

    if (statements.isEmpty()) {
        logger.debug { "No migration required" }
    } else {
        statements.forEach {
            exec(it)
        }
    }
}
