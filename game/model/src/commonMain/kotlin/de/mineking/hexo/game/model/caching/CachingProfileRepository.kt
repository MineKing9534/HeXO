package de.mineking.hexo.game.model.caching

import de.mineking.hexo.game.model.profile.Profile
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.profile.ProfileIdentifier
import de.mineking.hexo.game.model.profile.ProfileQueryError
import de.mineking.hexo.game.model.profile.ProfileRepository
import de.mineking.hexo.game.model.profile.ProfileWithStatistics
import de.mineking.hexo.utils.cache.CacheConfiguration
import de.mineking.hexo.utils.cache.InMemoryCache
import de.mineking.hexo.utils.types.Result

fun ProfileRepository.caching(
    profileConfig: CacheConfiguration<ProfileIdentifier, Result<ProfileWithStatistics, ProfileQueryError>>,
    searchConfig: CacheConfiguration<String, List<Profile>>,
): ProfileRepository = CachingProfileRepository(this, profileConfig, searchConfig)

private class CachingProfileRepository(
    val delegate: ProfileRepository,
    profileConfig: CacheConfiguration<ProfileIdentifier, Result<ProfileWithStatistics, ProfileQueryError>>,
    searchConfig: CacheConfiguration<String, List<Profile>>,
) : ProfileRepository {
    override val url by delegate::url

    private val cache = InMemoryCache(profileConfig)
    private val searchCache = InMemoryCache(searchConfig)

    override suspend fun getProfile(id: ProfileIdentifier) = cache.getOrPut(id) {
        delegate.getProfile(it)
    }

    override suspend fun getProfileStatistics(id: ProfileId) = delegate.getProfileStatistics(id)
    override suspend fun getProfilesByName(name: String) = searchCache.getOrPut(name) {
        delegate.getProfilesByName(it)
    }
}
