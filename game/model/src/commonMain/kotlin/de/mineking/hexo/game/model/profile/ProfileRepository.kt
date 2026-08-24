package de.mineking.hexo.game.model.profile

interface ProfileRepository {
    suspend fun getProfileStatistics(id: ProfileId): ProfileStatistics?
    suspend fun getProfile(id: ProfileId): ProfileWithStatistics?

    suspend fun getProfilesByName(name: String): List<Profile>
}

suspend fun ProfileRepository.getProfileByName(name: String) = getProfilesByName(name)
    .firstOrNull { it.displayName.equals(name, ignoreCase = true) }
