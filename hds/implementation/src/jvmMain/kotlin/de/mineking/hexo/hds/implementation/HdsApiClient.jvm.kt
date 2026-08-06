package de.mineking.hexo.hds.implementation

import io.ktor.client.engine.cio.CIO

actual val DefaultHttpEngine = CIO.create()

actual val HEXO_USER_AGENT: String? = "HeXO-Kotlin"
