package de.mineking.hexo.discord.bot.config

import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.board.render.image.theme.DefaultTheme
import de.mineking.hexo.board.render.image.theme.Theme
import de.mineking.hexo.database.DatabaseManager
import de.mineking.hexo.database.Transaction
import de.mineking.hexo.database.UnexpectedDatabaseErrorException
import de.mineking.hexo.database.UniqueViolationError
import de.mineking.hexo.database.mapNullableResult
import de.mineking.hexo.database.select
import de.mineking.hexo.database.throwOnDatabaseError
import de.mineking.hexo.discord.bot.config.database.ThemeDataTable
import de.mineking.hexo.discord.bot.config.database.UserThemeTable
import de.mineking.hexo.discord.core.DiscordUserId
import de.mineking.hexo.utils.types.IError
import de.mineking.hexo.utils.types.Result
import de.mineking.hexo.utils.types.mapError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.leftJoin
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
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

interface UserThemeRepository {
    suspend fun listUserThemes(user: DiscordUserId): List<CustomTheme>

    suspend fun getCurrentUserTheme(user: DiscordUserId): Theme
    suspend fun getThemeById(id: CustomThemeSelector): Result<CustomTheme, CustomThemeQueryError>

    suspend fun setCurrentUserTheme(user: DiscordUserId, theme: UserThemeSelection): Result<Theme?, CustomThemeQueryError>
    suspend fun createCustomTheme(owner: DiscordUserId, name: String, theme: Theme): Result<CustomTheme, CustomThemeCreateError>

    context(user: DiscordUserId)
    suspend fun updateCustomThemeById(id: CustomThemeSelector, theme: Theme): Result<CustomTheme, CustomThemeUpdateError>

    context(user: DiscordUserId)
    suspend fun deleteThemeById(id: CustomThemeSelector): Result<Unit, CustomThemeDeleteError>
}

class UserThemeRepositoryImpl(private val database: DatabaseManager) : UserThemeRepository {
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

    private fun UpdateBuilder<*>.bindTheme(theme: Theme) {
        val theme = if (theme is CustomTheme) theme.delegate else theme

        val base = theme.base
        this[ThemeDataTable.base] = base

        val overrides = base.theme::class.primaryConstructor!!.parameters
            .mapNotNull { param ->
                val name = param.name!!

                @Suppress("UNCHECKED_CAST")
                val property = base.theme::class.memberProperties
                    .first { it.name == name }
                    as KProperty1<Theme, Any?>

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

    override suspend fun listUserThemes(user: DiscordUserId): List<CustomTheme> {
        return database.transaction(readOnly = true) {
            ThemeDataTable.select()
                .where(ThemeDataTable.owner eq user)
                .map { it.mapToCustomTheme() }
                .execute()
                .toList()
        }.throwOnDatabaseError()
    }

    override suspend fun getCurrentUserTheme(user: DiscordUserId): Theme {
        return database.transaction(readOnly = true) {
            val result = UserThemeTable.leftJoin(ThemeDataTable, onColumn = { customTheme }, otherColumn = { id })
                .select()
                .where(UserThemeTable.id eq user)
                .execute()
                .firstOrNull() ?: return@transaction Theme.Default

            val default = result[UserThemeTable.defaultTheme]
            if (default != null) return@transaction default.theme

            result.mapToCustomTheme()
        }.throwOnDatabaseError()
    }

    override suspend fun setCurrentUserTheme(user: DiscordUserId, theme: UserThemeSelection): Result<Theme?, CustomThemeQueryError> {
        return database.transaction(readOnly = false) {
            val theme = when (theme) {
                is UserThemeSelection.Custom -> {
                    val theme = ThemeDataTable.select()
                        .where(theme.selector.toCondition())
                        .execute()
                        .firstOrNull()
                        ?.mapToCustomTheme()
                        ?: return@transaction Result.Error(CustomThemeNotFoundError)

                    ThemeContainer.Custom(theme)
                }
                is UserThemeSelection.Default -> ThemeContainer.Default(theme.theme)
            }

            UserThemeTable.upsert {
                this[UserThemeTable.id] = user

                when (theme) {
                    is ThemeContainer.Default -> {
                        this[UserThemeTable.defaultTheme] = theme.default
                        this[UserThemeTable.customTheme] = null
                    }
                    is ThemeContainer.Custom -> {
                        this[UserThemeTable.defaultTheme] = null
                        this[UserThemeTable.customTheme] = theme.theme.id
                    }
                }
            }.execute()

            Result.Success(theme.theme)
        }.throwOnDatabaseError()
    }

    override suspend fun createCustomTheme(owner: DiscordUserId, name: String, theme: Theme): Result<CustomTheme, CustomThemeCreateError> {
        return database.transaction(readOnly = false) {
            ThemeDataTable.insert {
                this[ThemeDataTable.owner] = owner
                this[ThemeDataTable.name] = name

                this.bindTheme(theme)
            }.first().mapToCustomTheme()
        }.mapError {
            when (it) {
                is UniqueViolationError if it.constraintName == "theme_data_owner_name_unique" -> CustomThemeAlreadyExists
                else -> throw UnexpectedDatabaseErrorException.Known(it)
            }
        }
    }

    context(user: DiscordUserId)
    override suspend fun updateCustomThemeById(id: CustomThemeSelector, theme: Theme): Result<CustomTheme, CustomThemeUpdateError> {
        return database.transaction(readOnly = false) {
            val owner = getThemeOwner(id)
                ?: return@transaction Result.Error(CustomThemeNotFoundError)

            if (owner != user) return@transaction Result.Error(MissingCustomThemePermissionError)

            val result = ThemeDataTable.update(where = id.toCondition()) {
                this.bindTheme(theme)
            }.first().mapToCustomTheme()

            return@transaction Result.Success(result)
        }.throwOnDatabaseError()
    }

    override suspend fun getThemeById(id: CustomThemeSelector): Result<CustomTheme, CustomThemeQueryError> {
        return database.transaction(readOnly = true) {
            ThemeDataTable.select()
                .where(id.toCondition())
                .execute()
                .firstOrNull()
                ?.mapToCustomTheme()
        }.mapNullableResult(CustomThemeNotFoundError)
    }

    context(user: DiscordUserId)
    override suspend fun deleteThemeById(id: CustomThemeSelector): Result<Unit, CustomThemeDeleteError> {
        return database.transaction(readOnly = false) {
            val owner = getThemeOwner(id)
                ?: return@transaction Result.Error(CustomThemeNotFoundError)

            if (owner != user) return@transaction Result.Error(MissingCustomThemePermissionError)

            ThemeDataTable.delete(where = id.toCondition()).execute()

            return@transaction Result.Success(Unit)
        }.throwOnDatabaseError()
    }

    context(_: Transaction)
    private suspend fun getThemeOwner(id: CustomThemeSelector) = ThemeDataTable.select(column = ThemeDataTable.owner)
        .where(id.toCondition())
        .forUpdate()
        .execute()
        .firstOrNull()
}
