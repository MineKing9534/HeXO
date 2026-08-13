package de.mineking.hexo.discord.core

import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.LongColumnType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.IdTable

abstract class DiscordUserIdTable(name: String, idColumnName: String = "id") : IdTable<DiscordUserId>(name) {
    final override val id = discordUserIdColumn(idColumnName).entityId()

    final override val primaryKey = PrimaryKey(id)
}

fun Table.discordUserIdColumn(name: String) = registerColumn(name, DiscordUserIdColumnType())

private class DiscordUserIdColumnType : ColumnType<DiscordUserId>() {
    companion object {
        private val DELEGATE = LongColumnType()
    }

    override fun sqlType() = DELEGATE.sqlType()

    override fun valueFromDB(value: Any) = DiscordUserId(DELEGATE.valueFromDB(value))
    override fun valueToDB(value: DiscordUserId?) = DELEGATE.valueToDB(value?.value)
    override fun notNullValueToDB(value: DiscordUserId) = DELEGATE.notNullValueToDB(value.value)
}
