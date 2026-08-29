package de.mineking.hexo.bot

import de.mineking.discord.Manager
import de.mineking.discord.discordToolKit
import de.mineking.discord.localization.DefaultLocalizationManager
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.read
import de.mineking.discord.ui.message.MessageMenu
import de.mineking.discord.utils.await
import de.mineking.discord.utils.listen
import de.mineking.discord.withLocalization
import de.mineking.hexo.board.parse.BoardParser
import de.mineking.hexo.board.render.BoardRenderer
import de.mineking.hexo.board.render.image.theme.Theme
import de.mineking.hexo.bot.commands.accountLinkCommand
import de.mineking.hexo.bot.commands.gameCommand
import de.mineking.hexo.bot.commands.leaderboardCommand
import de.mineking.hexo.bot.commands.modalCommand
import de.mineking.hexo.bot.commands.profileCommand
import de.mineking.hexo.bot.commands.profileUserCommand
import de.mineking.hexo.bot.commands.renderHexoMessageCommand
import de.mineking.hexo.bot.commands.renderHexoSlashCommand
import de.mineking.hexo.bot.commands.themeCommand
import de.mineking.hexo.bot.menus.GameMenuParameter
import de.mineking.hexo.bot.menus.NotationMenuParameter
import de.mineking.hexo.bot.menus.ProfileMenuParameter
import de.mineking.hexo.bot.menus.accountLinkMenu
import de.mineking.hexo.bot.menus.gameMenu
import de.mineking.hexo.bot.menus.leaderboardMenu
import de.mineking.hexo.bot.menus.notationMenu
import de.mineking.hexo.bot.menus.profileMenu
import de.mineking.hexo.bot.utils.installErrorHandling
import de.mineking.hexo.bot.utils.updateLinkedRoleMetadata
import de.mineking.hexo.discord.bot.config.UserThemeRepository
import de.mineking.hexo.discord.core.DiscordUserId
import de.mineking.hexo.game.model.RepositoryContainer
import de.mineking.hexo.link.AccountLinkRepository
import de.mineking.hexo.link.oauth2.DiscordUserAuthenticationRepository
import dev.freya02.jda.emojis.unicode.Emojis
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import net.dv8tion.jda.api.utils.messages.MessageRequest
import java.time.Duration

internal val logger = KotlinLogging.logger {}

val Manager.main get() = manager.bot as HeXODiscordBot

class HeXODiscordBot(
    private val repositories: RepositoryContainer,
    private val accountLinkRepository: AccountLinkRepository?,
    private val discordUserAuthenticationRepository: DiscordUserAuthenticationRepository?,
    val userThemeRepository: UserThemeRepository?,
    val notationParser: BoardParser,
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
        .build()

    val emojiManager = EmojiManager(jda)

    private lateinit var notationMenu: MessageMenu<NotationMenuParameter, *>
    private lateinit var gameMenu: MessageMenu<GameMenuParameter, *>
    private lateinit var profileMenu: MessageMenu<ProfileMenuParameter, *>
    private lateinit var leaderboardMenu: MessageMenu<Interaction, *>
    private lateinit var accountLinkMenu: MessageMenu<IModalCallback, *>

    val dtk = discordToolKit(jda, this)
        .withLocalization<_, DefaultLocalizationManager>()
        .withUIManager {
            localize()
            installErrorHandling()

            notationMenu = notationMenu(repositories.finishedGameRepository)
            gameMenu = gameMenu(repositories.finishedGameRepository, notationMenu)
            profileMenu = profileMenu(repositories.profileRepository, accountLinkRepository)
            leaderboardMenu = leaderboardMenu(repositories.leaderboardRepository, accountLinkRepository, profileMenu)
            if (discordUserAuthenticationRepository != null && accountLinkRepository != null) {
                accountLinkMenu = accountLinkMenu(
                    discordAuthRepository = discordUserAuthenticationRepository,
                    accountLinkRepository = accountLinkRepository,
                    profileRepository = repositories.profileRepository,
                )
            }
        }
        .withCommandManager {
            localize()
            installErrorHandling()

            +renderHexoMessageCommand()
            +renderHexoSlashCommand()
            +modalCommand()

            +gameCommand(gameMenu)
            +leaderboardCommand(leaderboardMenu)

            +profileCommand(accountLinkRepository, repositories.profileRepository, profileMenu)
            if (accountLinkRepository != null) +profileUserCommand(accountLinkRepository, profileMenu)
            if (::accountLinkMenu.isInitialized) +accountLinkCommand(accountLinkMenu)

            if (userThemeRepository != null) +themeCommand()

            updateCommands().queue()
        }
        .build()

    init {
        if (publicUrl != null) {
            runBlocking {
                dtk.updateLinkedRoleMetadata()
            }
        }

        jda.registerMessageDeleteListener()
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun getUserTheme(user: DiscordUserId) = try {
        userThemeRepository
            ?.getCurrentUserTheme(user)
            ?: Theme.Default
    } catch (e: Exception) {
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

private fun JDA.registerMessageDeleteListener() {
    listen<MessageReactionAddEvent> {
        if (reaction.emoji != Emojis.WASTEBASKET) return@listen

        val message = retrieveMessage().await()
        if (message.author != jda.selfUser) return@listen
        if (message.interactionMetadata?.user?.idLong != userIdLong) return@listen

        message.delete().queue()
    }
}

inline fun <reified L : LocalizationFile> HeXODiscordBot.localization(): L = dtk.localizationManager.read<L>()
val UserSnowflake.userId get() = DiscordUserId(idLong)

fun String.escapeMarkdown() = MarkdownSanitizer.sanitize(this, MarkdownSanitizer.SanitizationStrategy.ESCAPE)
