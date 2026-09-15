package de.mineking.hexo.discord.oauth2.api

import de.mineking.hexo.discord.oauth2.OAuth2Tokens
import de.mineking.hexo.discord.oauth2.Scope
import de.mineking.hexo.discord.oauth2.model.OAuth2Flow
import de.mineking.hexo.utils.types.IError
import de.mineking.hexo.utils.types.Result

interface OAuth2FlowHandler {
    val flow: OAuth2Flow
    val scopes: Set<Scope>

    suspend fun complete(tokens: OAuth2Tokens): Result<Unit, OAuth2FlowError>
}

interface OAuth2FlowError : IError
