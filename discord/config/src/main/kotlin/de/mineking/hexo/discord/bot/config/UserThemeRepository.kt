package de.mineking.hexo.discord.bot.config

import de.mineking.hexo.board.render.image.theme.BaseTheme
import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.board.render.image.theme.DefaultTheme
import de.mineking.hexo.board.render.image.theme.Theme
import de.mineking.hexo.database.HexoDatabaseManager
import de.mineking.hexo.database.UnexpectedDatabaseErrorException
import de.mineking.hexo.database.UniqueViolationError
import de.mineking.hexo.database.mapNullableResult
import de.mineking.hexo.database.throwOnDatabaseError
import de.mineking.hexo.discord.bot.config.database.ThemeDataTable
import de.mineking.hexo.discord.bot.config.database.UserThemeTable
import de.mineking.hexo.discord.core.DiscordUserId
import de.mineking.hexo.utils.types.IError
import de.mineking.hexo.utils.types.Result
import de.mineking.hexo.utils.types.mapError
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.leftJoin
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.core.vendors.ForUpdateOption
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.updateReturning
import org.jetbrains.exposed.v1.jdbc.upsert
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

sealed interface CustomThemeQueryError : IError
sealed interface CustomThemeCreateError : IError
sealed interface CustomThemeUpdateError : IError
sealed interface CustomThemeDeleteError : IError

object CustomThemeNotFoundError : CustomThemeQueryError, CustomThemeDeleteError, CustomThemeUpdateError
object CustomThemeAlreadyExists : CustomThemeCreateError
object MissingCustomThemePermissionError : CustomThemeUpdateError, CustomThemeDeleteError

sealed interface CustomThemeSelector {
    data class Id(val id: CustomThemeId) : CustomThemeSelector
    data class Name(val owner: DiscordUserId, val name: String) : CustomThemeSelector
}

sealed interface UserThemeSelection {
    data class Custom(val selector: CustomThemeSelector) : UserThemeSelection
    data class Default(val theme: DefaultTheme) : UserThemeSelection
}

class UserThemeRepository(private val database: HexoDatabaseManager) {
    private fun ResultRow.mapToCustomTheme() = CustomTheme(
        id = this[ThemeDataTable.id].value,
        owner = this[ThemeDataTable.owner],
        name = this[ThemeDataTable.name],
        base = this[ThemeDataTable.base],
        overrides = this[ThemeDataTable.overrides],
    )

    private fun CustomThemeSelector.toCondition() = when (this) {
        is CustomThemeSelector.Id -> ThemeDataTable.id eq id
        is CustomThemeSelector.Name -> (ThemeDataTable.owner eq owner) and (ThemeDataTable.name eq name)
    }

    private fun UpdateBuilder<*>.bindTheme(theme: BaseTheme) {
        val base = DefaultTheme.entries.single { it.theme::class.isInstance(theme) }
        this[ThemeDataTable.base] = base

        val overrides = base.theme::class.primaryConstructor!!.parameters
            .mapNotNull { param ->
                val name = param.name!!

                @Suppress("UNCHECKED_CAST")
                val property = base.theme::class.memberProperties
                    .first { it.name == name }
                    as KProperty1<BaseTheme, Any?>

                val defaultValue = property.get(base.theme)
                val actual = property.get(theme)

                if (actual == defaultValue) return@mapNotNull null

                val wrapped = when (actual) {
                    is Double -> ThemeOverrideValue.DoubleValue(actual)
                    is Color -> ThemeOverrideValue.ColorValue(actual)
                    else -> error("Unsupported theme parameter type ${param.type} ($name)")
                }

                name to wrapped
            }
            .toMap()

        this[ThemeDataTable.overrides] = overrides
    }

    suspend fun listUserThemes(user: DiscordUserId): List<CustomTheme> {
        return database.transaction(readOnly = true) {
            ThemeDataTable.selectAll()
                .where(ThemeDataTable.owner eq user)
                .map { it.mapToCustomTheme() }
        }.throwOnDatabaseError()
    }

    suspend fun getCurrentUserTheme(user: DiscordUserId): Theme? {
        return database.transaction(readOnly = true) {
            val result = UserThemeTable.leftJoin(ThemeDataTable, onColumn = { customTheme }, otherColumn = { id })
                .selectAll()
                .where(UserThemeTable.id eq user)
                .firstOrNull() ?: return@transaction null

            val default = result[UserThemeTable.defaultTheme]
            if (default != null) return@transaction default.theme

            result.mapToCustomTheme()
        }.throwOnDatabaseError()
    }

    suspend fun setCurrentUserTheme(user: DiscordUserId, theme: UserThemeSelection): Result<Theme?, CustomThemeQueryError> {
        return database.transaction(readOnly = false) {
            val theme = when (theme) {
                is UserThemeSelection.Custom -> {
                    val theme = ThemeDataTable.selectAll()
                        .where(theme.selector.toCondition())
                        .firstOrNull()
                        ?.mapToCustomTheme()
                        ?: return@transaction Result.Error(CustomThemeNotFoundError)

                    ThemeContainer.Custom(theme)
                }
                is UserThemeSelection.Default -> ThemeContainer.Default(theme.theme)
            }

            UserThemeTable.upsert(where = { UserThemeTable.id eq user }) {
                it[UserThemeTable.id] = user

                when (theme) {
                    is ThemeContainer.Default -> {
                        it[UserThemeTable.defaultTheme] = theme.default
                        it[UserThemeTable.customTheme] = null
                    }
                    is ThemeContainer.Custom -> {
                        it[UserThemeTable.defaultTheme] = null
                        it[UserThemeTable.customTheme] = theme.theme.id
                    }
                }
            }

            Result.Success(theme.theme)
        }.throwOnDatabaseError()
    }

    suspend fun createCustomTheme(owner: DiscordUserId, name: String, theme: BaseTheme): Result<CustomTheme, CustomThemeCreateError> {
        return database.transaction(readOnly = false) {
            ThemeDataTable.insertReturning {
                it[ThemeDataTable.owner] = owner
                it[ThemeDataTable.name] = name

                it.bindTheme(theme)
            }.first().mapToCustomTheme()
        }.mapError {
            when (it) {
                is UniqueViolationError if it.constraintName == "theme_data_owner_name_unique" -> CustomThemeAlreadyExists
                else -> throw UnexpectedDatabaseErrorException.Known(it)
            }
        }
    }

    context(user: DiscordUserId)
    suspend fun updateCustomThemeById(id: CustomThemeSelector, theme: BaseTheme): Result<CustomTheme, CustomThemeUpdateError> {
        return database.transaction(readOnly = false) {
            val owner = getThemeOwner(id)
                ?: return@transaction Result.Error(CustomThemeNotFoundError)

            if (owner != user) return@transaction Result.Error(MissingCustomThemePermissionError)

            val result = ThemeDataTable.updateReturning(where = { id.toCondition() }) {
                it.bindTheme(theme)
            }.first().mapToCustomTheme()

            return@transaction Result.Success(result)
        }.throwOnDatabaseError()
    }

    suspend fun getThemeById(id: CustomThemeSelector): Result<CustomTheme, CustomThemeQueryError> {
        return database.transaction(readOnly = true) {
            ThemeDataTable.selectAll()
                .where(id.toCondition())
                .firstOrNull()
                ?.mapToCustomTheme()
        }.mapNullableResult(CustomThemeNotFoundError)
    }

    context(user: DiscordUserId)
    suspend fun deleteThemeById(id: CustomThemeSelector): Result<Unit, CustomThemeDeleteError> {
        return database.transaction(readOnly = false) {
            val owner = getThemeOwner(id)
                ?: return@transaction Result.Error(CustomThemeNotFoundError)

            if (owner != user) return@transaction Result.Error(MissingCustomThemePermissionError)

            ThemeDataTable.deleteWhere { id.toCondition() }

            return@transaction Result.Success(Unit)
        }.throwOnDatabaseError()
    }

    private fun getThemeOwner(id: CustomThemeSelector) = ThemeDataTable.select(ThemeDataTable.owner)
        .where(id.toCondition())
        .forUpdate(ForUpdateOption.ForUpdate)
        .firstOrNull()
        ?.get(ThemeDataTable.owner)
}

private sealed interface ThemeContainer {
    val theme: Theme

    data class Custom(override val theme: CustomTheme) : ThemeContainer
    data class Default(val default: DefaultTheme) : ThemeContainer {
        override val theme = default.theme
    }
}
