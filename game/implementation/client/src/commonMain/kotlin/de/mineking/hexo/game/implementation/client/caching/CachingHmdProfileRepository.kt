package de.mineking.hexo.game.implementation.client.caching

import de.mineking.hexo.game.implementation.client.profile.HmdProfile
import de.mineking.hexo.game.implementation.client.profile.HmdProfileRepository
import de.mineking.hexo.game.implementation.client.profile.HmdProfileWithStatistics
import de.mineking.hexo.game.model.caching.CachingProfileRepository
import de.mineking.hexo.game.model.profile.Profile
import de.mineking.hexo.game.model.profile.ProfileIdentifier
import de.mineking.hexo.game.model.profile.ProfileNotFoundError
import de.mineking.hexo.game.model.profile.ProfileQueryError
import de.mineking.hexo.game.model.profile.ProfileWithStatistics
import de.mineking.hexo.utils.cache.CacheConfiguration
import de.mineking.hexo.utils.types.Result

fun HmdProfileRepository.caching(
    profileConfig: CacheConfiguration<ProfileIdentifier, Result<ProfileWithStatistics, ProfileQueryError>>,
    searchConfig: CacheConfiguration<String, List<Profile>>,
): HmdProfileRepository {
    require(this !is CachingProfileRepository)
    return CachingHmdProfileRepository(this, profileConfig, searchConfig)
}

class CachingHmdProfileRepository(
    override val delegate: HmdProfileRepository,
    profileConfig: CacheConfiguration<ProfileIdentifier, Result<ProfileWithStatistics, ProfileQueryError>>,
    searchConfig: CacheConfiguration<String, List<Profile>>,
) : HmdProfileRepository, CachingProfileRepository(delegate, profileConfig, searchConfig) {
    override val currentProfile get() = delegate.currentProfile

    @Suppress("UNCHECKED_CAST")
    override suspend fun getProfile(id: ProfileIdentifier) = super.getProfile(id) as Result<HmdProfileWithStatistics, ProfileNotFoundError>

    @Suppress("UNCHECKED_CAST")
    override suspend fun getProfilesByName(name: String) = super.getProfilesByName(name) as List<HmdProfile>
}
