package de.mineking.hexo.launcher.discord

import de.mineking.discord.localization.read
import de.mineking.hexo.board.parse.BoardParser
import de.mineking.hexo.board.parse.RemoteBoardParser
import de.mineking.hexo.board.parse.caching
import de.mineking.hexo.board.parse.focusWinningRows
import de.mineking.hexo.board.parse.or
import de.mineking.hexo.board.render.caching
import de.mineking.hexo.board.render.image.BufferedImageBoardRenderer
import de.mineking.hexo.board.render.image.ErrorMessage
import de.mineking.hexo.board.render.image.ImageSizeLimitExceededException
import de.mineking.hexo.board.render.image.drawExceptionMessages
import de.mineking.hexo.board.render.image.limitSize
import de.mineking.hexo.board.render.image.outputPngBytes
import de.mineking.hexo.board.render.limitConcurrency
import de.mineking.hexo.bot.DiscordLocalization
import de.mineking.hexo.bot.HeXODiscordBot
import de.mineking.hexo.bot.outputBoardAttachment
import de.mineking.hexo.bot.utils.RenderLocalization
import de.mineking.hexo.discord.bot.config.UserThemeRepositoryImpl
import de.mineking.hexo.discord.linkedroles.LinkedRolesUpdateService
import de.mineking.hexo.discord.linkedroles.installLinkedRoleMetadata
import de.mineking.hexo.discord.linkedroles.syncLinkedRolesData
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.hds.implementation.HdsHttpClient
import de.mineking.hexo.launcher.createDatabase
import de.mineking.hexo.launcher.createOAuth2Dependencies
import de.mineking.hexo.launcher.loadConfig
import de.mineking.hexo.launcher.shutdownHook
import de.mineking.hexo.link.AccountLinkRepositoryImpl
import de.mineking.hexo.utils.cache.CacheConfiguration
import de.mineking.hexo.utils.cache.EvictionStrategy
import de.mineking.hexo.utils.cache.entries
import de.mineking.hexo.utils.cache.megabytes
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

suspend fun main() = coroutineScope {
    val config = loadConfig<BotApplicationConfig>()
    val hds = HdsApiClient(
        client = HdsHttpClient.createDefault(),
        repositoryWrapper = CachingRepositoryWrapper,
    )

    val database = config.database.createDatabase()
    val oauth2 = createOAuth2Dependencies(config.oauth2, config.server?.url, database)

    val accountLinks = database?.let { AccountLinkRepositoryImpl(it) }
    val userThemes = database?.let { UserThemeRepositoryImpl(it) }

    val linkedRoles = if (database != null && oauth2 != null && accountLinks != null) {
        LinkedRolesUpdateService(
            accountLinkRepository = accountLinks,
            discordUserAuthenticationRepository = oauth2.tokenRepository,
            discordOAuth2Client = oauth2.client,
            profileRepository = hds.profileRepository,
        )
    } else {
        null
    }

    val parser = createBoardParser(hds)
    val renderer = createBoardRenderer()

    val bot = HeXODiscordBot(
        repositories = hds,
        accountLinkRepository = accountLinks,
        discordUserAuthenticationRepository = oauth2?.tokenRepository,
        userThemeRepository = userThemes,
        notationParser = parser,
        boardRenderer = renderer.outputBoardAttachment("png"),
        publicUrl = config.server?.url,
        token = config.bot.token,
    )

    if (linkedRoles != null) bot.dtk.installLinkedRoleMetadata()

    val linkedRolesSynchronization = if (linkedRoles != null && oauth2 != null && database != null) {
        launch {
            linkedRoles.syncLinkedRolesData(
                oAuth2TokenRepository = oauth2.tokenRepository,
                finishedGameRepository = hds.finishedGameRepository,
                listener = database,
            )
        }
    } else {
        null
    }

    printBanner()

    shutdownHook {
        linkedRolesSynchronization?.cancelAndJoin()
        bot.shutdown()
        hds.close()
        oauth2?.close()
        database?.close()
    }
}

private fun createBoardParser(hds: HdsApiClient) = (RemoteBoardParser(hds) or BoardParser.Default)
    .focusWinningRows()
    .caching(CacheConfiguration(
        sizeLimit = 16.entries,
        expiration = null,
        evictionStrategy = EvictionStrategy.LeastFrequentlyUsed,
    ))

private fun createBoardRenderer() = BufferedImageBoardRenderer.Default
    .limitSize(64.megabytes)
    .drawExceptionMessages { error ->
        when (error) {
            is ImageSizeLimitExceededException -> {
                val context = currentCoroutineContext()[DiscordLocalization] ?: throw error
                val localization = context.localizationManager.read<RenderLocalization>()

                ErrorMessage(
                    title = localization.responseErrorImageTooLargeTitle(context.locale),
                    details = localization.responseErrorImageTooLargeDetails(
                        locale = context.locale,
                        required = error.requiredBytes.formatBytes(),
                        limit = error.limitBytes.formatBytes(),
                    ),
                )
            }
            else -> null
        }
    }
    .limitConcurrency(10)
    .outputPngBytes()
    .caching(CacheConfiguration(
        sizeLimit = 128.megabytes,
        expiration = null,
        evictionStrategy = EvictionStrategy.LeastFrequentlyUsed,
    ))

private fun Long.formatBytes(): String {
    val units = arrayOf("B", "KiB", "MiB", "GiB")
    var value = toDouble()
    var unit = 0

    while (kotlin.math.abs(value) >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }

    if (unit == 0) return "$this ${units[unit]}"

    val rounded = (value * 10).roundToLong() / 10.0
    val formatted = if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    return "$formatted ${units[unit]}"
}

private fun printBanner() {
    println("""
     _    _     __   ______        _____  _                       _          ___  
    | |  | |    \ \ / / __ \      |  __ \(_)                     | |        |__ \ 
    | |__| | ___ \ V / |  | | ___ | |  | |_ ___  ___ ___  _ __ __| |   __   __ ) |
    |  __  |/ _ \ > <| |  | ||___|| |  | | / __|/ __/ _ \| '__/ _` |   \ \ / // / 
    | |  | |  __// . \ |__| |     | |__| | \__ \ (_| (_) | | | (_| |    \ V // /_ 
    |_|  |_|\___/_/ \_\____/      |_____/|_|___/\___\___/|_|  \__,_|     \_/|____|
    """)
}
