package de.mineking.hexo.game.model

import kotlin.time.Duration
import kotlin.time.Instant

data class LiveDuration(val duration: Duration, val timestamp: Instant)

sealed interface TimeControl {
    data object Unlimited : TimeControl

    data class Turn(
        val turnTime: Duration,
    ) : TimeControl

    data class Match(
        val mainTime: Duration,
        val increment: Duration,
    ) : TimeControl
}
