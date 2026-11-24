package de.mineking.hexo.discord.bot.utils

import de.mineking.hexo.board.binary.EncodedBoard
import de.mineking.hexo.board.binary.decode
import de.mineking.hexo.game.model.game.FinishedGameRepository
import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.utils.types.orElse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface BoardReference {
    @Serializable
    @SerialName("game")
    data class Game(val gameId: GameId, val move: Int) : BoardReference

    @Serializable
    @SerialName("binary")
    class Binary(val data: EncodedBoard) : BoardReference
}

suspend inline fun BoardReference.decode(
    gameRepository: FinishedGameRepository,
    notFound: () -> Nothing,
) = when (this) {
    is BoardReference.Game -> gameRepository.getGame(gameId)
        .orElse(notFound)
        .position
        .getState(move - 1)
    is BoardReference.Binary -> data.decode()
}
