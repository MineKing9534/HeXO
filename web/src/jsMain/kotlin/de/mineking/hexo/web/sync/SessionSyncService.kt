package de.mineking.hexo.web.sync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.mineking.hexo.hds.utils.EntityState
import de.mineking.hexo.sync.client.SessionSyncClient
import de.mineking.hexo.sync.client.SyncSession
import de.mineking.hexo.sync.common.SessionSyncId
import de.mineking.hexo.web.rememberSessionSyncService
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SessionSyncService(host: String) {
    private val sessionSyncClient = SessionSyncClient(host)

    suspend fun connect(id: SessionSyncId) = sessionSyncClient.connectSession(id)
}

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun rememberSyncSession(id: SessionSyncId): EntityState<SyncSession> {
    val service = rememberSessionSyncService()
    var state by remember { mutableStateOf<EntityState<SyncSession>>(EntityState.Loading) }

    LaunchedEffect(id) {
        state = EntityState.Loading
        val session = service.connect(id)
        state = session
            ?.let { EntityState.Data(it) }
            ?: EntityState.NotFound
    }

    DisposableEffect(Unit) {
        onDispose {
            val state = state
            if (state !is EntityState.Data) return@onDispose

            GlobalScope.launch { state.value.close() }
        }
    }

    return state
}
