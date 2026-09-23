package de.mineking.hexo.game.model.profile

import de.mineking.hexo.utils.types.EntityRepository
import de.mineking.hexo.utils.types.IError
import de.mineking.hexo.utils.types.Result
import kotlinx.serialization.Serializable

@Serializable
sealed interface ProfileQueryError : IError

@Serializable
data object ProfileNotFoundError : ProfileQueryError

sealed interface ProfileIdentifier {
    data class Name(val name: String) : ProfileIdentifier
}

interface ProfileRepository : EntityRepository<Profile> {
    suspend fun getProfileStatistics(id: ProfileIdentifier): Result<ProfileStatistics, ProfileQueryError>
    suspend fun getProfile(id: ProfileIdentifier): Result<ProfileWithStatistics, ProfileQueryError>

    suspend fun getProfilesByName(name: String): List<Profile>

    companion object Empty : ProfileRepository {
        override val url = ""

        override suspend fun getProfileStatistics(id: ProfileIdentifier) = Result.Error(ProfileNotFoundError)
        override suspend fun getProfile(id: ProfileIdentifier) = Result.Error(ProfileNotFoundError)
        override suspend fun getProfilesByName(name: String) = emptyList<Profile>()
    }
}

suspend fun ProfileRepository.getProfileByName(name: String) = getProfile(ProfileIdentifier.Name(name))
suspend fun ProfileRepository.getProfileStatisticsByName(name: String) = getProfileStatistics(ProfileIdentifier.Name(name))
