package de.mineking.hexo.web.pages.watchparty

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.browser.storage.StorageKey
import com.varabyte.kobweb.browser.storage.getItem
import com.varabyte.kobweb.browser.storage.setItem
import de.mineking.hexo.utils.types.EntityState
import de.mineking.hexo.watchparty.client.WatchParty
import de.mineking.hexo.watchparty.client.WatchPartyClient
import de.mineking.hexo.watchparty.client.createAndConnectWatchParty
import de.mineking.hexo.watchparty.model.WatchPartyConnectionId
import de.mineking.hexo.watchparty.model.WatchPartyId
import de.mineking.hexo.web.onSet
import de.mineking.hexo.web.settings.SettingsController
import de.mineking.hexo.web.settings.SettingsKey
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
class WatchPartyController(host: String, private val settingsController: SettingsController) {
    private val initialize = CompletableDeferred<Unit>()
    private val connectionId = getOrCreateConnectionId()

    var hostWatchParty by mutableStateOf<WatchParty?>(null).interceptWatchPartyHost()
        private set

    var subscribedWatchParty by mutableStateOf<EntityState<WatchParty>?>(null).interceptWatchPartySubscriber()
        private set

    val currentWatchParty get() = hostWatchParty
        ?: (subscribedWatchParty as? EntityState.Data)?.value

    private val watchPartyClient = WatchPartyClient(host, coroutineScope = GlobalScope)

    init {
        GlobalScope.launch {
            settingsController[SettingsKey.HostWatchPartyId].collect {
                if (it != hostWatchParty?.id) {
                    hostWatchParty = it?.let {
                        watchPartyClient.connectWatchParty(it, detachOnClose = true, connectionId = connectionId)
                    }
                }
                initialize.complete(Unit)
            }
        }
    }

    suspend fun awaitReady() {
        initialize.await()
    }

    suspend fun startHost() {
        closeHost()

        val session = watchPartyClient.createAndConnectWatchParty(detachOnClose = true, connectionId = connectionId)
        hostWatchParty = session
    }

    fun closeHost() {
        hostWatchParty = null
    }

    private fun connect(id: WatchPartyId) {
        subscribedWatchParty = EntityState.Loading
        GlobalScope.launch {
            subscribedWatchParty = when (
                val watchParty = watchPartyClient.connectWatchParty(
                    id = id,
                    detachOnClose = false,
                    connectionId = connectionId,
                )
            ) {
                null -> EntityState.NotFound
                else -> EntityState.Data(watchParty)
            }
        }
    }

    @Composable
    fun rememberWatchParty(id: WatchPartyId): EntityState<WatchParty> {
        DisposableEffect(id) {
            connect(id)
            onDispose { subscribedWatchParty = null }
        }

        return subscribedWatchParty ?: EntityState.NotFound
    }

    private fun MutableState<WatchParty?>.interceptWatchPartyHost() = onSet { new ->
        if (new == null) {
            value?.close()
            settingsController[SettingsKey.HostWatchPartyId].value = null
        } else {
            settingsController[SettingsKey.HostWatchPartyId].value = new.id
            new.onClose { reason ->
                if (reason.closedByServer && value === new) value = null
            }
        }
    }

    private fun MutableState<EntityState<WatchParty>?>.interceptWatchPartySubscriber() = onSet { new ->
        val current = value
        if (current is EntityState.Data) {
            current.value.close()
        }

        if (new is EntityState.Data) {
            new.value.onClose { reason ->
                if (reason.closedByServer && value === new) value = EntityState.NotFound
            }
        }
    }
}

private object WatchPartyConnectionIdKey : StorageKey<WatchPartyConnectionId>("watchparty_connection_id") {
    override fun convertToString(value: WatchPartyConnectionId) = value.value
    override fun convertFromString(value: String) = WatchPartyConnectionId(value)
}

private fun getOrCreateConnectionId(): WatchPartyConnectionId {
    val existing = window.sessionStorage.getItem(WatchPartyConnectionIdKey)
    if (existing != null) return existing

    return WatchPartyConnectionId.generate()
        .also { window.sessionStorage.setItem(WatchPartyConnectionIdKey, it) }
}
