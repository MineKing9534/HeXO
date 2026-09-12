package socketio

private fun <T> createJsObject(config: T.() -> Unit) = js("{}").unsafeCast<T>().apply { config() }

operator fun SocketOptions.Companion.invoke(config: SocketOptions.() -> Unit) = createJsObject(config)
operator fun AuthPayload.Companion.invoke(config: AuthPayload.() -> Unit) = createJsObject(config)

fun Map<String, String?>.toJsObject(): dynamic {
    val obj = js("{}")
    forEach { (k, v) -> obj[k] = v }
    return obj
}
