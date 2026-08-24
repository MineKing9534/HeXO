package de.mineking.hexo.game.model.formation

interface FormationRepository {
    suspend fun getFormation(id: FormationId): Formation?
}
