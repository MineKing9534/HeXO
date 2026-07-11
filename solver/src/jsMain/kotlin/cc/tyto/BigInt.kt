package cc.tyto

@JsName("BigInt")
@OptIn(ExperimentalWasmJsInterop::class)
internal external fun jsBigInt(value: Int): JsBigInt
