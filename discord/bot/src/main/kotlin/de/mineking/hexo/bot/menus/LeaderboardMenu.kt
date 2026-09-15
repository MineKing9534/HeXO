package de.mineking.hexo.bot.menus

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import de.mineking.discord.localization.Locale
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.LocalizationParameter
import de.mineking.discord.localization.Localize
import de.mineking.discord.ui.UIManager
import de.mineking.discord.ui.builder.append
import de.mineking.discord.ui.builder.components.buildTextDisplay
import de.mineking.discord.ui.builder.components.localizedTextDisplay
import de.mineking.discord.ui.builder.components.message.actionRow
import de.mineking.discord.ui.builder.components.message.button
import de.mineking.discord.ui.builder.components.message.container
import de.mineking.discord.ui.builder.components.message.section
import de.mineking.discord.ui.builder.components.message.separator
import de.mineking.discord.ui.builder.components.message.thumbnail
import de.mineking.discord.ui.builder.components.selectOption
import de.mineking.discord.ui.builder.components.stringSelect
import de.mineking.discord.ui.builder.h2
import de.mineking.discord.ui.localize
import de.mineking.discord.ui.message.MessageMenu
import de.mineking.discord.ui.message.MessageMenuConfig
import de.mineking.discord.ui.message.createMessageComponent
import de.mineking.discord.ui.message.disableComponents
import de.mineking.discord.ui.parameter
import de.mineking.discord.ui.registerLocalizedMenu
import de.mineking.discord.ui.renderValue
import de.mineking.discord.ui.setup
import de.mineking.hexo.bot.escapeMarkdown
import de.mineking.hexo.bot.utils.effectiveLocale
import de.mineking.hexo.game.model.leaderboard.Leaderboard
import de.mineking.hexo.game.model.leaderboard.LeaderboardEntry
import de.mineking.hexo.game.model.leaderboard.LeaderboardRepository
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.link.AccountLinkRepository
import dev.freya02.jda.emojis.unicode.Emojis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.URI
import javax.imageio.ImageIO

fun UIManager.leaderboardMenu(
    leaderboardRepository: LeaderboardRepository,
    accountLinkRepository: AccountLinkRepository?,
    profileMenu: MessageMenu<ProfileMenuParameter, *>,
) = registerLocalizedMenu<Interaction, LeaderboardMenuLocalization>("leaderboard") { localization ->
    val reloadButton = register(button("reload", emoji = Emojis.RECYCLE) {
        disableComponents(message).queue()
        forceUpdate()
    })

    val locale = parameter({ DiscordLocale.UNKNOWN }, { it.effectiveLocale }, { effectiveLocale })
    localize(locale)

    +container {
        +section(
            accessory = reloadButton,
            localizedTextDisplay("title"),
        )
        +separator(spacing = Separator.Spacing.LARGE)

        val entries = renderValue {
            val leaderboard = leaderboardRepository.getLeaderboard()
            +leaderboardEntries(localization, locale, accountLinkRepository, leaderboard)

            leaderboard.players
        } ?: emptyList()

        +separator(spacing = Separator.Spacing.LARGE)
        +actionRow(stringSelect("details", options = entries.map {
            selectOption(
                it.profileId.value,
                label = it.displayName,
                description = MarkdownSanitizer.sanitize(localization.playerDetails(locale, it)),
            )
        }) {
            val defer = deferReply().submit()
            coroutineScope {
                launch {
                    message.editMessage(render()).queue()
                }

                launch {
                    defer.await()
                    val profileId = ProfileId(event.values[0])
                    hook.editOriginal(profileMenu.createInitial(ProfileMenuParameter(event, profileId))).queue()
                }
            }
        })
    }
}

private suspend fun MessageMenuConfig<*, *>.leaderboardEntries(
    localization: LeaderboardMenuLocalization,
    locale: DiscordLocale,
    accountLinkRepository: AccountLinkRepository?,
    leaderboard: Leaderboard,
) = coroutineScope {
    val scalingService = setup { ImageScalingService() }
    val images = leaderboard.players.map {
        async {
            val data = scalingService.getScaledImage(it.image ?: FALLBACK_IMAGE_URL)
            FileUpload.fromData(data, "${it.profileId}.png")
        }
    }

    val discordProfiles = accountLinkRepository
        ?.getDiscordProfiles(leaderboard.players.map { it.profileId })
        ?: emptyMap()

    createMessageComponent(
        leaderboard.players.mapIndexed { index, entry ->
            section(
                accessory = thumbnail(images[index].await()),
                buildTextDisplay {
                    +h2 {
                        val rank = when (index) {
                            0 -> Emojis.FIRST_PLACE.formatted
                            1 -> Emojis.SECOND_PLACE.formatted
                            2 -> Emojis.THIRD_PLACE.formatted
                            else -> "${index + 1}."
                        }
                        append("$rank ")
                        append(entry.displayName.escapeMarkdown())

                        discordProfiles[entry.profileId]?.also {
                            append(" (${it.asMention})")
                        }
                    }
                    append(localization.playerDetails(locale, entry))
                },
            )
        },
    )
}

private class ImageScalingService {
    private val cache = Caffeine.newBuilder()
        .maximumSize(10)
        .asCache<String, ByteArray>()

    suspend fun getScaledImage(url: String) = cache.get(url) {
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

interface LeaderboardMenuLocalization : LocalizationFile {
    @Localize("menu.leaderboard.player.details")
    fun playerDetails(@Locale locale: DiscordLocale, @LocalizationParameter entry: LeaderboardEntry): String
}
