package de.mineking.hexo.hds.implementation.profile

import de.mineking.hexo.hds.model.profile.Profile
import de.mineking.hexo.hds.model.profile.ProfileStatistics
import de.mineking.hexo.hds.model.profile.RichProfile

internal class ProfileImpl(
    private val repository: ProfileRepositoryImpl,
    dto: ProfileDto,
) : Profile {
    override val id = dto.id
    override val url = "${repository.client.host}/profile/${id.value}"
    override val displayName = dto.username
    override val image = dto.image
    override val registeredAt = dto.registeredAt
    override val lastActiveAt = dto.lastActiveAt

    override suspend fun retrieveStatistics(forceUpdate: Boolean) = repository.getProfileStatistics(id)
}

internal class RichProfileImpl(
    private val repository: ProfileRepositoryImpl,
    dto: ProfileDto,
    override val statistics: ProfileStatistics,
) : RichProfile, Profile by ProfileImpl(repository, dto) {
    override suspend fun retrieveStatistics(forceUpdate: Boolean): ProfileStatistics? {
        if (!forceUpdate) return statistics
        return repository.getProfileStatistics(id)
    }
}
