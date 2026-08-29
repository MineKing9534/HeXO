package de.mineking.hexo.game.model.profile

import de.mineking.hexo.game.model.EntityRepository
import de.mineking.hexo.utils.types.IError
import de.mineking.hexo.utils.types.Result

sealed interface ProfileQueryError : IError
data object ProfileNotFoundError : ProfileQueryError

sealed interface ProfileIdentifier {
    data class Id(val id: ProfileId) : ProfileIdentifier
    data class Name(val name: String) : ProfileIdentifier
}

interface ProfileRepository : EntityRepository<Profile> {
    suspend fun getProfileStatistics(id: ProfileId): Result<ProfileStatistics, ProfileQueryError>
    suspend fun getProfile(id: ProfileIdentifier): Result<ProfileWithStatistics, ProfileQueryError>

    suspend fun getProfilesByName(name: String): List<Profile>
}

suspend fun ProfileRepository.getProfileById(id: ProfileId) = getProfile(ProfileIdentifier.Id(id))
suspend fun ProfileRepository.getProfileByName(name: String) = getProfile(ProfileIdentifier.Name(name))
