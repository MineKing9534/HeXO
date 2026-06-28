package de.mineking.hexo.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.core.App
import com.varabyte.kobweb.core.AppGlobals
import com.varabyte.kobweb.core.init.InitKobweb
import com.varabyte.kobweb.core.init.InitKobwebContext
import com.varabyte.kobweb.core.isExporting
import de.mineking.hexo.hds.HdsApiClient
import de.mineking.hexo.hds.socket.SocketIOClient
import de.mineking.hexo.hds.socket.SocketIOOptions
import de.mineking.hexo.hds.socket.connectSocketClient
import de.mineking.hexo.web.pages.NotFoundPage
import de.mineking.hexo.web.web.BuildConfig
import org.jetbrains.compose.web.dom.Main

@Composable
fun rememberHdsApiClient(withSocket: Boolean): HdsApiClient? {
    if (AppGlobals.isExporting) return null
    val host = BuildConfig.API_PROXY ?: return null

    fun createClient(socketClient: SocketIOClient?) = HdsApiClient(host = host, socketClient = socketClient)

    var client by remember { mutableStateOf(if (withSocket) null else createClient(null)) }

    if (withSocket) {
        LaunchedEffect(Unit) {
            val socketClient = connectSocketClient(options = SocketIOOptions.createDefault(host))
            client = createClient(socketClient)
        }
    }

    return client
}

@App
@Composable
fun App(content: @Composable () -> Unit) {
    Main({ classes("h-dvh", "w-screen", "overflow-hidden", "bg-slate-950", "font-sans", "text-slate-100") }) {
        content()
    }
}

@InitKobweb
fun configure(ctx: InitKobwebContext) {
    ctx.router.setErrorPage {
        NotFoundPage()
    }
}
