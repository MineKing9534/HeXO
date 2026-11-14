package de.mineking.hexo.discord.bot

import de.mineking.discord.await
import de.mineking.discord.extensions.SwitchEmojis
import de.mineking.discord.extensions.installDiscordToolKit
import de.mineking.discord.localization.runtime.TranslationManager
import de.mineking.discord.localization.source.yaml.YamlCatalogLoader
import de.mineking.discord.menu.message.MessageMenu
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.parse.NotationParser
import de.mineking.hexo.board.render.BoardRenderer
import de.mineking.hexo.board.render.image.theme.Theme
import de.mineking.hexo.discord.bot.commands.accountLinkCommand
import de.mineking.hexo.discord.bot.commands.gameCommand
import de.mineking.hexo.discord.bot.commands.leaderboardCommand
import de.mineking.hexo.discord.bot.commands.profileCommand
import de.mineking.hexo.discord.bot.commands.profileUserCommand
import de.mineking.hexo.discord.bot.commands.renderHexoMessageCommand
import de.mineking.hexo.discord.bot.commands.renderHexoModalCommand
import de.mineking.hexo.discord.bot.commands.renderHexoSlashCommand
import de.mineking.hexo.discord.bot.commands.themeCommand
import de.mineking.hexo.discord.bot.config.UserThemeRepository
import de.mineking.hexo.discord.bot.menus.GameMenuParameter
import de.mineking.hexo.discord.bot.menus.NotationMenuParameter
import de.mineking.hexo.discord.bot.menus.accountLinkMenu
import de.mineking.hexo.discord.bot.menus.boardMenu
import de.mineking.hexo.discord.bot.menus.gameMenu
import de.mineking.hexo.discord.bot.menus.leaderboardMenu
import de.mineking.hexo.discord.bot.menus.notationMenu
import de.mineking.hexo.discord.bot.menus.profileMenu
import de.mineking.hexo.discord.core.DiscordUserId
import de.mineking.hexo.discord.oauth2.OAuth2TokenRepository
import de.mineking.hexo.game.model.RepositoryContainer
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.link.AccountLinkRepository
import dev.freya02.jda.emojis.unicode.Emojis
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.utils.messages.MessageRequest
import java.time.Duration
import kotlin.coroutines.CoroutineContext

internal val logger = KotlinLogging.logger {}

class HeXODiscordBot(
    val coroutineScope: CoroutineScope,
    val repositories: RepositoryContainer,
    val accountLinkRepository: AccountLinkRepository?,
    val discordUserAuthenticationRepository: OAuth2TokenRepository?,
    val userThemeRepository: UserThemeRepository?,
    val notationParser: NotationParser,
    val boardRenderer: BoardRenderer<Theme, BoardAttachment>,
    val publicUrl: String?,
    token: String,
) {
    init {
        MessageRequest.setDefaultUseComponentsV2(true)
    }

    val jda = JDABuilder.createLight(token)
        .setStatus(OnlineStatus.ONLINE)
        .setActivity(Activity.playing("HeXO"))
        .addEventListeners(object : ListenerAdapter() {
            override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
                if (event.reaction.emoji != Emojis.WASTEBASKET) return

                coroutineScope.launch {
                    val message = event.retrieveMessage().await()
                    if (message.author != event.jda.selfUser) return@launch
                    if (message.interactionMetadata?.user?.idLong != event.userIdLong) return@launch

                    message.delete().await()
                }
            }
        })
        .build()

    val translationManager = TranslationManager(DiscordLocale.ENGLISH_US, YamlCatalogLoader.resources())

    init {
        coroutineScope.launch {
            installDiscordToolKit(jda, this@HeXODiscordBot, coroutineScope) {
                translation(translationManager)

                emojis {
                    register(SwitchEmojis)
                    register(PlayerEmojis)
                    register(NotationEmojis)
                    register(ThemeEmojis)
                    register(IconEmojis)
                }

                data class Menus(
                    val accountLinkMenu: MessageMenu<Unit>?,
                    val notationMenu: MessageMenu<NotationMenuParameter>,
                    val gameMenu: MessageMenu<GameMenuParameter>,
                    val boardMenu: MessageMenu<Board>,
                    val profileMenu: MessageMenu<ProfileId>,
                    val leaderboardMenu: MessageMenu<Unit>,
                )

                val menus = menus(LocalizedMenuErrorHandler(context)) {
                    val accountLinkMenu =
                        if (accountLinkRepository != null && discordUserAuthenticationRepository != null) accountLinkMenu() else null
                    val notationMenu = notationMenu()
                    val gameMenu = gameMenu(notationMenu)
                    val boardMenu = boardMenu(notationMenu)
                    val profileMenu = profileMenu()
                    val leaderboardMenu = leaderboardMenu(profileMenu)

                    Menus(accountLinkMenu, notationMenu, gameMenu, boardMenu, profileMenu, leaderboardMenu)
                }

                commands(LocalizedCommandErrorHandler(context)) {
                    global {
                        interactionContexts = InteractionContextType.ALL
                        integrationTypes = IntegrationType.ALL

                        if (menus.accountLinkMenu != null) accountLinkCommand(menus.accountLinkMenu)
                        gameCommand(menus.gameMenu)
                        leaderboardCommand(menus.leaderboardMenu)
                        profileCommand(menus.profileMenu)
                        profileUserCommand(menus.profileMenu)

                        renderHexoMessageCommand()
                        renderHexoModalCommand()
                        renderHexoSlashCommand(menus.boardMenu)

                        themeCommand()
                    }

                    autoSync()
                }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun getUserTheme(user: DiscordUserId) = try {
        userThemeRepository
            ?.getCurrentUserTheme(user)
            ?: Theme.Default
    } catch (e: Exception) {
        currentCoroutineContext().ensureActive()
        logger.error(e) { "Failed to fetch user theme" }
        Theme.Default
    }

    fun shutdown() {
        jda.shutdown()
        if (!jda.awaitShutdown(Duration.ofSeconds(10))) {
            jda.shutdownNow()
            jda.awaitShutdown()
        }
    }
}

class DiscordLocalization(val translationManager: TranslationManager, val locale: DiscordLocale) : CoroutineContext.Element {
    override val key = Key
    companion object Key : CoroutineContext.Key<DiscordLocalization>
}

val UserSnowflake.userId get() = DiscordUserId(idLong)
