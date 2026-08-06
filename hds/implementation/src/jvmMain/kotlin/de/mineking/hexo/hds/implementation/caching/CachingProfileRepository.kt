package de.mineking.hexo.hds.implementation.caching

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import com.sksamuel.aedile.core.expireAfterWrite
import de.mineking.hexo.hds.model.profile.Profile
import de.mineking.hexo.hds.model.profile.ProfileId
import de.mineking.hexo.hds.model.profile.ProfileRepository
import de.mineking.hexo.hds.model.profile.RichProfile
import kotlin.time.Duration.Companion.minutes

internal class CachingProfileRepository(val delegate: ProfileRepository, cacheSize: Long) : ProfileRepository {
    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(3.minutes)
        .maximumSize(cacheSize)
        .asCache<ProfileId, RichProfile>()

    private val profileListCache = Caffeine.newBuilder()
        .expireAfterWrite(10.minutes)
        .maximumSize(cacheSize)
        .asCache<String, List<Profile>>()

    override suspend fun getProfile(id: ProfileId) = cache
        .getOrNull(id) { delegate.getProfile(it) }

    override suspend fun getProfilesByName(name: String) = profileListCache
        .getOrNull(name) { delegate.getProfilesByName(it) }
        ?: emptyList()
}
