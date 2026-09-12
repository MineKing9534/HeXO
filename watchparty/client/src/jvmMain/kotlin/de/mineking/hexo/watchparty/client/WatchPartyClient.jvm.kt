package de.mineking.hexo.watchparty.client

import io.ktor.client.engine.cio.CIO

actual val DefaultHttpEngine = CIO.create()
