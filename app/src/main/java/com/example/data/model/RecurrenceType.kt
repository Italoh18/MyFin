package com.example.data.model

enum class RecurrenceType(
    val id: String,
    val displayName: String,
    val daysInterval: Int
) {
    SEMANAL("SEMANAL", "Semanal", 7),
    QUINZENAL("QUINZENAL", "Quinzenal", 14),
    MENSAL("MENSAL", "Mensal", 30);

    companion object {
        fun fromId(id: String?): RecurrenceType {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) || it.displayName.equals(id, ignoreCase = true) }
                ?: MENSAL
        }
    }
}
