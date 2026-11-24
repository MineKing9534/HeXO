package de.mineking.hexo.discord.bot.utils

import de.mineking.discord.extensions.DiscordToolKitContext
import de.mineking.discord.extensions.localization.translation
import de.mineking.discord.localization.LocalizationBundle
import de.mineking.discord.menu.Action
import de.mineking.discord.menu.DeclarationScope
import de.mineking.discord.menu.action
import de.mineking.discord.menu.components.ComponentScope
import de.mineking.discord.menu.message.components.button
import de.mineking.discord.menu.message.components.linkButton
import de.mineking.hexo.discord.bot.IconEmojis
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent

fun DeclarationScope.reloadAction() = action("reload") { rerender() }

context(context: DiscordToolKitContext<*>)
fun ComponentScope<Button>.reloadButton(
    action: Action<GenericInteractionCreateEvent, Unit> = reloadAction(),
) {
    val localization = translation<ComponentsLocalization>()
    button(action, emoji = context.emojis[IconEmojis.Reload], label = localization.reloadLabel())
}

context(context: DiscordToolKitContext<*>)
fun ComponentScope<Button>.backButton(action: Action<GenericInteractionCreateEvent, Unit>) {
    val localization = translation<ComponentsLocalization>()
    button(action, emoji = context.emojis[IconEmojis.Back], label = localization.backLabel())
}

context(context: DiscordToolKitContext<*>)
fun ComponentScope<Button>.viewButton(url: String) {
    val localization = translation<ComponentsLocalization>()
    linkButton(url, label = localization.viewLabel(), emoji = context.emojis[IconEmojis.View])
}

@LocalizationBundle("components")
interface ComponentsLocalization {
    fun reloadLabel(): String
    fun backLabel(): String
    fun viewLabel(): String
}
