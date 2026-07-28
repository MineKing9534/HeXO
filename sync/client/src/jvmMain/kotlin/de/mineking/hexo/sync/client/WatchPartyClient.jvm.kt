package de.mineking.hexo.sync.client

import io.ktor.client.engine.cio.CIO

actual val DefaultHttpEngine = CIO.create()
