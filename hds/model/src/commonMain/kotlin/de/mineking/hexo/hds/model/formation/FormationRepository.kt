package de.mineking.hexo.hds.model.formation

interface FormationRepository {
    suspend fun getFormation(id: FormationId): Formation?
}
