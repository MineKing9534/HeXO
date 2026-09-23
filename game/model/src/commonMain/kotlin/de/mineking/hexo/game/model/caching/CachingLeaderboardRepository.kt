package de.mineking.hexo.game.model.caching

import de.mineking.hexo.game.model.leaderboard.Leaderboard
import de.mineking.hexo.game.model.leaderboard.LeaderboardRepository
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.time.Clock

fun LeaderboardRepository.caching(): LeaderboardRepository {
    require(this !is CachingLeaderboardRepository)
    return CachingLeaderboardRepository(this)
}

open class CachingLeaderboardRepository(open val delegate: LeaderboardRepository) : LeaderboardRepository {
    private val lock = SynchronizedObject()
    private var cache: Leaderboard? = null

    override suspend fun getLeaderboard(): Leaderboard {
        synchronized(lock) {
            val cached = cache?.takeIf { it.nextRefreshAt > Clock.System.now() }
            if (cached != null) return cached
        }

        val fetched = delegate.getLeaderboard()
        synchronized(lock) {
            val current = cache
            if (current != null && fetched.generatedAt < current.generatedAt) return current

            return fetched.also { cache = it }
        }
    }
}
