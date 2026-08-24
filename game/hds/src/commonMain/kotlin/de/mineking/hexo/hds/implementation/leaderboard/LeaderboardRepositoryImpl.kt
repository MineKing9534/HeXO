package de.mineking.hexo.hds.implementation.leaderboard

import de.mineking.hexo.game.model.leaderboard.Leaderboard
import de.mineking.hexo.game.model.leaderboard.LeaderboardRepository
import de.mineking.hexo.hds.implementation.HdsApiClient
import io.ktor.client.call.body
import io.ktor.http.isSuccess

internal class LeaderboardRepositoryImpl(private val client: HdsApiClient) : LeaderboardRepository {
    private val requester = client.entityRequesterFactory.createEntityRequester<Unit, Leaderboard> {
        val response = client.request("/leaderboard")

        if (!response.status.isSuccess()) return@createEntityRequester null
        LeaderboardImpl(client, response.body())
    }

    override suspend fun getLeaderboard() = requester.fetch(Unit)
        ?: error("Failed to fetch leaderboard")
}
