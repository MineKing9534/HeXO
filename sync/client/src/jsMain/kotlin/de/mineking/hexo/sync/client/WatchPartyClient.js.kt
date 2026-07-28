package de.mineking.hexo.sync.client

import io.ktor.client.engine.js.Js

actual val DefaultHttpEngine = Js.create()
