package de.mineking.hexo.discord.bot

import de.mineking.discord.command.runtime.CommandErrorContext
import de.mineking.discord.command.runtime.CommandErrorHandler
import de.mineking.discord.extensions.DiscordToolKitContext
import de.mineking.discord.extensions.localization.locale
import de.mineking.discord.extensions.reply
import de.mineking.discord.interaction.InteractionReplyScope
import de.mineking.discord.localization.LocalizationBundle
import de.mineking.discord.localization.LocalizationMessageGroup
import de.mineking.discord.localization.LocalizationOperation
import de.mineking.discord.markdown.MarkdownScope
import de.mineking.discord.markdown.markdown
import de.mineking.discord.menu.components.text
import de.mineking.discord.menu.message.components.container
import de.mineking.discord.menu.runtime.InvalidWireFormatVersion
import de.mineking.discord.menu.runtime.MenuErrorContext
import de.mineking.discord.menu.runtime.MenuErrorHandler
import de.mineking.discord.menu.runtime.MenuStateTooLarge
import net.dv8tion.jda.api.interactions.Interaction

internal class ValidationException(val title: String, override val message: String) : RuntimeException(message), ErrorResponseMessage<Unit> {
    override fun title() = title
    override fun description(parameter: Unit) = message

    override fun throwValidationError(parameter: Unit, details: String?): Nothing = throw this
}

private val Interaction.effectiveLocale get() = if (isFromGuild) guildLocale else userLocale

context(_: DiscordToolKitContext<*>)
private suspend fun InteractionReplyScope<*>.replyError(error: ErrorResponseMessage<Unit>) {
    replyError(error.title()) {
        raw(error.description(Unit))
    }
}

context(_: DiscordToolKitContext<*>)
private suspend fun InteractionReplyScope<*>.replyError(title: String, description: MarkdownScope.() -> Unit) {
    reply(ephemeral = true) {
        container(accentColor = 0xDB2B14) {
            text {
                h2(title)
                description()
            }
        }
    }
}

class LocalizedCommandErrorHandler(val context: DiscordToolKitContext<*>) : CommandErrorHandler {
    override suspend fun CommandErrorContext.handle() = with(context) {
        val exception = exception
        if (exception is ValidationException) {
            if (canRespond) replyError(exception)
            return@with
        }

        logger.error(exception) { "Unexpected error during command execution ($stage) for $logicalPath" }

        if (canRespond) {
            val localization = translationManager.bind<ErrorHandlingLocalization>(interaction.effectiveLocale)
            replyError(localization.commandError())
        }
    }
}

class LocalizedMenuErrorHandler(val context: DiscordToolKitContext<*>) : MenuErrorHandler {
    override suspend fun MenuErrorContext.handle() = with(this@LocalizedMenuErrorHandler.context) {
        suspend fun replyError(text: ErrorHandlingLocalization.() -> ErrorResponseMessage<Unit>) {
            val locale = context(context) { locale }
            val localization = translationManager.bind<ErrorHandlingLocalization>(locale)
            replyError(localization.text())
        }

        when (val exception = exception) {
            is ValidationException -> replyError(exception)
            is MenuStateTooLarge -> replyError {
                menuErrorStateTooLarge().bind(
                    StateTooLargeError(
                        required = exception.requiredCharacters,
                        available = exception.availableCharacters,
                    ),
                )
            }
            is InvalidWireFormatVersion -> replyError { menuErrorInvalidVersion() }
            else -> {
                logger.error(exception) { "Unexpected error during command execution ($stage) for $menuId" }
                replyError { menuError() }
            }
        }
    }
}

@LocalizationMessageGroup
interface ErrorResponseMessage<P> {
    fun title(): String
    fun description(parameter: P): String

    @LocalizationOperation
    fun bind(value: P) = object : ErrorResponseMessage<Unit> {
        override fun title() = this@ErrorResponseMessage.title()
        override fun description(parameter: Unit) = this@ErrorResponseMessage.description(value)
    }

    @LocalizationOperation
    fun throwValidationError(parameter: P, details: String? = null): Nothing {
        throw ValidationException(title(), markdown {
            line {
                raw(description(parameter))
                if (details != null) text(":")
            }
            if (details != null) codeBlock(details)
        })
    }
}

fun ErrorResponseMessage<Unit>.throwValidationError(details: String? = null): Nothing = throwValidationError(Unit, details)

internal data class StateTooLargeError(val required: Int, val available: Int)

@LocalizationBundle("errorHandling")
internal interface ErrorHandlingLocalization {
    fun commandError(): ErrorResponseMessage<Unit>
    fun menuError(): ErrorResponseMessage<Unit>
    fun menuErrorStateTooLarge(): ErrorResponseMessage<StateTooLargeError>
    fun menuErrorInvalidVersion(): ErrorResponseMessage<Unit>
}
