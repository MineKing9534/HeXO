package de.mineking.hexo.web.worker

import com.varabyte.kobweb.serialization.createIOSerializer
import com.varabyte.kobweb.worker.OutputDispatcher
import com.varabyte.kobweb.worker.WorkerFactory
import com.varabyte.kobweb.worker.WorkerStrategy
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.solver.FindDefenseResult
import de.mineking.hexo.solver.FindWinResult
import de.mineking.hexo.solver.strix.StrixHexoSolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AnalysisInput(
    val requestId: Int,
    val board: Board,
    val player: CellOwner,
    val remaining: Int,
)

@Serializable
data class AnalysisOutput(
    val requestId: Int,
    val threat: FindWinResult,
    val defense: FindDefenseResult,
)

private val analysisJson = Json {
    allowStructuredMapKeys = true
}

internal class AnalysisWorkerFactory : WorkerFactory<AnalysisInput, AnalysisOutput> {
    override fun createStrategy(postOutput: OutputDispatcher<AnalysisOutput>): WorkerStrategy<AnalysisInput> {
        val solver = StrixHexoSolver()
        val scope = CoroutineScope(Dispatchers.Default)

        return WorkerStrategy { input ->
            scope.launch {
                @Suppress("TooGenericExceptionCaught")
                val output = try {
                    AnalysisOutput(
                        requestId = input.requestId,
                        threat = solver.findWin(input.board, input.player, input.remaining),
                        defense = solver.findDefense(input.board, input.player, input.remaining),
                    )
                } catch (error: Exception) {
                    console.error("Analysis worker failed", error)
                    AnalysisOutput(
                        requestId = input.requestId,
                        threat = FindWinResult.Unknown,
                        defense = FindDefenseResult.NoThreat,
                    )
                }

                postOutput(output)
            }
        }
    }

    override fun createIOSerializer() = analysisJson.createIOSerializer<AnalysisInput, AnalysisOutput>()
}
