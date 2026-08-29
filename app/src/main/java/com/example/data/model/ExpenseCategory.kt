package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.intl.Locale
import com.example.ui.theme.CategoryBills
import com.example.ui.theme.CategoryEducation
import com.example.ui.theme.CategoryFood
import com.example.ui.theme.CategoryHealth
import com.example.ui.theme.CategoryHousing
import com.example.ui.theme.CategoryLeisure
import com.example.ui.theme.CategoryOthers
import com.example.ui.theme.CategoryShopping
import com.example.ui.theme.CategoryTransport

object CategoryColorProvider {
    private val dynamicPalette = listOf(
        Color(0xFF00897B), // Teal
        Color(0xFF3949AB), // Indigo
        Color(0xFFFB8C00), // Amber / Orange
        Color(0xFF8E24AA), // Purple
        Color(0xFFE53935), // Red
        Color(0xFF00ACC1), // Cyan
        Color(0xFF43A047), // Green
        Color(0xFFD81B60), // Pink
        Color(0xFF5E35B1), // Deep Purple
        Color(0xFF1E88E5), // Blue
        Color(0xFFF4511E), // Deep Orange
        Color(0xFF7CB342), // Light Green
        Color(0xFF6D4C41), // Brown
        Color(0xFF00838F), // Dark Cyan
        Color(0xFFC2185B), // Maroon / Rose
        Color(0xFF2E7D32)  // Forest Green
    )

    fun getColorForName(name: String): Color {
        if (name.isBlank()) return Color(0xFF78909C)
        val hash = kotlin.math.abs(name.trim().lowercase().hashCode())
        return dynamicPalette[hash % dynamicPalette.size]
    }
}

enum class ExpenseCategory(
    val id: String,
    val displayName: String,
    val color: Color,
    val icon: ImageVector
) {
    ALIMENTACAO("ALIMENTACAO", "Alimentação", CategoryFood, Icons.Default.Fastfood),
    TRANSPORTE("TRANSPORTE", "Transporte", CategoryTransport, Icons.Default.DirectionsCar),
    MORADIA("MORADIA", "Moradia", CategoryHousing, Icons.Default.Home),
    LAZER("LAZER", "Lazer", CategoryLeisure, Icons.Default.Movie),
    SAUDE("SAUDE", "Saúde", CategoryHealth, Icons.Default.LocalHospital),
    EDUCACAO("EDUCACAO", "Educação", CategoryEducation, Icons.Default.School),
    COMPRAS("COMPRAS", "Compras", CategoryShopping, Icons.Default.ShoppingCart),
    CONTAS("CONTAS", "Contas & Fixos", CategoryBills, Icons.Default.AccountBalance),
    OUTROS("OUTROS", "Outros", CategoryOthers, Icons.Default.Category);

    companion object {
        fun fromId(id: String?): ExpenseCategory {
            if (id.isNullOrBlank()) return OUTROS
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) || it.displayName.equals(id, ignoreCase = true) }
                ?: OUTROS
        }
    }
}
