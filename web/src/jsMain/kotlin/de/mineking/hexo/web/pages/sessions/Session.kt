package de.mineking.hexo.web.pages.sessions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.PageContext
import de.mineking.hexo.hds.session.SessionId
import de.mineking.hexo.web.rememberHdsApiClient

@Page("{id}")
@Composable
fun Session(ctx: PageContext) {
    val id = remember { SessionId(ctx.route.params["id"]!!) }

    val hdsClient = rememberHdsApiClient(withSocket = true)
//    val syncClient = remember { SessionSyncClient() }
}
