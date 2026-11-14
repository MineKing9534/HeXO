package de.mineking.hexo.discord.bot

import de.mineking.discord.emoji.EmojiKey
import de.mineking.discord.emoji.EmojiProvider
import de.mineking.discord.emoji.EmojiRegistrar
import net.dv8tion.jda.api.entities.Icon

context(key: EmojiKey)
internal fun readEmoji(): Icon {
    val path = "emoji/${key.javaClass.simpleName.removeSuffix("Emojis").lowercase()}/${key.discordName}.png"

    val resource = requireNotNull(key.javaClass.classLoader.getResourceAsStream(path)) { "Emoji '$path' not found" }
    return resource.use { Icon.from(it) }
}

enum class PlayerEmojis : EmojiProvider, EmojiKey {
    X,
    O,
    ;

    override val discordName = name.lowercase()
    override fun getImage() = readEmoji()

    companion object : EmojiRegistrar {
        override val namespace = "player"
        override val emojis = entries.associateWith { it }
    }
}

enum class NotationEmojis : EmojiProvider, EmojiKey {
    CRN,
    MRN,
    Combined,
    HTTTX,
    Tyto,
    ;

    override val discordName = name.lowercase()
    override fun getImage() = readEmoji()

    companion object : EmojiRegistrar {
        override val namespace = "notation"
        override val emojis = entries.associateWith { it }
    }
}

enum class IconEmojis : EmojiProvider, EmojiKey {
    Link,
    Unlink,
    Send,
    Check,
    Selected,
    Delete,
    Create,
    Edit,
    Reload,
    Back,
    View,
    Lock,
    Unlock,
    First,
    Previous,
    Dialog,
    Next,
    Last,
    Palette,
    Paintbrush,
    Number,
    ;

    override val discordName = name.lowercase()
    override fun getImage() = readEmoji()

    companion object : EmojiRegistrar {
        override val namespace = "icon"
        override val emojis = entries.associateWith { it }
    }
}
