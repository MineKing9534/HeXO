package de.mineking.hexo.discord.bot.menus

import de.mineking.discord.extensions.DiscordToolKitContext
import de.mineking.discord.extensions.localization.localize
import de.mineking.discord.extensions.localization.translation
import de.mineking.discord.localization.LocalizationBundle
import de.mineking.discord.markdown.MarkdownScope
import de.mineking.discord.markdown.markdown
import de.mineking.discord.menu.Defer
import de.mineking.discord.menu.action
import de.mineking.discord.menu.components.ComponentScope
import de.mineking.discord.menu.components.stringSelect
import de.mineking.discord.menu.components.text
import de.mineking.discord.menu.message.MessageMenu
import de.mineking.discord.menu.message.components.actionRow
import de.mineking.discord.menu.message.components.container
import de.mineking.discord.menu.message.components.section
import de.mineking.discord.menu.message.components.separator
import de.mineking.discord.menu.message.components.thumbnail
import de.mineking.discord.menu.renderOnly
import de.mineking.discord.menu.runtime.MenuRuntime
import de.mineking.discord.menu.state.MemoryKey
import de.mineking.discord.menu.state.MemoryScope
import de.mineking.hexo.discord.bot.HeXODiscordBot
import de.mineking.hexo.discord.bot.utils.reloadButton
import de.mineking.hexo.discord.core.jda
import de.mineking.hexo.game.model.leaderboard.Leaderboard
import de.mineking.hexo.game.model.leaderboard.LeaderboardEntry
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.utils.cache.CacheConfiguration
import de.mineking.hexo.utils.cache.EvictionStrategy
import de.mineking.hexo.utils.cache.InMemoryCache
import de.mineking.hexo.utils.cache.entries
import dev.freya02.jda.emojis.unicode.Emojis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.URI
import javax.imageio.ImageIO

context(context: DiscordToolKitContext<HeXODiscordBot>)
fun MenuRuntime.leaderboardMenu(profileMenu: MessageMenu<ProfileId>) = registerMessageMenu("leaderboard") {
    localize()
    val localization = translation<LeaderboardMenuLocalization>()

    val details = action<List<String>>("details", defer = Defer.Reply(false)) {
        launch { replyMenu(profileMenu, ProfileId(it.single())) }
        rerender() // Clear selection
    }

    container {
        section(accessory = { reloadButton() }) {
            text {
                h1(":trophy: ${localization.title()}")
            }
        }
        separator(Separator.Spacing.LARGE)

        renderOnly {
            val leaderboard = context.main.repositories.leaderboardRepository.getLeaderboard()
            leaderboardEntries(leaderboard)

            separator(Separator.Spacing.LARGE)
            actionRow {
                stringSelect(details, placeholder = localization.profileDetails()) {
                    leaderboard.players.forEach {
                        option(
                            value = it.profileId.value,
                            label = it.displayName,
                            description = rawPlayerDetails(localization, it),
                        )
                    }
                }
            }
        }
    }
}

private val ScalingServiceMemoryKey = MemoryKey<ImageScalingService>(MemoryScope.Runtime)

context(context: DiscordToolKitContext<HeXODiscordBot>)
private suspend fun ComponentScope<Section>.leaderboardEntries(leaderboard: Leaderboard) {
    val scalingService = memory(ScalingServiceMemoryKey) { ImageScalingService() }
    val images = coroutineScope {
        leaderboard.players.map {
            async {
                val data = scalingService.getScaledImage(it.image ?: FALLBACK_IMAGE_URL)
                FileUpload.fromData(data, "${it.profileId}.png")
            }
        }.awaitAll()
    }

    val discordProfiles = context.main.accountLinkRepository
        ?.getDiscordProfiles(leaderboard.players.map { it.profileId })
        .orEmpty()

    val localization = translation<LeaderboardMenuLocalization>()
    leaderboard.players.forEachIndexed { index, entry ->
        section(accessory = { thumbnail(images[index]) }) {
            text {
                h2 {
                    val rank = when (index) {
                        0 -> Emojis.FIRST_PLACE.formatted
                        1 -> Emojis.SECOND_PLACE.formatted
                        2 -> Emojis.THIRD_PLACE.formatted
                        else -> "${index + 1}."
                    }
                    text("$rank ${entry.displayName}")

                    discordProfiles[entry.profileId]?.let {
                        raw(" (${it.jda.asMention})")
                    }
                }

                playerDetails(localization, entry)
            }
        }
    }
}

private fun rawPlayerDetails(localization: LeaderboardMenuLocalization, entry: LeaderboardEntry): String {
    val content = markdown {
        playerDetails(localization, entry)
    }
    return MarkdownSanitizer.sanitize(content, MarkdownSanitizer.SanitizationStrategy.REMOVE)
}

private fun MarkdownScope.playerDetails(localization: LeaderboardMenuLocalization, entry: LeaderboardEntry) {
    fun delimiter() = raw(", ")

    fun property(label: String, value: String) {
        bold {
            code(value)
        }
        text(" $label")
    }

    property(localization.played(), "${entry.totalGames.total}")
    delimiter()

    property(localization.won(), "${entry.totalGames.won}")
    delimiter()

    property(localization.elo(), "${entry.rating.elo}")
}

private class ImageScalingService {
    private val cache = InMemoryCache(CacheConfiguration<String, ByteArray>(
        sizeLimit = 10.entries,
        expiration = null,
        evictionStrategy = EvictionStrategy.LeastRecentlyUsed,
    ))

    suspend fun getScaledImage(url: String) = cache.getOrPut(url) {
        rescaleImage(url)
    }

    private suspend fun rescaleImage(url: String) = withContext(Dispatchers.IO) {
        val image = ImageIO.read(URI(url).toURL())
        val result = BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB)

        val graphics = result.createGraphics()
        graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR,
        )
        graphics.clip = RoundRectangle2D.Float(48f, 8f, 80f, 80f, 24f, 24f)
        graphics.drawImage(image, 48, 8, 80, 80, null)
        graphics.dispose()

        val bytes = ByteArrayOutputStream()
        ImageIO.write(result, "png", bytes)
        bytes.toByteArray()
    }
}

@LocalizationBundle("leaderboard")
internal interface LeaderboardMenuLocalization {
    fun title(): String
    fun profileDetails(): String

    fun played(): String
    fun won(): String
    fun elo(): String
}
