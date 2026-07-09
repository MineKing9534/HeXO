package de.mineking.hexo.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.varabyte.kobweb.core.App
import com.varabyte.kobweb.core.AppGlobals
import com.varabyte.kobweb.core.init.InitKobweb
import com.varabyte.kobweb.core.init.InitKobwebContext
import com.varabyte.kobweb.core.isExporting
import de.mineking.hexo.hds.HdsApiClient
import de.mineking.hexo.hds.socket.SocketIOOptions
import de.mineking.hexo.hds.socket.connectSocketClient
import de.mineking.hexo.web.audio.SoundPlayer
import de.mineking.hexo.web.pages.NotFoundPage
import de.mineking.hexo.web.sync.SessionSyncService
import de.mineking.hexo.web.web.BuildConfig
import org.jetbrains.compose.web.dom.Main

private val LocalHdsApiClient = staticCompositionLocalOf<HdsApiClient?> { null }
private val LocalSoundPlayer = staticCompositionLocalOf<SoundPlayer> { error("SoundPlayer not initialized!") }
private val LocalSessionSyncService = staticCompositionLocalOf<SessionSyncService> { error("SessionSyncService not initialized!") }

@Composable
fun rememberHdsApiClient() = LocalHdsApiClient.current

@Composable
fun rememberSoundPlayer() = LocalSoundPlayer.current

@Composable
fun rememberSessionSyncService() = LocalSessionSyncService.current

@Composable
private fun rememberSharedHdsApiClient(): HdsApiClient? {
    if (AppGlobals.isExporting) return null
    val host = BuildConfig.API_PROXY ?: return null

    var client by remember { mutableStateOf<HdsApiClient?>(null) }

    LaunchedEffect(host) {
        val socketClient = connectSocketClient(options = SocketIOOptions.createDefault(host))
        client = HdsApiClient(host = host, socketClient = socketClient)
    }

    val currentClient by rememberUpdatedState(client)
    DisposableEffect(Unit) {
        onDispose {
            currentClient?.shutdown()
        }
    }

    return client
}

@App
@Composable
fun App(content: @Composable () -> Unit) {
    val hdsApiClient = rememberSharedHdsApiClient()
    val soundPlayer = remember { SoundPlayer() }
    val sessionSyncService = remember { SessionSyncService("http://localhost:1234") } // TODO

    CompositionLocalProvider(
        LocalHdsApiClient provides hdsApiClient,
        LocalSoundPlayer provides soundPlayer,
        LocalSessionSyncService provides sessionSyncService,
    ) {
        Main({ classes("h-dvh", "w-screen", "overflow-hidden", "bg-slate-950", "font-sans", "text-slate-100") }) {
            content()
        }
    }
}

@InitKobweb
fun configure(ctx: InitKobwebContext) {
    ctx.router.setErrorPage {
        NotFoundPage()
    }
}
