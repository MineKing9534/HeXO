package de.mineking.hexo.web.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.isComplete
import de.mineking.hexo.game.model.TimeControl
import de.mineking.hexo.game.model.game.FinishedGameWithPosition
import de.mineking.hexo.game.model.game.GameWithPosition
import de.mineking.hexo.game.model.game.Player
import de.mineking.hexo.game.model.session.LiveSessionPlayer
import de.mineking.hexo.web.audio.SoundEffect
import de.mineking.hexo.web.rememberSoundPlayer
import de.mineking.hexo.web.settings.SettingsKey
import de.mineking.hexo.web.settings.collectAsState
import kotlinx.browser.window
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal interface PlayerTimeProvider {
    @Composable
    fun remainingTime(player: Player, current: Boolean): Duration?
}

@Composable
internal fun rememberPlayerTimeProvider(game: GameWithPosition, move: Int): PlayerTimeProvider =
    if (game is FinishedGameWithPosition) {
        val selectedMove = move.coerceIn(0, game.moveCount)
        remember(game, selectedMove) { FinishedPlayerTimeProvider(game, selectedMove) }
    } else {
        remember(game) { LivePlayerTimeProvider(game.position.nextTurn.player, game.result == null) }
    }

private class LivePlayerTimeProvider(
    private val activePlayer: CellOwner,
    private val running: Boolean,
) : PlayerTimeProvider {
    @Composable
    override fun remainingTime(player: Player, current: Boolean): Duration? {
        val livePlayer = player as? LiveSessionPlayer ?: return null
        val source = livePlayer.timeRemaining ?: return null
        val ticking = running && player.color == activePlayer
        fun remainingNow(): Duration {
            val elapsed = if (ticking) maxOf(Duration.ZERO, Clock.System.now() - source.timestamp) else Duration.ZERO
            return maxOf(Duration.ZERO, source.duration - elapsed)
        }
        var remaining by remember(livePlayer.id, source, ticking) { mutableStateOf(remainingNow()) }

        DisposableEffect(source, ticking) {
            fun update() {
                remaining = remainingNow()
            }

            update()
            val interval = if (ticking) window.setInterval(::update, 250) else null
            onDispose { interval?.let { window.clearInterval(it) } }
        }

        val soundPlayer = rememberSoundPlayer()
        val timerSounds by SettingsKey.SessionViewTimerSounds.collectAsState()
        LaunchedEffect(remaining.inWholeSeconds, ticking) {
            if (timerSounds && ticking && current && remaining > Duration.ZERO && remaining <= 10.seconds) {
                soundPlayer.play(SoundEffect.CountdownWarning)
            }
        }
        return remaining
    }
}

private class FinishedPlayerTimeProvider(game: FinishedGameWithPosition, move: Int) : PlayerTimeProvider {
    private val remaining = reconstructPlayerTimes(game, move)

    @Composable
    override fun remainingTime(player: Player, current: Boolean) = remaining[player.color]
}

private fun reconstructPlayerTimes(game: FinishedGameWithPosition, move: Int): Map<CellOwner, Duration> = when (
    val control = game.options.timeControl
) {
    TimeControl.Unlimited -> emptyMap()
    is TimeControl.Turn -> reconstructTimes(game, move, control.turnTime) { _, complete ->
        if (complete) control.turnTime else null
    }
    is TimeControl.Match -> reconstructTimes(game, move, control.mainTime) { remaining, complete ->
        if (complete) remaining + control.increment else remaining
    }
}

private inline fun reconstructTimes(
    game: FinishedGameWithPosition,
    move: Int,
    initialTime: Duration,
    afterTurn: (remaining: Duration, complete: Boolean) -> Duration?,
): Map<CellOwner, Duration> {
    val remaining = CellOwner.entries.associateWith { initialTime }.toMutableMap()
    var consumedMoves = 0
    var turnStartedAt = game.startedAt

    for (turn in game.position.turns) {
        val selectedCount = minOf(turn.moves.size, move - consumedMoves)
        if (selectedCount <= 0) break

        val lastMove = turn.moves[selectedCount - 1]
        val elapsed = positiveDuration(lastMove.timestamp - turnStartedAt)
        val playerTime = positiveDuration(remaining.getValue(turn.meta.player) - elapsed)
        val complete = selectedCount == turn.moves.size && turn.isComplete()
        remaining[turn.meta.player] = afterTurn(playerTime, complete) ?: playerTime

        consumedMoves += selectedCount
        turnStartedAt = lastMove.timestamp
        if (selectedCount < turn.moves.size) break
    }

    if (move == game.moveCount) {
        val elapsed = positiveDuration(game.startedAt + game.result.duration - turnStartedAt)
        val currentPlayer = game.position.nextTurn.player
        remaining[currentPlayer] = positiveDuration(remaining.getValue(currentPlayer) - elapsed)
    }
    return remaining
}

private fun positiveDuration(duration: Duration) = maxOf(Duration.ZERO, duration)
