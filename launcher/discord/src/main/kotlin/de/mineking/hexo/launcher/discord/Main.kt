package de.mineking.hexo.launcher.discord

import de.mineking.hexo.board.parse.BoardParser
import de.mineking.hexo.board.parse.RemoteBoardParser
import de.mineking.hexo.board.parse.cached
import de.mineking.hexo.board.parse.focusWinningRows
import de.mineking.hexo.board.parse.or
import de.mineking.hexo.board.render.caching
import de.mineking.hexo.board.render.image.BufferedImageBoardRenderer
import de.mineking.hexo.board.render.image.outputPngBytes
import de.mineking.hexo.bot.HeXODiscordBot
import de.mineking.hexo.bot.outputBoardAttachment
import de.mineking.hexo.discord.bot.config.UserThemeRepositoryImpl
import de.mineking.hexo.discord.linkedroles.LinkedRolesUpdateService
import de.mineking.hexo.discord.linkedroles.installLinkedRoleMetadata
import de.mineking.hexo.discord.linkedroles.syncLinkedRolesData
import de.mineking.hexo.game.model.caching.CachingRepositoryWrapper
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.hds.implementation.HdsHttpClient
import de.mineking.hexo.launcher.createDatabase
import de.mineking.hexo.launcher.createOAuth2Dependencies
import de.mineking.hexo.launcher.loadConfig
import de.mineking.hexo.launcher.shutdownHook
import de.mineking.hexo.link.AccountLinkRepositoryImpl
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun main() = coroutineScope {
    val config = loadConfig<BotApplicationConfig>()
    val hds = HdsApiClient(
        client = HdsHttpClient.createDefault(),
        repositoryWrapper = CachingRepositoryWrapper(),
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

    val parser = (RemoteBoardParser(hds) or BoardParser.Default)
        .focusWinningRows()
        .cached()

    val renderer = BufferedImageBoardRenderer.Default
        .outputPngBytes()
        .caching()

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
