package de.mineking.hexo.web.watchparty

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.browser.storage.StorageKey
import com.varabyte.kobweb.browser.storage.getItem
import com.varabyte.kobweb.browser.storage.removeItem
import com.varabyte.kobweb.browser.storage.setItem
import de.mineking.hexo.hds.utils.EntityState
import de.mineking.hexo.sync.client.WatchParty
import de.mineking.hexo.sync.client.WatchPartyClient
import de.mineking.hexo.sync.client.createSession
import de.mineking.hexo.sync.common.WatchPartyId
import de.mineking.hexo.web.onSet
import kotlinx.browser.localStorage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private object WatchPartyHostIdKey : StorageKey<WatchPartyId?>("watch_party_host") {
    override fun convertFromString(value: String) = WatchPartyId(value).takeIf { value.isNotEmpty() }
    override fun convertToString(value: WatchPartyId?) = value?.value ?: ""
}

@OptIn(DelicateCoroutinesApi::class)
class WatchPartyController(host: String) {
    var hostWatchParty by mutableStateOf<WatchParty?>(null).interceptWatchPartyHost()
        private set

    var subscribedWatchParty by mutableStateOf<EntityState<WatchParty>?>(null).interceptWatchPartySubscriber()
        private set

    private val watchPartyClient = WatchPartyClient(host)

    init {
        val hostId = localStorage.getItem(WatchPartyHostIdKey)
        if (hostId != null) {
            GlobalScope.launch {
                hostWatchParty = watchPartyClient.connectWatchParty(hostId)
                if (hostWatchParty == null) localStorage.removeItem(WatchPartyHostIdKey)
            }
        }
    }

    suspend fun startHost() {
        closeHost()

        val session = watchPartyClient.createSession()

        hostWatchParty = session
        localStorage.setItem(WatchPartyHostIdKey, session.data.value.id)
    }

    fun closeHost() {
        val session = hostWatchParty
        hostWatchParty = null
        if (session != null) GlobalScope.launch { session.close() }
    }

    fun connect(id: WatchPartyId) {
        subscribedWatchParty = EntityState.Loading
        GlobalScope.launch {
            subscribedWatchParty = when (val watchParty = watchPartyClient.connectWatchParty(id)) {
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

    private fun MutableState<WatchParty?>.interceptWatchPartyHost() = onSet {
        if (it == null) {
            localStorage.removeItem(WatchPartyHostIdKey)
        } else {
            it.onClose { reason ->
                if (reason.closedByServer) value = null
            }
        }
    }

    private fun MutableState<EntityState<WatchParty>?>.interceptWatchPartySubscriber() = onSet {
        val party = it as? EntityState.Data<WatchParty> ?: return@onSet
        party.value.onClose { value = null }
    }
}
