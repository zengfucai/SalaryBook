
package com.salarybook.ui
import com.salarybook.ui.home.HomeScreen
import com.salarybook.ui.calendar.CalendarScreen
import com.salarybook.ui.record.RecordScreen
import com.salarybook.ui.report.ReportScreen
import com.salarybook.ui.settings.SettingsScreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

enum class AppPage(
    val title: String,
    val symbol: String
) {
    HOME("首页", "⌂"),
    CALENDAR("日历", "▦"),
    RECORD("记工时", "＋"),
    REPORT("工资报表", "▤"),
    SETTINGS("我的", "☷")
}

@Composable
fun AppNavigation() {
    var currentPage by remember {
        mutableStateOf(AppPage.HOME)
    }

    Scaffold(
        containerColor = Color(0xFFF5F7FB),
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                AppPage.entries.forEach { page ->
                    NavigationBarItem(
                        selected = currentPage == page,
                        onClick = {
                            currentPage = page
                        },
                        icon = {
                            Text(
                                text = page.symbol,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        label = {
                            Text(
                                text = page.title,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF2864B4),
                            selectedTextColor = Color(0xFF2864B4),
                            indicatorColor = Color(0xFFEAF1FC),
                            unselectedIconColor = Color(0xFF929BAA),
                            unselectedTextColor = Color(0xFF929BAA)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentPage) {
                AppPage.HOME -> HomeScreen()
                AppPage.CALENDAR -> CalendarScreen()
                AppPage.RECORD -> RecordScreen()
                AppPage.REPORT -> ReportScreen()
                AppPage.SETTINGS -> SettingsScreen()
            }
        }
    }
}
