package de.mineking.hexo.hds.implementation.formation

import de.mineking.hexo.game.model.formation.Formation
import de.mineking.hexo.game.model.formation.FormationId
import de.mineking.hexo.game.model.formation.FormationRepository
import de.mineking.hexo.hds.implementation.HdsApiClient
import io.ktor.client.call.body
import io.ktor.http.isSuccess

internal class FormationRepositoryImpl(private val client: HdsApiClient) : FormationRepository {
    private val requester = client.entityRequesterFactory.createEntityRequester<FormationId, Formation> { id ->
        val response = client.request("/sandbox-positions/${id.value}")

        if (!response.status.isSuccess()) return@createEntityRequester null
        FormationImpl(client, response.body())
    }

    override suspend fun getFormation(id: FormationId) = requester.fetch(id)
}
