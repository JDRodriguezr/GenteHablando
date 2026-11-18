package com.example.gentehablando.domain.model

data class ReportRecord(
    val roomId: String = "",
    val timestamp: Long = 0L
)

data class UserProfile(
    val id: String = "",
    val reputationScore: Double = 0.0,
    val totalRatings: Long = 0L,
    val reportsCount: Long = 0L,
    val recentReports: List<ReportRecord> = emptyList(),
    val completedCourses: List<String> = emptyList() // Historial de cursos aprobados
) {
    /**
     * Lógica Avanzada de Moderación:
     * - Ventana de 30 días.
     * - Perdona 1 reporte por cada semana (7 días) de buen comportamiento desde el ÚLTIMO reporte.
     * - Banea si los reportes "efectivos" llegan a 3.
     */
    fun isBanned(): Boolean {
        val now = System.currentTimeMillis()
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000

        // 1. Solo miramos reportes de los últimos 30 días
        val activeReports = recentReports.filter { (now - it.timestamp) < thirtyDaysMs }
        if (activeReports.isEmpty()) return false

        // 2. Calculamos semanas de buen comportamiento
        val lastReportTime = activeReports.maxOf { it.timestamp }
        val weeksSinceLastReport = (now - lastReportTime) / sevenDaysMs

        // 3. Descontamos 1 reporte por cada semana limpia
        val effectiveReports = Math.max(0, activeReports.size - weeksSinceLastReport.toInt())

        return effectiveReports >= 3
    }

    fun getBannedRoomIds(): List<String> {
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val now = System.currentTimeMillis()
        return recentReports.filter { (now - it.timestamp) < thirtyDaysMs }.map { it.roomId }
    }
}
