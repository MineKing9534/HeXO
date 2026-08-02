package de.mineking.hexo.web.watchparty

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.browser.storage.StorageKey
import com.varabyte.kobweb.browser.storage.getItem
import com.varabyte.kobweb.browser.storage.setItem
import de.mineking.hexo.hds.model.EntityState
import de.mineking.hexo.sync.client.WatchParty
import de.mineking.hexo.sync.client.WatchPartyClient
import de.mineking.hexo.sync.client.createWatchParty
import de.mineking.hexo.sync.common.WatchPartyId
import de.mineking.hexo.web.onSet
import de.mineking.hexo.web.settings.SettingsController
import de.mineking.hexo.web.settings.SettingsKey
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

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

        val session = watchPartyClient.createWatchParty(detachOnClose = true, connectionId = connectionId)
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
            val current = value
            GlobalScope.launch { current?.close() }
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
            GlobalScope.launch { current.value.close() }
        }

        if (new is EntityState.Data) {
            new.value.onClose { reason ->
                if (reason.closedByServer && value === new) value = EntityState.NotFound
            }
        }
    }
}

private object WatchPartyConnectionIdKey : StorageKey<String>("watchparty_connection_id") {
    override fun convertToString(value: String) = value
    override fun convertFromString(value: String) = value
}

private fun getOrCreateConnectionId(): String {
    val existing = window.sessionStorage.getItem(WatchPartyConnectionIdKey)
    if (existing != null) return existing

    return Uuid.random().toString()
        .also { window.sessionStorage.setItem(WatchPartyConnectionIdKey, it) }
}
