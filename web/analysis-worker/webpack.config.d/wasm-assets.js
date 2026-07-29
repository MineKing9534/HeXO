config.target = "webworker"
config.output = config.output || {}
config.output.globalObject = "self"

config.module.rules.unshift({
    test: /\.wasm$/,
    type: "asset/inline",
})
