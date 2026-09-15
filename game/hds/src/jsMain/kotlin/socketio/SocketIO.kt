@file:JsModule("socket.io-client")
@file:JsNonModule

package socketio

external interface Socket {
    @IgnorableReturnValue
    fun on(event: String, callback: (dynamic) -> Unit): Socket

    @IgnorableReturnValue
    fun off(event: String, callback: (dynamic) -> Unit): Socket

    @IgnorableReturnValue
    fun emit(event: String, data: dynamic = definedExternally): Socket

    @IgnorableReturnValue
    fun connect(): Socket

    @IgnorableReturnValue
    fun disconnect(): Socket
}

external interface AuthPayload {
    var deviceId: String
    var ephemeralClientId: String
    var versionHash: String

    companion object
}

external interface SocketOptions {
    var autoConnect: Boolean
    var transports: Array<String>
    var auth: AuthPayload
    var path: String
    var query: dynamic
    var extraHeaders: dynamic
    var addTrailingSlash: Boolean

    companion object
}

external fun io(url: String, options: SocketOptions = definedExternally): Socket
