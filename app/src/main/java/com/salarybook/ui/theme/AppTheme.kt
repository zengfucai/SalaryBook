
package com.salarybook.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF193B68)
val PrimaryBlue = Color(0xFF2864B4)
val IncomeGreen = Color(0xFF159B70)
val ExpenseRed = Color(0xFFE45B60)
val PageBackground = Color(0xFFF5F7FB)
val CardWhite = Color(0xFFFFFFFF)
val SecondaryText = Color(0xFF8792A5)

private val SalaryColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    secondary = IncomeGreen,
    background = PageBackground,
    surface = CardWhite,
    onSurface = Color(0xFF202B3C),
    onBackground = Color(0xFF202B3C),
    error = ExpenseRed
)

@Composable
fun SalaryBookTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SalaryColorScheme,
        content = content
    )
}
