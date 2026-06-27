@file:OptIn(ExperimentalContracts::class)

package de.mineking.hexo.launcher

import de.mineking.hexo.board.parse.BoardParser
import de.mineking.hexo.board.parse.cached
import de.mineking.hexo.board.parse.focusWinningRows
import de.mineking.hexo.board.render.cached
import de.mineking.hexo.board.render.image.BufferedImageBoardRenderer
import de.mineking.hexo.board.render.image.outputPngBytes
import de.mineking.hexo.bot.HeXODiscordBot
import de.mineking.hexo.bot.outputBoardAttachment
import de.mineking.hexo.bot.utils.LinkedRolesUpdateService
import de.mineking.hexo.bot.web.LinkedRolesRedirectWebService
import de.mineking.hexo.hds.HdsApiClient
import de.mineking.hexo.hds.caching.CachingRepositoryWrapper
import de.mineking.hexo.launcher.web.OAUth2CallbackWebService
import de.mineking.hexo.link.AccountLinkRepository
import de.mineking.hexo.link.HexoDatabaseManager
import de.mineking.hexo.link.oauth2.AESTokenTransform
import de.mineking.hexo.link.oauth2.DiscordOAuth2Client
import de.mineking.hexo.link.oauth2.DiscordUserAuthenticationRepository
import de.mineking.hexo.link.oauth2.OAuth2Tokens
import de.mineking.hexo.server.HttpServer
import de.mineking.hexo.sever.service.WebService
import de.mineking.hexo.sync.server.SessionSyncWebService
import javax.crypto.spec.SecretKeySpec
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.contract
import kotlin.io.encoding.Base64

fun main() {
    val config = Config.fromEnvironment()
    val client = HdsApiClient(socketClient = null, repositoryWrapper = CachingRepositoryWrapper())

    val database = config.database?.let { HexoDatabaseManager(it.url) }
    val discordOAuth2Client = config.createDiscordOAuth2Client()
    val discordUserAuthenticationRepository = config.createDiscordUserAuthenticationRepository(database, discordOAuth2Client)
    val accountLinkRepository = database?.let { AccountLinkRepository(it) }
    val linkedRoles = createLinkedRoles(client, accountLinkRepository, discordUserAuthenticationRepository)

    val parser = BoardParser.createWithHdsSupport(client).focusWinningRows().cached()
    val pngRenderer = BufferedImageBoardRenderer.Default.outputPngBytes().cached()

    val bot = HeXODiscordBot(
        client = client,
        accountLinkRepository = accountLinkRepository,
        discordUserAuthenticationRepository = discordUserAuthenticationRepository,
        notationParser = parser,
        boardRenderer = pngRenderer.outputBoardAttachment("png"),
        publicUrl = config.server?.url,
        token = config.bot.token,
    )

    val oauth2Service = discordUserAuthenticationRepository?.createOAuth2WebService {
        linkedRoles?.scheduleLinkedRoleDataUpdate(it)
    }
    val linkedRolesService = linkedRoles?.let {
        oauth2Service!! // Required because smart casting isn't smart enough
        LinkedRolesRedirectWebService(oauth2Service::generateState, discordOAuth2Client)
    }

    val httpServer = config.startHttpServer(oauth2Service, linkedRolesService, SessionSyncWebService())
    installShutdownHook(bot, httpServer)
    printBanner()
}

private fun Config.createDiscordOAuth2Client(): DiscordOAuth2Client? {
    val oauth2 = oauth2 ?: return null
    val server = server ?: return null

    return DiscordOAuth2Client(
        clientId = oauth2.clientId,
        clientSecret = oauth2.clientSecret,
        redirectUri = "${server.url}/oauth2/callback",
    )
}

@OptIn(ExperimentalExtendedContracts::class)
private fun DiscordUserAuthenticationRepository?.createOAuth2WebService(callback: (OAuth2Tokens) -> Unit): OAUth2CallbackWebService? {
    contract {
        (this@createOAuth2WebService != null) implies returnsNotNull()
    }

    if (this == null) return null
    return OAUth2CallbackWebService(this, callback)
}

private fun Config.createDiscordUserAuthenticationRepository(
    database: HexoDatabaseManager?,
    discordOAuth2Client: DiscordOAuth2Client?,
): DiscordUserAuthenticationRepository? {
    contract {
        returnsNotNull() implies (discordOAuth2Client != null)
    }

    val oauth2 = oauth2 ?: return null
    if (database == null || discordOAuth2Client == null) return null

    return DiscordUserAuthenticationRepository(
        database = database,
        discordOAuth2Client = discordOAuth2Client,
        transform = AESTokenTransform(SecretKeySpec(Base64.decode(oauth2.encryptionKey), "AES")),
    )
}

private fun createLinkedRoles(
    client: HdsApiClient,
    accountLinkRepository: AccountLinkRepository?,
    discordUserAuthenticationRepository: DiscordUserAuthenticationRepository?,
): LinkedRolesUpdateService? {
    contract {
        returnsNotNull() implies (accountLinkRepository != null)
        returnsNotNull() implies (discordUserAuthenticationRepository != null)
    }

    if (accountLinkRepository == null || discordUserAuthenticationRepository == null) return null

    return LinkedRolesUpdateService(
        accountLinkRepository = accountLinkRepository,
        discordUserAuthenticationRepository = discordUserAuthenticationRepository,
        finishedGameRepository = client.finishedGameRepository,
        profileRepository = client.profileRepository,
    )
}

private fun Config.startHttpServer(vararg services: WebService?): HttpServer? {
    val server = server ?: return null
    return HttpServer(
        services = services.toList().filterNotNull(),
        port = server.port,
    ).also { it.start(wait = false) }
}

private fun installShutdownHook(bot: HeXODiscordBot, httpServer: HttpServer?) {
    Runtime.getRuntime().addShutdownHook(
        Thread {
            httpServer?.stop()
            bot.shutdown()
        },
    )
}

private fun printBanner() {
    println("""
     _    _     __   ______      ____        _          __ 
    | |  | |    \ \ / / __ \    |  _ \      | |        /_ |
    | |__| | ___ \ V / |  | |___| |_) | ___ | |_  __   _| |
    |  __  |/ _ \ > <| |  | |___|  _ < / _ \| __| \ \ / / |
    | |  | |  __// . \ |__| |   | |_) | (_) | |_   \ V /| |
    |_|  |_|\___/_/ \_\____/    |____/ \___/ \__|   \_/ |_|
    
    """.trimIndent())
}
