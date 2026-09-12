package de.mineking.hexo.web.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.isComplete
import de.mineking.hexo.board.take
import de.mineking.hexo.game.model.LiveDuration
import de.mineking.hexo.game.model.TimeControl
import de.mineking.hexo.game.model.game.FinishedGameWithPosition
import de.mineking.hexo.game.model.game.GameWithPosition
import de.mineking.hexo.game.model.game.Player
import de.mineking.hexo.game.model.session.LiveSessionPlayer
import de.mineking.hexo.web.audio.SoundEffect
import de.mineking.hexo.web.rememberSoundPlayer
import de.mineking.hexo.web.settings.SettingsKey
import de.mineking.hexo.web.settings.collectAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
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
        val remaining = source.rememberRemainingTime(ticking)

        CountdownWarning(remaining, ticking, current)

        return remaining
    }
}

private class FinishedPlayerTimeProvider(game: FinishedGameWithPosition, move: Int) : PlayerTimeProvider {
    private val remaining = reconstructPlayerTimes(game, move)

    @Composable
    override fun remainingTime(player: Player, current: Boolean) = remaining[player.color]
}

@Composable
private fun LiveDuration.rememberRemainingTime(ticking: Boolean): Duration {
    if (!ticking) return duration.coerceAtLeast(Duration.ZERO)

    var remaining by remember(this) { mutableStateOf(remainingNow()) }
    LaunchedEffect(this) {
        while (isActive) {
            remaining = remainingNow()
            delay(250.milliseconds)
        }
    }
    return remaining
}

private fun LiveDuration.remainingNow(): Duration {
    val elapsed = (Clock.System.now() - timestamp).coerceAtLeast(Duration.ZERO)
    return (duration - elapsed).coerceAtLeast(Duration.ZERO)
}

@Composable
private fun CountdownWarning(remaining: Duration, ticking: Boolean, current: Boolean) {
    val soundPlayer = rememberSoundPlayer()
    val enabled by SettingsKey.SessionViewTimerSounds.collectAsState()

    LaunchedEffect(remaining.inWholeSeconds, ticking) {
        if (enabled && ticking && current && remaining > Duration.ZERO && remaining <= 10.seconds) {
            soundPlayer.play(SoundEffect.CountdownWarning)
        }
    }
}

private fun reconstructPlayerTimes(game: FinishedGameWithPosition, move: Int): Map<CellOwner, Duration> {
    val control = game.options.timeControl
    val initialTime = when (control) {
        TimeControl.Unlimited -> return emptyMap()
        is TimeControl.Turn -> control.turnTime
        is TimeControl.Match -> control.mainTime
    }
    val remaining = CellOwner.entries.associateWith { initialTime }.toMutableMap()
    var turnStartedAt = game.startedAt

    for (turn in game.position.take(move).turns) {
        val lastMove = turn.moves.last()
        val elapsed = (lastMove.timestamp - turnStartedAt).coerceAtLeast(Duration.ZERO)
        val playerTime = (remaining.getValue(turn.meta.player) - elapsed).coerceAtLeast(Duration.ZERO)
        remaining[turn.meta.player] = if (turn.isComplete()) control.afterCompletedTurn(playerTime) else playerTime

        turnStartedAt = lastMove.timestamp
    }

    // Account for time after the last placement, such as waiting for a timeout or surrender.
    if (move == game.moveCount) {
        val finishedAt = game.startedAt + game.result.duration
        val elapsed = (finishedAt - turnStartedAt).coerceAtLeast(Duration.ZERO)
        val currentPlayer = game.position.nextTurn.player
        remaining[currentPlayer] = (remaining.getValue(currentPlayer) - elapsed).coerceAtLeast(Duration.ZERO)
    }
    return remaining
}

private fun TimeControl.afterCompletedTurn(remaining: Duration): Duration = when (this) {
    TimeControl.Unlimited -> remaining
    is TimeControl.Turn -> turnTime
    is TimeControl.Match -> remaining + increment
}
