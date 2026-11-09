
package com.salarybook.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.salarybook.data.SalaryRepository
import com.salarybook.data.local.SalaryDatabase
import com.salarybook.data.local.WorkRecordEntity
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

private val HomeBackground = Color(0xFFF5F7FB)
private val HomeNavy = Color(0xFF193B68)
private val HomeGreen = Color(0xFF80E0B6)
private val HomeText = Color(0xFF202B3C)
private val HomeSecondary = Color(0xFF8792A5)

@Composable
fun HomeScreen() {

    val context = LocalContext.current

    val repository = remember {
        SalaryRepository(
            SalaryDatabase.getInstance(context)
        )
    }

    val activeProfile by repository.getActiveProfile()
        .collectAsState(initial = null)

    val records by remember(activeProfile?.id) {
        if (activeProfile == null) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            repository.getRecordsByProfile(activeProfile!!.id)
        }
    }.collectAsState(initial = emptyList())

    val currentMonth = YearMonth.now()
    val monthPrefix = currentMonth.toString()

    val monthRecords = records.filter {
        it.workDate.startsWith(monthPrefix)
    }

    val totalPay = monthRecords.sumOf { it.totalPay }
    val normalPay = monthRecords.sumOf { it.normalPay }
    val overtimePay = monthRecords.sumOf { it.overtimePay }

    val workDays = monthRecords.size

    val normalMinutes = monthRecords.sumOf {
        it.normalMinutes
    }

    val overtimeMinutes = monthRecords.sumOf {
        it.overtimeMinutes
    }

    val totalMinutes = normalMinutes + overtimeMinutes

    val averagePay = if (workDays > 0) {
        totalPay / workDays
    } else {
        0.0
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            Column {
                Text(
                    "工资工时",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = HomeText
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    activeProfile?.name?.let { "当前档案：$it" }
                        ?: "请先创建工作档案",
                    fontSize = 13.sp,
                    color = HomeSecondary
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = HomeNavy
                )
            ) {
                Column(
                    modifier = Modifier.padding(22.dp)
                ) {
                    Text(
                        "本月累计收入",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        "¥ ${money(totalPay)}",
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(20.dp))

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.2f)
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "正常工资",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                            Text(
                                "¥ ${money(normalPay)}",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            )
                        }

                        Column {
                            Text(
                                "加班工资",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                            Text(
                                "¥ ${money(overtimePay)}",
                                color = HomeGreen,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                "本月工作情况",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatisticCard(
                    title = "工作天数",
                    value = "$workDays 天",
                    modifier = Modifier.weight(1f)
                )

                StatisticCard(
                    title = "总工时",
                    value = "${formatHours(totalMinutes)} 小时",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatisticCard(
                    title = "加班时长",
                    value = "${formatHours(overtimeMinutes)} 小时",
                    modifier = Modifier.weight(1f)
                )

                StatisticCard(
                    title = "日均收入",
                    value = "¥ ${money(averagePay)}",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                "最近记录",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (records.isEmpty()) {
            item {
                EmptyRecordsCard()
            }
        } else {
            items(
                items = records.take(5),
                key = { it.id }
            ) { record ->
                RecentRecordCard(record)
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatisticCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                title,
                color = HomeSecondary,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(10.dp))

            Text(
                value,
                color = HomeText,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RecentRecordCard(
    record: WorkRecordEntity
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    record.workDate,
                    fontWeight = FontWeight.SemiBold,
                    color = HomeText
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    "正常 ${formatHours(record.normalMinutes)} 小时 · 加班 ${formatHours(record.overtimeMinutes)} 小时",
                    fontSize = 12.sp,
                    color = HomeSecondary
                )
            }

            Text(
                "¥ ${money(record.totalPay)}",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF159B70)
            )
        }
    }
}

@Composable
private fun EmptyRecordsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "暂时还没有工时记录",
                color = HomeSecondary,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "添加第一条记录，开始管理你的工资吧。",
                color = HomeSecondary,
                fontSize = 12.sp
            )
        }
    }
}

private fun money(value: Double): String {
    return String.format(Locale.CHINA, "%.2f", value)
}

private fun formatHours(minutes: Int): String {
    return String.format(
        Locale.CHINA,
        "%.1f",
        minutes / 60.0
    )
}
