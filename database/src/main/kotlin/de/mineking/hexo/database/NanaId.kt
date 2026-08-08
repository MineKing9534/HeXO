package de.mineking.hexo.database

import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import io.viascom.nanoid.NanoId as NanoIdGenerator

@JvmInline
value class NanoId(val value: String)

data class NanoIdConfig(
    val size: Int,
    val alphabet: String,
) {
    companion object {
        val Default = NanoIdConfig(
            size = 8,
            alphabet = "0123456789abcdefghijklmnopqrstuvwxyz",
        )
    }
}

abstract class NanoIdTable<T : Any>(
    name: String,
    idColumnName: String = "id",
    private val config: NanoIdConfig = NanoIdConfig.Default,
) : IdTable<T>(name) {
    protected abstract fun NanoId.wrapId(): T
    protected abstract fun T.unwrapId(): NanoId

    final override val id = nanoIdColumn(idColumnName)
        .clientDefault { NanoId(NanoIdGenerator.generate(config.size, config.alphabet)) }
        .transform(wrap = { it.wrapId() }, unwrap = { it.unwrapId() })
        .entityId()

    final override val primaryKey = PrimaryKey(id)
}

fun Table.nanoIdColumn(name: String) = registerColumn(name, NanoIdColumnType())

private class NanoIdColumnType : ColumnType<NanoId>() {
    override fun sqlType() = currentDialect.dataTypeProvider.textType()

    override fun valueFromDB(value: Any) = NanoId(value.toString())
    override fun valueToDB(value: NanoId?) = value?.value
}
