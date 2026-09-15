package de.mineking.hexo.web.pages.discord

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import de.mineking.hexo.discord.oauth2.client.DiscordOAuth2ApiClient
import de.mineking.hexo.web.web.BuildConfig

@Composable
fun rememberDiscordOAuth2ApiClient(): DiscordOAuth2ApiClient {
    val client = remember { DiscordOAuth2ApiClient(apiUrl = BuildConfig.HMD_API_URL) }
    DisposableEffect(client) {
        onDispose { client.close() }
    }

    return client
}
