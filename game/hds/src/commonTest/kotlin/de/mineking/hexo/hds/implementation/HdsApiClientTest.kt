package de.mineking.hexo.hds.implementation

import de.mineking.hexo.utils.types.EntityRequestException
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.callContext
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class, InternalAPI::class)
class HdsApiClientTest {
    @Test
    fun `failed request does not cancel the client and can be retried`() = runTest {
        val client = createClient()
        try {
            val failure = EntityRequestException("Request failed")
            var fail = true
            val requester = client.entityRequesterFactory.createEntityRequester<Int, Int>(client.coroutineScope) { id ->
                if (fail) throw failure
                id
            }

            assertSame(failure, assertFailsWith<EntityRequestException> { requester.fetch(1) })
            assertTrue(client.client.httpClient.isActive)

            fail = false
            assertEquals(1, requester.fetch(1))
            assertEquals(2, requester.fetch(2))
        } finally {
            client.close()
        }
    }

    @Test
    fun `failed request does not cancel an unrelated in flight request`() = runTest {
        val client = createClient()
        try {
            val gate = CompletableDeferred<Unit>()
            val requester = client.entityRequesterFactory.createEntityRequester<Int, Int>(client.coroutineScope) { id ->
                if (id == 1) throw EntityRequestException("Request failed")
                gate.await()
                id
            }
            val pending = async { requester.fetch(2) }
            runCurrent()

            assertFailsWith<EntityRequestException> { requester.fetch(1) }
            gate.complete(Unit)
            assertEquals(2, pending.await())
        } finally {
            client.close()
        }
    }

    @Test
    fun `failed background lobby fetch does not cancel subsequent requests`() = runTest {
        val client = createClient(HttpStatusCode.InternalServerError)
        try {
            runCurrent()
            assertTrue(client.client.httpClient.isActive)
            assertTrue(client.coroutineScope.isActive)

            val requester = client.entityRequesterFactory.createEntityRequester<Int, Int>(client.coroutineScope) { it }
            assertEquals(1, requester.fetch(1))
            assertEquals(HttpStatusCode.OK, client.request("/health").status)
        } finally {
            client.close()
        }
    }

    @Test
    fun `closing HDS cancels pending requests and its scope`() = runTest {
        val client = createClient()
        try {
            val gate = CompletableDeferred<Unit>()
            val requester = client.entityRequesterFactory.createEntityRequester<Int, Unit>(client.coroutineScope) { gate.await() }
            val pending = async { requester.fetch(1) }
            runCurrent()

            client.close()
            assertFalse(client.coroutineScope.isActive)
            assertFailsWith<CancellationException> { pending.await() }
        } finally {
            client.close()
        }
    }

    @Test
    fun `HTTP client cancellation cancels the HDS scope`() = runTest {
        val client = createClient()
        try {
            client.client.httpClient.coroutineContext.cancel()
            assertFalse(client.coroutineScope.isActive)
        } finally {
            client.close()
        }
    }

    private fun TestScope.createClient(lobbyStatus: HttpStatusCode = HttpStatusCode.OK): HdsApiClient {
        val engine = object : HttpClientEngineBase("test") {
            override val dispatcher: CoroutineDispatcher = StandardTestDispatcher(testScheduler)
            override val config = HttpClientEngineConfig()

            override suspend fun execute(data: HttpRequestData) = HttpResponseData(
                statusCode = if (data.url.encodedPath == "/sessions") lobbyStatus else HttpStatusCode.OK,
                requestTime = GMTDate(),
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                version = HttpProtocolVersion.HTTP_1_1,
                body = ByteReadChannel("[]"),
                callContext = callContext(),
            )
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { json() }
        }
        return HdsApiClient(HdsHttpClient("https://hds.test", httpClient, null))
    }
}
