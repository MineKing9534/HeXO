package de.mineking.hexo.game.implementation.service.auth

import de.mineking.hexo.game.implementation.protocol.AuthSessionId
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.uuid.Uuid

internal object AuthSessionTable : IdTable<AuthSessionId>("auth_sessions") {
    override val id = uuid("id")
        .autoGenerate()
        .transform({ AuthSessionId(it.toString()) }, { Uuid.parse(it.value) })
        .entityId()

    val refreshId = uuid("refresh_id").autoGenerate()

    val lastUsedAt = timestamp("last_used_at").defaultExpression(CurrentTimestamp)
    val principalId = reference("principal_id", PrincipalTable.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(id)
}
