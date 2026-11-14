package de.mineking.hexo.discord.core

import net.dv8tion.jda.api.entities.UserSnowflake

val DiscordUserId.jda get() = UserSnowflake.fromId(value)
