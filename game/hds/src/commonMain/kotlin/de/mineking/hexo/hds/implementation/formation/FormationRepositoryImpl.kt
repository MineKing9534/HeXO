package de.mineking.hexo.hds.implementation.formation

import de.mineking.hexo.game.model.formation.Formation
import de.mineking.hexo.game.model.formation.FormationId
import de.mineking.hexo.game.model.formation.FormationNotFoundError
import de.mineking.hexo.game.model.formation.FormationQueryError
import de.mineking.hexo.game.model.formation.FormationRepository
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.hds.implementation.utils.parseBodyOrNull
import de.mineking.hexo.utils.types.Result
import de.mineking.hexo.utils.types.successIfNotNullOrElse

internal class FormationRepositoryImpl(private val client: HdsApiClient) : FormationRepository {
    override val url = "${client.publicUrl}/sandbox"

    companion object {
        private val ID_PATTERN = "^[a-zA-Z0-9]{7}$".toRegex()
    }

    private val requester = client.entityRequesterFactory.createEntityRequester<FormationId, Formation?> { id ->
        val response = client.request("/sandbox-positions/${id.value}")
        response.parseBodyOrNull<FormationDto, Formation> { FormationImpl(client, it) }
    }

    override suspend fun getFormation(id: FormationId): Result<Formation, FormationQueryError> {
        if (!id.value.matches(ID_PATTERN)) return Result.Error(FormationNotFoundError)
        return requester.fetch(id)
            .successIfNotNullOrElse(FormationNotFoundError)
    }
}
