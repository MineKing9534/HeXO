package de.mineking.hexo.watchparty.client

import io.ktor.client.engine.js.Js

actual val DefaultHttpEngine = Js.create()
