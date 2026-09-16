package de.mineking.hexo.discord.linkedroles

import de.mineking.hexo.database.DatabaseNotificationListener
import de.mineking.hexo.database.listen
import de.mineking.hexo.discord.oauth2.OAuth2TokenRepository
import de.mineking.hexo.discord.oauth2.Scope
import de.mineking.hexo.discord.oauth2.model.DiscordTokenStoreDatabaseEvent
import de.mineking.hexo.discord.oauth2.model.OAuth2Flow
import de.mineking.hexo.game.model.game.FinishedGameRepository
import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.game.model.game.rated
import de.mineking.hexo.utils.types.Selector
import de.mineking.hexo.utils.types.limit
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

suspend fun LinkedRolesUpdateService.syncLinkedRolesData(
    oAuth2TokenRepository: OAuth2TokenRepository,
    finishedGameRepository: FinishedGameRepository,
    listener: DatabaseNotificationListener,
): Nothing = coroutineScope {
    launch {
        while (currentCoroutineContext().isActive) {
            syncAllLinkedRolesData(oAuth2TokenRepository)
            delay(1.hours)
        }
    }

    launch {
        delay(1.minutes)
        syncLinkedRolesDataFromGames(finishedGameRepository)
    }

    launch {
        syncLinkedRolesDataFromAuthorization(listener)
    }

    awaitCancellation()
}

@Suppress("TooGenericExceptionCaught")
suspend fun LinkedRolesUpdateService.syncAllLinkedRolesData(repository: OAuth2TokenRepository) {
    try {
        repository.getUsersWithTokens(setOf(Scope.Identify, Scope.RoleConnectionsWrite)).forEach {
            updateLinkedRolesData(it)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.error(e) { "Exception during linked-role reconciliation" }
    }
}

@Suppress("TooGenericExceptionCaught")
suspend fun LinkedRolesUpdateService.syncLinkedRolesDataFromAuthorization(listener: DatabaseNotificationListener): Nothing {
    while (currentCoroutineContext().isActive) {
        try {
            listener.listen<DiscordTokenStoreDatabaseEvent> {
                if (it.cause != OAuth2Flow.LinkedRoles) return@listen
                updateLinkedRolesData(it.user)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Linked-role authorization listener disconnected; reconnecting" }
            delay(5.seconds)
        }
    }

    awaitCancellation()
}

suspend fun LinkedRolesUpdateService.syncLinkedRolesDataFromGames(finishedGameRepository: FinishedGameRepository): Nothing {
    var lastSeenGame = finishedGameRepository.getGlobalHistory(
        Selector
            .limit(1)
            .rated(true),
    ).firstOrNull()?.id ?: GameId("")

    while (currentCoroutineContext().isActive) {
        @Suppress("TooGenericExceptionCaught")
        try {
            val finishedGames = finishedGameRepository.getGlobalHistory(
                Selector.rated(true),
            ).takeWhile { it.id != lastSeenGame }.toList()

            if (finishedGames.isNotEmpty()) {
                val players = finishedGames
                    .flatMapTo(mutableSetOf()) { it.players }
                    .mapNotNull { it.profile?.id }
                    .toSet()

                players.forEach {
                    updateLinkedRolesData(it)
                }

                lastSeenGame = finishedGames.first().id
            }
        } catch (e: Exception) {
            logger.error(e) { "Exception during profile polling" }
        }

        delay(1.minutes)
    }

    awaitCancellation()
}
