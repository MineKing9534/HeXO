package de.mineking.hexo.hds.implementation.caching

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import de.mineking.hexo.game.model.formation.Formation
import de.mineking.hexo.game.model.formation.FormationId
import de.mineking.hexo.game.model.formation.FormationRepository

internal class CachingFormationRepository(val delegate: FormationRepository, cacheSize: Long) : FormationRepository {
    private val cache = Caffeine.newBuilder()
        .maximumSize(cacheSize)
        .asCache<FormationId, Formation>()

    override suspend fun getFormation(id: FormationId) = cache.getOrNull(id) { delegate.getFormation(it) }
}
