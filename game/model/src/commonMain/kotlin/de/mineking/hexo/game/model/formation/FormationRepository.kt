package de.mineking.hexo.game.model.formation

import de.mineking.hexo.utils.types.EntityRepository
import de.mineking.hexo.utils.types.IError
import de.mineking.hexo.utils.types.Result
import kotlinx.serialization.Serializable

@Serializable
sealed interface FormationQueryError : IError

@Serializable
data object FormationNotFoundError : FormationQueryError

interface FormationRepository : EntityRepository<Formation> {
    suspend fun getFormation(id: FormationId): Result<Formation, FormationQueryError>

    companion object Empty : FormationRepository {
        override val url = ""
        override suspend fun getFormation(id: FormationId) = Result.Error(FormationNotFoundError)
    }
}
