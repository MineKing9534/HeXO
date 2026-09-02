package de.mineking.hexo.bot

import de.mineking.discord.utils.await
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Icon
import net.dv8tion.jda.api.entities.emoji.ApplicationEmoji
import net.dv8tion.jda.api.entities.emoji.Emoji
import java.util.concurrent.ConcurrentHashMap

enum class CustomEmoji(val path: String) {
    PlayerX("player_x"),
    PlayerO("player_o"),

    SwitchOn("switch_on"),
    SwitchOff("switch_off"),

    Link("link"),
    Unlink("unlink"),

    NotationCRN("notation/crn"),
    NotationMRN("notation/mrn"),

    ThemeHDS("theme/hds"),
    ThemeHTTTX("theme/htttx"),
    ThemeTyto("theme/tyto"),
    ThemeOmok("theme/omok"),
}

val CustomEmoji.discordName get() = name.lowercase()

class EmojiManager(val jda: JDA) {
    private val emojis = ConcurrentHashMap<CustomEmoji, Emoji>()

    init {
        runBlocking {
            val data = jda.retrieveApplicationEmojis().await()
            data.filter { emoji -> CustomEmoji.entries.none { it.discordName == emoji.name } }.forEach {
                logger.info { "Deleting unused emoji: ${it.name}" }
                it.delete().queue()
            }

            coroutineScope {
                CustomEmoji.entries.forEach {
                    launch {
                        it.register(data)
                    }
                }
            }
        }
    }

    private suspend fun CustomEmoji.register(data: List<ApplicationEmoji>) {
        val emoji = data.find { this.discordName == it.name }

        val currentImage = emoji?.image?.downloadAsIcon()?.await()
        val expectedImage = loadIcon(this)

        if (currentImage?.encoding == expectedImage.encoding) {
            emojis[this] = emoji
            return
        }

        if (emoji == null) {
            logger.info { "Creating missing emoji: $discordName" }
        } else {
            logger.info { "Overwriting existing emoji: $discordName" }
        }

        val createAction = jda.createApplicationEmoji(discordName, expectedImage)
        val action = emoji?.delete()?.flatMap { createAction } ?: createAction
        emojis[this] = action.await()
    }

    operator fun get(type: CustomEmoji) = emojis[type]!!
}

private fun loadIcon(type: CustomEmoji) = EmojiManager::class.java.classLoader
    .getResourceAsStream("emoji/${type.path}.png")
    ?.use { Icon.from(it) }
    ?: error("Could not load icon for emoji type: $type")
