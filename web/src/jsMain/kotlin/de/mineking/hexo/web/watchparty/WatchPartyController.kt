package de.mineking.hexo.web.watchparty

import androidx.compose.runtime.Composable
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
import de.mineking.hexo.web.interceptSet
import de.mineking.hexo.web.rememberAsyncResourceState
import de.mineking.hexo.web.rememberWatchPartyController
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
    private fun MutableState<WatchParty?>.interceptWatchPartyHost() = interceptSet {
        if (it == null) {
            localStorage.removeItem(WatchPartyHostIdKey)
        } else {
            it.onClose { value = null }
        }
    }

    var hostWatchParty by mutableStateOf<WatchParty?>(null).interceptWatchPartyHost()
        private set

    var subscribedWatchParty by mutableStateOf<WatchParty?>(null).interceptSet { it?.onClose { value = null } }
        private set

    private val watchPartyClient = WatchPartyClient(host)

    init {
        val hostId = localStorage.getItem(WatchPartyHostIdKey)
        if (hostId != null) {
            GlobalScope.launch {
                hostWatchParty = watchPartyClient.connectSession(hostId)
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
        if (session != null) GlobalScope.launch { session.close() }
    }

    suspend fun connect(id: WatchPartyId) = watchPartyClient
        .connectSession(id)
        .also { subscribedWatchParty = it }
}

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun rememberWatchParty(id: WatchPartyId): EntityState<WatchParty> {
    val watchPartyController = rememberWatchPartyController()
    val state = rememberAsyncResourceState(
        key = id,
        initialState = EntityState.Loading,
        load = {
            watchPartyController.connect(id)
                ?.let { EntityState.Data(it) }
                ?: EntityState.NotFound
        },
        dispose = {
            if (it is EntityState.Data) {
                it.value.close()
            }
        },
    )

    return state
}
