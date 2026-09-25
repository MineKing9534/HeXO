package de.mineking.hexo.game.model.caching

import de.mineking.hexo.game.model.formation.Formation
import de.mineking.hexo.game.model.formation.FormationId
import de.mineking.hexo.game.model.formation.FormationQueryError
import de.mineking.hexo.game.model.formation.FormationRepository
import de.mineking.hexo.utils.cache.CacheConfiguration
import de.mineking.hexo.utils.cache.InMemoryCache
import de.mineking.hexo.utils.types.Result

fun FormationRepository.caching(
    config: CacheConfiguration<FormationId, Result<Formation, FormationQueryError>>,
): FormationRepository {
    require(this !is CachingFormationRepository)
    return CachingFormationRepository(this, config)
}

open class CachingFormationRepository(
    open val delegate: FormationRepository,
    config: CacheConfiguration<FormationId, Result<Formation, FormationQueryError>>,
) : FormationRepository {
    override val url by delegate::url

    private val cache = InMemoryCache(config)

    override suspend fun getFormation(id: FormationId) = cache.getOrPut(id) { delegate.getFormation(it) }
}
