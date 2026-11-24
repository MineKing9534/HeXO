package de.mineking.hexo.discord.bot.commands

import de.mineking.discord.LocalizedString
import de.mineking.discord.command.CommandRegistrationScope
import de.mineking.discord.command.ReplyMode
import de.mineking.discord.command.SlashCommandBuilder
import de.mineking.discord.command.option.CommandChoice
import de.mineking.discord.command.option.CommandOption
import de.mineking.discord.command.option.OptionRequirement
import de.mineking.discord.command.option.required
import de.mineking.discord.extensions.DiscordToolKitContext
import de.mineking.discord.extensions.localization.localizedWith
import de.mineking.discord.localization.LocalizationBundle
import de.mineking.discord.menu.message.MessageMenu
import de.mineking.hexo.discord.bot.HeXODiscordBot
import de.mineking.hexo.discord.bot.menus.GameMenuParameter
import de.mineking.hexo.game.model.game.FinishedGame
import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.utils.cache.CacheConfiguration
import de.mineking.hexo.utils.cache.EvictionStrategy
import de.mineking.hexo.utils.cache.ExpirationStrategy
import de.mineking.hexo.utils.cache.InMemoryCache
import de.mineking.hexo.utils.cache.entries
import de.mineking.hexo.utils.types.Selector
import de.mineking.hexo.utils.types.limit
import de.mineking.hexo.utils.types.orNull
import dev.freya02.jda.emojis.unicode.Emojis
import kotlinx.coroutines.flow.toList
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaInstant

context(_: DiscordToolKitContext<HeXODiscordBot>)
fun CommandRegistrationScope.gameCommand(gameMenu: MessageMenu<GameMenuParameter>) = localizedWith<GameCommandLocalization> {
    slashCommand("game", localized { description() }) {
        val id = gameOption("id", localized { optionDescription() }, required())

        execute(ReplyMode.Defer(false)) {
            replyMenu(gameMenu, GameMenuParameter(id.value(), Int.MAX_VALUE))
        }
    }
}

private val winnerEmoji = Emojis.FIRST_PLACE.formatted

context(context: DiscordToolKitContext<HeXODiscordBot>)
private fun <T> SlashCommandBuilder.gameOption(
    name: String,
    description: LocalizedString,
    requirement: OptionRequirement<GameId, T>,
): CommandOption<T> {
    val cache = InMemoryCache<String, List<FinishedGame>>(CacheConfiguration(
        sizeLimit = 20.entries,
        expiration = ExpirationStrategy.AfterWrite(3.minutes),
        evictionStrategy = EvictionStrategy.LeastFrequentlyUsed,
    ))

    return stringOption(name, description, requirement.mapInput {
        GameId(it.split("/").last())
    }) {
        autocomplete {
            val games = cache.getOrPut(focusedText) {
                val profile = context.main.repositories.profileRepository.getProfilesByName(it).firstOrNull()
                val profileGames = profile
                    ?.let { context.main.repositories.finishedGameRepository.getProfileHistory(profile.id, Selector.limit(20)) }
                    ?.orNull()
                    ?.toList()
                profileGames ?: context.main.repositories.finishedGameRepository.getGlobalHistory(Selector.limit(20)).toList()
            }

            games.map { game ->
                val winnerId = game.result.winner?.id
                val players = game.players.mapIndexed { index, player ->
                    when {
                        player.id != winnerId -> player.displayName
                        index == 0 -> "$winnerEmoji ${player.displayName}"
                        else -> "${player.displayName} $winnerEmoji"
                    }
                }.joinToString(" vs. ")
                val date = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                    .withLocale(interaction.userLocale.toLocale())
                    .format(game.startedAt.toJavaInstant().atZone(ZoneOffset.UTC))

                CommandChoice(game.id.value, label = "$players • $date")
            }
        }
    }
}

@LocalizationBundle("command", ".game")
internal interface GameCommandLocalization {
    fun description(): String
    fun optionDescription(): String
}
