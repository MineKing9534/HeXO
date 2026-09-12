package de.mineking.hexo.game.model.caching

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import de.mineking.hexo.game.model.formation.Formation
import de.mineking.hexo.game.model.formation.FormationId
import de.mineking.hexo.game.model.formation.FormationQueryError
import de.mineking.hexo.game.model.formation.FormationRepository
import de.mineking.hexo.utils.types.Result

internal class CachingFormationRepository(val delegate: FormationRepository, cacheSize: Long) : FormationRepository {
    override val url by delegate::url

    private val cache = Caffeine.newBuilder()
        .maximumSize(cacheSize)
        .asCache<FormationId, Result<Formation, FormationQueryError>>()

    override suspend fun getFormation(id: FormationId) = cache.get(id) { delegate.getFormation(it) }
}
