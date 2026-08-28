package de.mineking.hexo.game.model.caching

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import com.sksamuel.aedile.core.expireAfterWrite
import de.mineking.hexo.game.model.profile.Profile
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.profile.ProfileIdentifier
import de.mineking.hexo.game.model.profile.ProfileQueryError
import de.mineking.hexo.game.model.profile.ProfileRepository
import de.mineking.hexo.game.model.profile.ProfileWithStatistics
import de.mineking.hexo.utils.types.Result
import kotlin.time.Duration.Companion.minutes

internal class CachingProfileRepository(val delegate: ProfileRepository, cacheSize: Long) : ProfileRepository {
    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(3.minutes)
        .maximumSize(cacheSize)
        .asCache<ProfileIdentifier, Result<ProfileWithStatistics, ProfileQueryError>>()

    private val profileListCache = Caffeine.newBuilder()
        .expireAfterWrite(10.minutes)
        .maximumSize(cacheSize)
        .asCache<String, List<Profile>>()

    override suspend fun getProfile(id: ProfileIdentifier) = cache
        .get(id) { delegate.getProfile(it) }

    override suspend fun getProfileStatistics(id: ProfileId) = delegate.getProfileStatistics(id)
    override suspend fun getProfilesByName(name: String) = profileListCache
        .get(name) { delegate.getProfilesByName(it) }
}
