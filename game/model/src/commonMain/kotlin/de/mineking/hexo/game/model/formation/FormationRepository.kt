package de.mineking.hexo.game.model.formation

import de.mineking.hexo.utils.types.IError
import de.mineking.hexo.utils.types.Result

sealed interface FormationQueryError : IError
data object FormationNotFoundError : FormationQueryError

interface FormationRepository {
    suspend fun getFormation(id: FormationId): Result<Formation, FormationQueryError>
}
