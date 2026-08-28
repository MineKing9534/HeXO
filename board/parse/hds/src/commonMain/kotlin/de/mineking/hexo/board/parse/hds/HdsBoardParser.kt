package de.mineking.hexo.board.parse.hds

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.parse.BoardParser
import de.mineking.hexo.board.parse.LinkParser
import de.mineking.hexo.board.parse.allowTurnLabels
import de.mineking.hexo.board.parse.or
import de.mineking.hexo.board.take
import de.mineking.hexo.board.toBoard
import de.mineking.hexo.game.model.HdsRepositoryContainer
import de.mineking.hexo.game.model.formation.FormationId
import de.mineking.hexo.game.model.formation.FormationRepository
import de.mineking.hexo.game.model.game.FinishedGameRepository
import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.utils.types.map
import de.mineking.hexo.utils.types.orThrow

class HdsBoardParser(
    formationRepository: FormationRepository,
    gameRepository: FinishedGameRepository,
) : BoardParser by (HdsGameLinkParser(gameRepository) or HdsSandboxLinkParser(formationRepository)).allowTurnLabels() {
    constructor(repositories: HdsRepositoryContainer) : this(repositories.formationRepository, repositories.finishedGameRepository)
}

class HdsSandboxLinkParser(private val repository: FormationRepository) : LinkParser(prefix = "https://hexo.did.science/sandbox/") {
    override suspend fun parseLink(param: String): Board {
        return repository.getFormation(FormationId(param))
            .map {
                it.position
                    .toBoard(focusWinningRows = false)
            }
            .orThrow { HexoNotationException("Formation $param not found") }
    }
}

class HdsGameLinkParser(private val repository: FinishedGameRepository) : LinkParser(prefix = "https://hexo.did.science/games/") {
    override suspend fun parseLink(param: String): Board {
        val (id, maxMoves) = param.parseGameLinkParameter()
        return repository.getGame(id)
            .map {
                it.position
                    .take(maxMoves ?: Int.MAX_VALUE)
                    .toBoard(focusWinningRows = false)
            }
            .orThrow { HexoNotationException("Game $param not found") }
    }

    private fun String.parseGameLinkParameter(): Pair<GameId, Int?> {
        val parts = split("?move=", limit = 2)
        if (parts.size == 1) return GameId(parts[0]) to null

        val maxMoves = parts[1].toIntOrNull()
            ?: throw HexoNotationException("Invalid move `${parts[1]}`")
        return GameId(parts[0]) to maxMoves
    }
}
