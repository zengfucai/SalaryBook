
package com.salarybook.ui.report

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salarybook.data.SalaryRepository
import com.salarybook.data.local.SalaryDatabase
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.util.Locale

private val ReportBackground = Color(0xFFF5F7FB)
private val ReportText = Color(0xFF26364A)
private val ReportSecondary = Color(0xFF8792A5)
private val ReportGreen = Color(0xFF159B70)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen() {
    val context = LocalContext.current

    val repository = remember {
        SalaryRepository(SalaryDatabase.getInstance(context))
    }

    val profiles by repository.getAllProfiles()
        .collectAsState(initial = emptyList())

    val activeProfile = profiles.firstOrNull { it.isActive }
        ?: profiles.firstOrNull()

    val recordsFlow = remember(activeProfile?.id) {
        activeProfile?.let {
            repository.getRecordsByProfile(it.id)
        } ?: flowOf(emptyList())
    }

    val records by recordsFlow.collectAsState(initial = emptyList())

    var period by remember { mutableStateOf("月度") }

    val today = LocalDate.now()

    val startDate: LocalDate
    val endDate: LocalDate

    when (period) {
        "季度" -> {
            val startMonth = ((today.monthValue - 1) / 3) * 3 + 1
            startDate = LocalDate.of(today.year, startMonth, 1)
            endDate = startDate.plusMonths(3).minusDays(1)
        }

        "年度" -> {
            startDate = LocalDate.of(today.year, 1, 1)
            endDate = LocalDate.of(today.year, 12, 31)
        }

        else -> {
            startDate = today.withDayOfMonth(1)
            endDate = today.withDayOfMonth(today.lengthOfMonth())
        }
    }

    val periodRecords = remember(records, startDate, endDate) {
        records.filter { record ->
            val date = runCatching {
                LocalDate.parse(record.workDate)
            }.getOrNull()

            date != null && !date.isBefore(startDate) && !date.isAfter(endDate)
        }
    }

    val normalPay = periodRecords.sumOf { it.normalPay }
    val overtimePay = periodRecords.sumOf { it.overtimePay }
    val pieceAndCommission = periodRecords.sumOf {
        it.piecePay + it.commissionPay
    }
    val bonusAndAllowance = periodRecords.sumOf {
        it.bonus + it.allowance + it.nightAllowance
    }
    val deduction = periodRecords.sumOf { it.deduction }
    val totalPay = periodRecords.sumOf { it.totalPay }

    val normalMinutes = periodRecords.sumOf { it.normalMinutes }
    val overtimeMinutes = periodRecords.sumOf { it.overtimeMinutes }
    val workDays = periodRecords.map { it.workDate }.distinct().size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ReportBackground)
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Text(
            text = "工资报表",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ReportText
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = activeProfile?.name ?: "暂无工作档案",
            fontSize = 13.sp,
            color = ReportSecondary
        )

        Spacer(Modifier.height(18.dp))

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("月度", "季度", "年度").forEachIndexed { index, value ->
                SegmentedButton(
                    selected = period == value,
                    onClick = { period = value },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = 3
                    )
                ) {
                    Text(value)
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF193B68)
            )
        ) {
            Column(Modifier.padding(22.dp)) {
                Text(
                    text = "统计期间实际收入",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "¥ ${money(totalPay)}",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "${startDate} 至 ${endDate}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = "工资构成",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = ReportText
        )

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(17.dp)
            ) {
                ReportItem("基本工资", "¥ ${money(normalPay)}")
                ReportItem("加班工资", "¥ ${money(overtimePay)}")
                ReportItem("计件及提成", "¥ ${money(pieceAndCommission)}")
                ReportItem("奖金及补贴", "¥ ${money(bonusAndAllowance)}")
                ReportItem("扣款", "-¥ ${money(deduction)}")

                HorizontalDivider(color = Color(0xFFECEFF3))

                ReportItem(
                    "最终合计",
                    "¥ ${money(totalPay)}",
                    true
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = "工时统计",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = ReportText
        )

        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReportStat(
                "正常工时",
                "${hours(normalMinutes)} 小时",
                Modifier.weight(1f)
            )

            ReportStat(
                "加班工时",
                "${hours(overtimeMinutes)} 小时",
                Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        ReportStat(
            "工作天数",
            "$workDays 天",
            Modifier.fillMaxWidth()
        )

        if (periodRecords.isEmpty()) {
            Spacer(Modifier.height(20.dp))

            Text(
                text = "当前统计期间还没有工时记录",
                color = ReportSecondary,
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(18.dp))

        OutlinedButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("报表导出功能暂未开放")
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ReportItem(
    title: String,
    amount: String,
    important: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = Color(0xFF687589),
            fontSize = 14.sp
        )

        Text(
            text = amount,
            color = if (important) ReportGreen else ReportText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ReportStat(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = ReportSecondary
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = ReportText
            )
        }
    }
}

private fun money(value: Double): String {
    return String.format(Locale.CHINA, "%,.2f", value)
}

private fun hours(minutes: Int): String {
    return String.format(Locale.CHINA, "%.1f", minutes / 60.0)
}
