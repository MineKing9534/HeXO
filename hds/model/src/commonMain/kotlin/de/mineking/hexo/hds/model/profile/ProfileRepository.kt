package de.mineking.hexo.hds.model.profile

interface ProfileRepository {
    suspend fun getProfile(id: ProfileId): RichProfile?

    suspend fun getProfilesByName(name: String): List<Profile>
}

suspend fun ProfileRepository.getProfileByName(name: String) = getProfilesByName(name)
    .firstOrNull { it.displayName.equals(name, ignoreCase = true) }
