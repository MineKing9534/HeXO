package de.mineking.hexo.discord.bot.menus

import de.mineking.discord.extensions.DiscordToolKitContext
import de.mineking.discord.extensions.localization.localize
import de.mineking.discord.extensions.localization.translation
import de.mineking.discord.localization.LocalizationBundle
import de.mineking.discord.markdown.MarkdownScope
import de.mineking.discord.menu.components.ComponentScope
import de.mineking.discord.menu.components.text
import de.mineking.discord.menu.message.components.actionRow
import de.mineking.discord.menu.message.components.container
import de.mineking.discord.menu.message.components.section
import de.mineking.discord.menu.message.components.separator
import de.mineking.discord.menu.message.components.thumbnail
import de.mineking.discord.menu.renderOnly
import de.mineking.discord.menu.runtime.MenuRuntime
import de.mineking.discord.menu.state.state
import de.mineking.hexo.discord.bot.ErrorResponseMessage
import de.mineking.hexo.discord.bot.HeXODiscordBot
import de.mineking.hexo.discord.bot.utils.reloadAction
import de.mineking.hexo.discord.bot.utils.reloadButton
import de.mineking.hexo.discord.bot.utils.viewButton
import de.mineking.hexo.discord.core.DiscordUserId
import de.mineking.hexo.discord.core.jda
import de.mineking.hexo.game.model.profile.ProfileGameStatistics
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.profile.ProfileWithStatistics
import de.mineking.hexo.link.getDiscordProfile
import de.mineking.hexo.utils.types.orElse
import dev.freya02.jda.emojis.unicode.Emojis
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import net.dv8tion.jda.api.EmbedBuilder.ZERO_WIDTH_SPACE
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.utils.TimeFormat
import kotlin.math.roundToInt
import kotlin.time.toJavaInstant

internal const val FALLBACK_IMAGE_URL = "https://cdn.discordapp.com/embed/avatars/0.png?size=128"

context(context: DiscordToolKitContext<HeXODiscordBot>)
fun MenuRuntime.profileMenu() = registerMessageMenu<ProfileId>("profile") {
    localize()
    val localization = translation<ProfileMenuLocalization>()

    val reloadAction = reloadAction()

    val id by state(fromInput { input })

    renderOnly {
        val (profile, linkedAccount) = coroutineScope {
            val profile = async {
                context.main.repositories.profileRepository.getProfile(id).orElse {
                    localization.profileNotFound().throwValidationError(id.value)
                }
            }
            val linkedAccount = async { context.main.accountLinkRepository?.getDiscordProfile(id) }

            profile.await() to linkedAccount.await()
        }

        container {
            profile(profile, linkedAccount)

            separator()
            actionRow {
                viewButton(profile.url)
                reloadButton(reloadAction)
            }
        }
    }
}

context(_: DiscordToolKitContext<*>)
private suspend fun ComponentScope<Section>.profile(
    profile: ProfileWithStatistics,
    linkedAccount: DiscordUserId?,
) {
    section(accessory = { thumbnail(profile.image ?: FALLBACK_IMAGE_URL) }) {
        text {
            h1 {
                val rank = when (profile.statistics.rating.worldRank) {
                    1 -> Emojis.FIRST_PLACE.formatted
                    2 -> Emojis.SECOND_PLACE.formatted
                    3 -> Emojis.THIRD_PLACE.formatted
                    null -> "[:question:]"
                    else -> "[#${profile.statistics.rating.worldRank}]"
                }

                text("$rank ${profile.displayName}")

                if (linkedAccount != null) raw(" (${linkedAccount.jda.asMention})")

                text(" ".repeat(5))
                text(ZERO_WIDTH_SPACE)
            }
        }

        profileStatistics(profile)
    }
}

context(_: DiscordToolKitContext<*>)
private fun ComponentScope<TextDisplay>.profileStatistics(profile: ProfileWithStatistics) {
    val localization = translation<ProfileMenuLocalization>()

    fun MarkdownScope.line(label: String, value: String, raw: Boolean = false) {
        line {
            bold("$label: ")
            if (raw) {
                raw(value)
            } else {
                text(value)
            }
        }
    }

    fun MarkdownScope.gameStats(stats: ProfileGameStatistics) {
        line(localization.gamesPlayedLabel(), "${stats.total}")
        line(localization.gamesWonLabel(), localization.gamesWonValue(stats.won, stats.total, (stats.winRate * 100).roundToInt()))
    }

    text {
        line(localization.memberSinceLabel(), TimeFormat.DATE_SHORT.format(profile.registeredAt.toJavaInstant()), raw = true)

        val currentElo = profile.statistics.rating.elo
        val highestElo = profile.statistics.highestRating.elo
        line(localization.eloLabel(), localization.eloValue(currentElo, highestElo, currentElo != highestElo))

        line()
        h3(":seedling: ${localization.overallGames()}")
        gameStats(profile.statistics.totalGames)

        line()
        h3(":trophy: ${localization.rankedGames()}")
        gameStats(profile.statistics.ratedGames)

        val currentStreak = profile.statistics.ratedGames.currentWinStreak
        val longestStreak = profile.statistics.ratedGames.longestWinStreak
        line(localization.streakLabel(), localization.streakValue(currentStreak, longestStreak))
    }
}

@LocalizationBundle("profile")
internal interface ProfileMenuLocalization {
    fun memberSinceLabel(): String
    fun eloLabel(): String
    fun eloValue(current: Int, highest: Int, showHighest: Boolean): String

    fun overallGames(): String
    fun rankedGames(): String

    fun gamesPlayedLabel(): String
    fun gamesWonLabel(): String
    fun gamesWonValue(won: Int, total: Int, winRate: Int): String
    fun streakLabel(): String
    fun streakValue(current: Int, longest: Int): String

    fun profileNotFound(): ErrorResponseMessage<String>
}
