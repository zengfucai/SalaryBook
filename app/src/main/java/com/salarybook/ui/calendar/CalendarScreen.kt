
package com.salarybook.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.salarybook.data.local.SalaryDatabase
import com.salarybook.data.SalaryRepository
import com.salarybook.data.local.WorkRecordEntity
import com.salarybook.domain.salary.WorkSegment
import com.salarybook.domain.salary.WorkSegmentJson
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current

    val repository = remember {
        SalaryRepository(SalaryDatabase.getInstance(context))
    }

    val activeProfile by repository.getActiveProfile()
        .collectAsState(initial = null)

    val records by (activeProfile?.let {
        repository.getRecordsByProfile(it.id)
    } ?: flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    var displayedMonth by remember {
        mutableStateOf(YearMonth.now())
    }

    var selectedDate by remember {
        mutableStateOf(LocalDate.now())
    }

    val monthRecords = remember(records, displayedMonth) {
        records.filter {
            it.workDate.startsWith(displayedMonth.toString())
        }
    }

    val selectedRecord = remember(records, selectedDate) {
        records.firstOrNull {
            it.workDate == selectedDate.toString()
        }
    }

    val totalPay = monthRecords.sumOf { it.totalPay }
    val workDays = monthRecords.map { it.workDate }.distinct().size

    val totalMinutes = monthRecords.sumOf {
        recordTotalMinutes(it)
    }

    val overtimeMinutes = monthRecords.sumOf {
        it.overtimeMinutes
    }

    val averagePay = if (workDays > 0) {
        totalPay / workDays
    } else {
        0.0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "工资日历",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "本月累计应得",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        money(totalPay),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MonthStatistic("工作天数", "$workDays 天")
                        MonthStatistic("累计工时", formatHours(totalMinutes))
                        MonthStatistic("加班工时", formatHours(overtimeMinutes))
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "日均工资",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            money(averagePay),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                displayedMonth = displayedMonth.minusMonths(1)
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "上个月"
                            )
                        }

                        Text(
                            displayedMonth.format(
                                DateTimeFormatter.ofPattern("yyyy年MM月")
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = {
                                displayedMonth = displayedMonth.plusMonths(1)
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "下个月"
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("日", "一", "二", "三", "四", "五", "六")
                            .forEach {
                                Text(
                                    text = it,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                    }

                    val firstDay = displayedMonth.atDay(1)
                    val offset = firstDay.dayOfWeek.value % 7
                    val daysInMonth = displayedMonth.lengthOfMonth()
                    val totalCells = ((offset + daysInMonth + 6) / 7) * 7

                    val days = (0 until totalCells).map { index ->
                        val dayNumber = index - offset + 1

                        if (dayNumber in 1..daysInMonth) {
                            displayedMonth.atDay(dayNumber)
                        } else {
                            null
                        }
                    }

                    days.chunked(7).forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            week.forEach { date ->
                                if (date == null) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(68.dp)
                                    )
                                } else {
                                    val record = monthRecords.firstOrNull {
                                        it.workDate == date.toString()
                                    }

                                    CalendarDayCell(
                                        date = date,
                                        record = record,
                                        selected = date == selectedDate,
                                        onClick = {
                                            selectedDate = date
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(50)
                                )
                        )

                        Text(
                            "有工资记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                "${selectedDate.year}年${selectedDate.monthValue}月${selectedDate.dayOfMonth}日记录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (selectedRecord == null) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "这一天还没有工作记录",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                RecordDetailCard(selectedRecord)
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    record: WorkRecordEntity?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = modifier
            .height(68.dp)
            .padding(1.dp)
            .background(
                background,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 1.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )

        if (record != null) {
            Text(
                moneyShort(record.totalPay),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Text(
                formatHours(recordTotalMinutes(record)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RecordDetailCard(record: WorkRecordEntity) {
    val segments = remember(record.workSegmentsJson) {
        WorkSegmentJson.decode(record.workSegmentsJson)
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "工作时段",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (segments.isNotEmpty()) {
                segments.forEachIndexed { index, segment ->
                    SegmentDetailRow(index, segment)
                }
            } else {
                Text(
                    "旧版记录：${record.startTimeMillis?.let(::formatTime) ?: "--:--"} - " +
                            "${record.endTimeMillis?.let(::formatTime) ?: "--:--"}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    "旧记录没有保存各段加班的具体位置，以下仅展示原有工时统计。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            Text(
                "工时统计",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            DetailRow("正常工时", formatHours(record.normalMinutes))
            DetailRow("加班工时", formatHours(record.overtimeMinutes))
            DetailRow("总工时", formatHours(recordTotalMinutes(record)), true)

            HorizontalDivider()

            Text(
                "工资明细",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            DetailRow("正常工资", money(record.normalPay))
            DetailRow("加班工资", money(record.overtimePay))
            DetailRow("计件工资", money(record.piecePay))
            DetailRow("提成", money(record.commissionPay))
            DetailRow("奖金", money(record.bonus))
            DetailRow("补贴", money(record.allowance))
            DetailRow("夜班补贴", money(record.nightAllowance))
            DetailRow("扣款", money(record.deduction))

            HorizontalDivider()

            DetailRow("当天应得工资", money(record.totalPay), true)

            if (record.notes.isNotBlank()) {
                HorizontalDivider()

                Text(
                    "备注",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(record.notes)
            }
        }
    }
}

@Composable
private fun SegmentDetailRow(
    index: Int,
    segment: WorkSegment
) {
    val overtimeText = when (segment.actualOvertimeType()) {
        "WEEKDAY" -> "平时加班"
        "REST_DAY" -> "休息日加班"
        "HOLIDAY" -> "法定节假日加班"
        else -> "正常工时"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                "时段 ${index + 1}：${segment.startTime} - ${segment.endTime}",
                fontWeight = FontWeight.Medium
            )

            Text(
                overtimeText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            formatHours(segment.durationMinutes()),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MonthStatistic(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    strong: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            value,
            fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun recordTotalMinutes(record: WorkRecordEntity): Int {
    val segments = WorkSegmentJson.decode(record.workSegmentsJson)

    return if (segments.isNotEmpty()) {
        segments.sumOf { it.durationMinutes() }
    } else {
        record.normalMinutes + record.overtimeMinutes
    }
}

private fun formatTime(millis: Long): String {
    return try {
        java.time.Instant.ofEpochMilli(millis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalTime()
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (_: Exception) {
        "--:--"
    }
}

private fun formatHours(minutes: Int): String {
    return "%.2f 小时".format(minutes / 60.0)
}

private fun money(value: Double): String {
    return "¥%.2f".format(value)
}

private fun moneyShort(value: Double): String {
    return if (value >= 10000) {
        "¥%.1f万".format(value / 10000)
    } else {
        "¥%.0f".format(value)
    }
}
