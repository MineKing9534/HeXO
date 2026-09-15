package de.mineking.hexo.hds.implementation

import io.ktor.client.engine.js.Js

actual val DefaultHttpEngine = Js.create()

// Prevent CORS problems
actual val HEXO_USER_AGENT: String? = null
