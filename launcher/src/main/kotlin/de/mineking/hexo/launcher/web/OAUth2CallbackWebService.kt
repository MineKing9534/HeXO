package de.mineking.hexo.launcher.web

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.expireAfterWrite
import de.mineking.hexo.link.oauth2.DiscordUserAuthenticationRepository
import de.mineking.hexo.link.oauth2.OAuth2Tokens
import de.mineking.hexo.server.html.CardColorScheme
import de.mineking.hexo.server.html.statusPage
import de.mineking.hexo.sever.service.WebService
import io.ktor.http.HttpStatusCode
import io.ktor.server.html.respondHtml
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import kotlinx.html.p
import kotlinx.html.span
import java.security.SecureRandom
import kotlin.time.Duration.Companion.minutes

class OAUth2CallbackWebService(
    private val discordUserAuthenticationRepository: DiscordUserAuthenticationRepository,
    private val callback: (OAuth2Tokens) -> Unit,
) : WebService() {
    private val random = SecureRandom()
    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(5.minutes)
        .build<String, Unit>()

    fun generateState(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)

        val state = bytes.toHexString()
        cache.put(state, Unit)
        return state
    }

    private fun verifyState(state: String): Boolean {
        val result = cache.getIfPresent(state) != null
        cache.invalidate(state)
        return result
    }

    override fun Route.registerRoutes() {
        get("/oauth2/callback") {
            val code = call.queryParameters["code"]
            val state = call.queryParameters["state"]

            if (code == null || state == null) {
                call.respondFailedHtml(HttpStatusCode.BadRequest)
                return@get
            }

            if (!verifyState(state)) {
                call.respondFailedHtml(HttpStatusCode.Unauthorized)
                return@get
            }

            val tokens = discordUserAuthenticationRepository.authenticateUser(code)

            if (tokens == null) {
                call.respondFailedHtml(HttpStatusCode.Unauthorized)
            } else {
                call.respondSuccessHtml()
                callback(tokens)
            }
        }
    }

    private suspend fun RoutingCall.respondSuccessHtml() {
        respondHtml {
            statusPage(CardColorScheme.Success, title = "Account Linked Successfully", footer = "HeXO Account Linking") {
                p {
                    +"Your Discord account is now connected. You can close this tab and return to Discord."
                }

                p(classes = "rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-amber-950 dark:border-amber-300/25 " +
                    "dark:bg-amber-500/10 dark:text-amber-100",
                ) {
                    span(classes = "mr-1 font-semibold uppercase tracking-wide text-amber-700 dark:text-amber-200") {
                        +"Note:"
                    }

                    +"Make sure your HeXO profile is linked to this Discord account before claiming roles. Use the"
                    span(classes = "mx-1 inline-flex items-center rounded border border-amber-300/50 bg-white px-1.5 py-0.5 font-mono " +
                        "text-xs font-semibold text-amber-800 dark:bg-slate-950/60 dark:text-amber-300",
                    ) {
                        +"/link"
                    }
                    +"command in Discord."
                }
            }
        }
    }

    private suspend fun RoutingCall.respondFailedHtml(code: HttpStatusCode) {
        respondHtml(code) {
            statusPage(CardColorScheme.Error, title = "${code.value} ${code.description}", footer = "HeXO Account Linking") {
                p {
                    +"We could not complete your Discord authorization. "
                    +"Start the linking flow from Discord again and finish it before the link expires. "
                }
            }
        }
    }
}
