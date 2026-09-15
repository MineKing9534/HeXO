@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.launcher

import de.mineking.hexo.database.DatabaseManager
import de.mineking.hexo.database.r2dbc.vendors.R2dbcPostgresDatabaseManager
import de.mineking.hexo.database.runMigrations
import de.mineking.hexo.discord.oauth2.DiscordOAuth2Client
import de.mineking.hexo.discord.oauth2.OAuth2TokenRepository
import de.mineking.hexo.discord.oauth2.OAuth2TokenRepositoryImpl
import de.mineking.hexo.discord.oauth2.database.AESTokenTransform
import kotlinx.coroutines.runBlocking
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64

suspend fun DatabaseConfig?.createDatabase(): DatabaseManager? = this
    ?.let { R2dbcPostgresDatabaseManager(it.url) }
    ?.also { it.runMigrations() }

data class OAuth2Dependencies(
    val client: DiscordOAuth2Client,
    val tokenRepository: OAuth2TokenRepository,
) : AutoCloseable {
    override fun close() = client.close()
}

fun createOAuth2Dependencies(
    config: DiscordOAuth2Config?,
    publicServerUrl: String?,
    database: DatabaseManager?,
): OAuth2Dependencies? {
    if (config == null || publicServerUrl == null || database == null) return null

    val client = DiscordOAuth2Client(
        clientId = config.clientId,
        clientSecret = config.clientSecret,
        redirectUri = "$publicServerUrl/oauth2/callback",
    )
    val tokenRepository = OAuth2TokenRepositoryImpl(
        database = database,
        discordOAuth2Client = client,
        transform = AESTokenTransform(SecretKeySpec(Base64.decode(config.encryptionKey), "AES")),
    )

    return OAuth2Dependencies(client, tokenRepository)
}

fun shutdownHook(block: suspend () -> Unit) {
    Runtime.getRuntime().addShutdownHook(Thread {
        runBlocking {
            block()
        }
    })
}
