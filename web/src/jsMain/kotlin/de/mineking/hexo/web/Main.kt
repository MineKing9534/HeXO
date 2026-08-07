package de.mineking.hexo.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.varabyte.kobweb.core.App
import com.varabyte.kobweb.core.AppGlobals
import com.varabyte.kobweb.core.init.InitKobweb
import com.varabyte.kobweb.core.init.InitKobwebContext
import com.varabyte.kobweb.core.isExporting
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.hds.implementation.socket.SocketIOOptions
import de.mineking.hexo.hds.implementation.socket.connectSocketClient
import de.mineking.hexo.hds.model.HdsRepositoryContainer
import de.mineking.hexo.web.audio.SoundPlayer
import de.mineking.hexo.web.pages.NotFoundPage
import de.mineking.hexo.web.settings.SettingsControllerProvider
import de.mineking.hexo.web.watchparty.WatchPartyController
import de.mineking.hexo.web.web.BuildConfig
import kotlinx.browser.localStorage
import org.jetbrains.compose.web.dom.Main

private val LocalHdsApiRepositories = staticCompositionLocalOf<HdsRepositoryContainer?> { null }
private val LocalSoundPlayer = staticCompositionLocalOf<SoundPlayer> { error("SoundPlayer not initialized!") }
private val LocalWatchPartyController = staticCompositionLocalOf<WatchPartyController> { error("SessionSyncService not initialized!") }

@Composable
fun rememberHdsRepositories() = LocalHdsApiRepositories.current

@Composable
fun rememberSoundPlayer() = LocalSoundPlayer.current

@Composable
fun rememberWatchPartyController() = LocalWatchPartyController.current

@Composable
private fun rememberSharedHdsApiClient(): HdsApiClient? {
    if (AppGlobals.isExporting) return null
    val host = BuildConfig.API_PROXY

    return rememberAsyncResourceState(
        key = host,
        initialState = null,
        load = {
            val socketClient = connectSocketClient(options = SocketIOOptions.createDefault(host))
            HdsApiClient(host = host, socketClient = socketClient)
        },
        dispose = { it?.shutdown() },
    )
}

@App
@Composable
fun App(content: @Composable () -> Unit) {
    SettingsControllerProvider(localStorage) { settingsController ->
        val hdsApiClient = rememberSharedHdsApiClient()
        val soundPlayer = remember(settingsController) { SoundPlayer(settingsController) }
        val watchPartyController = remember { WatchPartyController(BuildConfig.TOOLS_API, settingsController) }

        CompositionLocalProvider(
            LocalHdsApiRepositories provides hdsApiClient,
            LocalSoundPlayer provides soundPlayer,
            LocalWatchPartyController provides watchPartyController,
        ) {
            Main({ classes("h-dvh", "w-screen", "overflow-hidden", "bg-slate-950", "font-sans", "text-slate-100") }) {
                content()
            }
        }
    }
}

@InitKobweb
fun configure(ctx: InitKobwebContext) {
    ctx.router.setErrorPage {
        NotFoundPage()
    }
}
