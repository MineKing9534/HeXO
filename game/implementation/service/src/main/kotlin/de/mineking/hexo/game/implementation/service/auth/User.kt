package de.mineking.hexo.game.implementation.service.auth

import de.mineking.hexo.game.model.profile.ProfileId

internal sealed interface User {
    data object Anonymous : User
    data class Authenticated(val profileId: ProfileId) : User
}
