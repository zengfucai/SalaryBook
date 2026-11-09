package com.salarybook.ui.record
import androidx.compose.foundation.lazy.LazyColumn
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.salarybook.data.SalaryRepository
import com.salarybook.data.local.SalaryDatabase
import com.salarybook.data.local.WorkProfileEntity
import com.salarybook.data.local.WorkRecordEntity
import com.salarybook.domain.salary.SalaryCalculationInput
import com.salarybook.domain.salary.SalaryCalculator
import com.salarybook.domain.salary.WorkSegment
import com.salarybook.domain.salary.WorkSegmentJson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

private const val OVERTIME_NONE = "NONE"
private const val OVERTIME_WEEKDAY = "WEEKDAY"
private const val OVERTIME_REST_DAY = "REST_DAY"
private const val OVERTIME_HOLIDAY = "HOLIDAY"

private const val TEMPLATE_PREFS = "work_segment_templates"

/**
 * 工作时段模板
 */
private data class WorkSegmentTemplate(
    val id: String,
    val name: String,
    val segments: List<WorkSegment>
)

/**
 * 根据日期自动识别加班类型。
 *
 * 周六、周日：休息日加班
 * 周一至周五：平时加班
 * 法定节假日：保留手动选择
 * 未勾选加班：NONE
 */
private fun classifySegmentsForDate(
    segments: List<WorkSegment>,
    date: LocalDate
): List<WorkSegment> {
    val isWeekend = date.dayOfWeek.value == 6 ||
            date.dayOfWeek.value == 7

    return segments.map { segment ->
        when {
            !segment.isOvertime -> {
                segment.copy(overtimeType = OVERTIME_NONE)
            }

            segment.overtimeType == OVERTIME_HOLIDAY -> {
                segment
            }

            isWeekend -> {
                segment.copy(overtimeType = OVERTIME_REST_DAY)
            }

            else -> {
                segment.copy(overtimeType = OVERTIME_WEEKDAY)
            }
        }
    }
}

/**
 * 模板本地存储
 *
 * 每个工作档案独立保存模板。
 * 使用 SharedPreferences，不需要修改 Room 数据库。
 */
private fun loadTemplates(
    context: Context,
    profileId: Any
): List<WorkSegmentTemplate> {
    return try {
        val preferences = context.getSharedPreferences(
            TEMPLATE_PREFS,
            Context.MODE_PRIVATE
        )

        val json = preferences.getString(
            "profile_${profileId}",
            "[]"
        ) ?: "[]"

        val array = JSONArray(json)

        buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue

                val segments = WorkSegmentJson.decode(
                    item.optString("segments", "[]")
                )

                add(
                    WorkSegmentTemplate(
                        id = item.optString("id"),
                        name = item.optString("name"),
                        segments = segments
                    )
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveTemplates(
    context: Context,
    profileId: Any,
    templates: List<WorkSegmentTemplate>
) {
    val array = JSONArray()

    templates.forEach { template ->
        array.put(
            JSONObject().apply {
                put("id", template.id)
                put("name", template.name)
                put(
                    "segments",
                    WorkSegmentJson.encode(template.segments)
                )
            }
        )
    }

    context.getSharedPreferences(
        TEMPLATE_PREFS,
        Context.MODE_PRIVATE
    ).edit()
        .putString("profile_${profileId}", array.toString())
        .apply()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val repository = remember {
        SalaryRepository(SalaryDatabase.getInstance(context))
    }

    val profiles by repository.getAllProfiles()
        .collectAsState(initial = emptyList())

    val activeProfile by repository.getActiveProfile()
        .collectAsState(initial = null)

    var selectedProfile by remember {
        mutableStateOf<WorkProfileEntity?>(null)
    }

    var profileExpanded by remember {
        mutableStateOf(false)
    }

    var workDate by remember {
        mutableStateOf(LocalDate.now())
    }

    var segments by remember {
        mutableStateOf(listOf<WorkSegment>())
    }

    var templates by remember {
        mutableStateOf(listOf<WorkSegmentTemplate>())
    }

    var showTemplateDialog by remember {
        mutableStateOf(false)
    }

    var pieceCount by remember { mutableStateOf("") }
    var commissionBase by remember { mutableStateOf("") }
    var commissionPay by remember { mutableStateOf("") }
    var bonus by remember { mutableStateOf("") }
    var allowance by remember { mutableStateOf("") }
    var nightHours by remember { mutableStateOf("") }
    var nightAllowance by remember { mutableStateOf("") }
    var deduction by remember { mutableStateOf("") }
    var paidUnits by remember { mutableStateOf("1") }
    var notes by remember { mutableStateOf("") }

    var customIncome by remember {
        mutableStateOf(mapOf<String, String>())
    }

    var customDeduction by remember {
        mutableStateOf(mapOf<String, String>())
    }

    var existingRecord by remember {
        mutableStateOf<WorkRecordEntity?>(null)
    }

    var statusMessage by remember {
        mutableStateOf("")
    }

    var isSaving by remember {
        mutableStateOf(false)
    }

    val profile = selectedProfile ?: activeProfile

    LaunchedEffect(activeProfile) {
        if (selectedProfile == null) {
            selectedProfile = activeProfile
        }
    }

    // 切换工作档案时，加载对应档案的模板
    LaunchedEffect(profile?.id) {
        templates = profile?.id?.let {
            loadTemplates(context, it)
        } ?: emptyList()
    }

    // 切换日期或工作档案时，加载当天记录
    LaunchedEffect(profile?.id, workDate) {
        val currentProfile = profile ?: return@LaunchedEffect

        val record = repository.getRecordByDate(
            currentProfile.id,
            workDate.toString()
        )

        existingRecord = record

        if (record == null) {
            val previousRecord = repository
                .getRecordsByProfile(currentProfile.id)
                .first()
                .filter {
                    it.workDate < workDate.toString()
                }
                .maxByOrNull {
                    it.workDate
                }

            val previousSegments = previousRecord?.let {
                WorkSegmentJson.decode(it.workSegmentsJson)
            }.orEmpty()

            val loadedSegments = when {
                previousSegments.isNotEmpty() -> {
                    previousSegments.map {
                        it.copy(id = UUID.randomUUID().toString())
                    }
                }

                previousRecord != null -> {
                    val start = previousRecord.startTimeMillis?.let {
                        java.time.Instant.ofEpochMilli(it)
                            .atZone(ZoneId.systemDefault())
                            .toLocalTime()
                            .format(
                                DateTimeFormatter.ofPattern("HH:mm")
                            )
                    } ?: "09:00"

                    val end = previousRecord.endTimeMillis?.let {
                        java.time.Instant.ofEpochMilli(it)
                            .atZone(ZoneId.systemDefault())
                            .toLocalTime()
                            .format(
                                DateTimeFormatter.ofPattern("HH:mm")
                            )
                    } ?: "18:00"

                    listOf(
                        WorkSegment(
                            id = UUID.randomUUID().toString(),
                            startTime = start,
                            endTime = end
                        )
                    )
                }

                else -> {
                    listOf(
                        WorkSegment(
                            id = UUID.randomUUID().toString(),
                            startTime = "09:00",
                            endTime = "18:00"
                        )
                    )
                }
            }

            // 新日期自动识别周末加班类型
            segments = classifySegmentsForDate(
                loadedSegments,
                workDate
            )

            pieceCount = ""
            commissionBase = ""
            commissionPay = ""
            bonus = ""
            allowance = ""
            nightHours = ""
            nightAllowance = ""
            deduction = ""
            paidUnits = "1"
            notes = ""
            customIncome = emptyMap()
            customDeduction = emptyMap()

        } else {
            val decoded = WorkSegmentJson.decode(
                record.workSegmentsJson
            )

            val loadedSegments = if (decoded.isNotEmpty()) {
                decoded
            } else {
                val start = record.startTimeMillis?.let {
                    java.time.Instant.ofEpochMilli(it)
                        .atZone(ZoneId.systemDefault())
                        .toLocalTime()
                        .format(
                            DateTimeFormatter.ofPattern("HH:mm")
                        )
                } ?: "09:00"

                val end = record.endTimeMillis?.let {
                    java.time.Instant.ofEpochMilli(it)
                        .atZone(ZoneId.systemDefault())
                        .toLocalTime()
                        .format(
                            DateTimeFormatter.ofPattern("HH:mm")
                        )
                } ?: "18:00"

                listOf(
                    WorkSegment(
                        id = UUID.randomUUID().toString(),
                        startTime = start,
                        endTime = end
                    )
                )
            }

            segments = classifySegmentsForDate(
                loadedSegments,
                workDate
            )

            pieceCount = record.pieceCount.toString()
            commissionBase = record.commissionBase.toString()
            commissionPay = record.commissionPay.toString()
            bonus = record.bonus.toString()
            allowance = record.allowance.toString()
            nightHours = record.nightHours.toString()
            nightAllowance = record.nightAllowance.toString()
            deduction = record.deduction.toString()
            paidUnits = record.paidUnits.toString()
            notes = record.notes
            customIncome = decodeValues(record.customIncomeValues)
            customDeduction = decodeValues(record.customDeductionValues)
        }
    }

    val normalMinutes = segments
        .filter { !it.isOvertime }
        .sumOf { it.durationMinutes() }

    val weekdayOvertimeMinutes = segments
        .filter {
            it.isOvertime &&
                    it.actualOvertimeType() == OVERTIME_WEEKDAY
        }
        .sumOf { it.durationMinutes() }

    val restDayOvertimeMinutes = segments
        .filter {
            it.isOvertime &&
                    it.actualOvertimeType() == OVERTIME_REST_DAY
        }
        .sumOf { it.durationMinutes() }

    val holidayOvertimeMinutes = segments
        .filter {
            it.isOvertime &&
                    it.actualOvertimeType() == OVERTIME_HOLIDAY
        }
        .sumOf { it.durationMinutes() }

    val totalMinutes = normalMinutes +
            weekdayOvertimeMinutes +
            restDayOvertimeMinutes +
            holidayOvertimeMinutes

    val input = remember(
        profile,
        normalMinutes,
        weekdayOvertimeMinutes,
        restDayOvertimeMinutes,
        holidayOvertimeMinutes,
        pieceCount,
        commissionBase,
        commissionPay,
        bonus,
        allowance,
        nightHours,
        nightAllowance,
        deduction,
        paidUnits,
        customIncome,
        customDeduction
    ) {
        val p = profile

        SalaryCalculationInput(
            salaryType = p?.salaryType ?: "HOURLY",
            monthlySalary = p?.monthlySalary ?: 0.0,
            hourlyRate = p?.hourlyRate ?: 0.0,
            dailyRate = p?.dailyRate ?: 0.0,
            weeklyRate = p?.weeklyRate ?: 0.0,
            monthlyAllocationDays = p?.monthlyAllocationDays ?: 0.0,
            weeklyAllocationDays = p?.weeklyAllocationDays ?: 0.0,
            paidUnits = paidUnits.toDoubleOrNull() ?: 1.0,
            normalMinutes = normalMinutes,
            overtimeMinutes = 0,
            overtimeMultiplier = p?.weekdayOvertimeMultiplier ?: 1.5,
            weekdayOvertimeMinutes = weekdayOvertimeMinutes,
            restDayOvertimeMinutes = restDayOvertimeMinutes,
            holidayOvertimeMinutes = holidayOvertimeMinutes,
            weekdayOvertimeMultiplier = p?.weekdayOvertimeMultiplier ?: 1.5,
            restDayOvertimeMultiplier = p?.restDayOvertimeMultiplier ?: 2.0,
            holidayOvertimeMultiplier = p?.holidayOvertimeMultiplier ?: 3.0,
            standardHoursPerDay = p?.standardHoursPerDay ?: 8.0,
            pieceCount = pieceCount.toDoubleOrNull() ?: 0.0,
            pieceRate = p?.pieceRate ?: 0.0,
            commissionBase = commissionBase.toDoubleOrNull() ?: 0.0,
            commissionRate = p?.commissionRate ?: 0.0,
            commissionPay = commissionPay.toDoubleOrNull() ?: 0.0,
            bonus = bonus.toDoubleOrNull() ?: 0.0,
            allowance = allowance.toDoubleOrNull() ?: 0.0,
            nightHours = nightHours.toDoubleOrNull() ?: 0.0,
            nightAllowancePerHour = p?.nightAllowancePerHour ?: 0.0,
            nightAllowance = nightAllowance.toDoubleOrNull() ?: 0.0,
            deduction = deduction.toDoubleOrNull() ?: 0.0,
            customIncome = customIncome.values.sumOf {
                it.toDoubleOrNull() ?: 0.0
            },
            customDeduction = customDeduction.values.sumOf {
                it.toDoubleOrNull() ?: 0.0
            }
        )
    }

    val calculation = remember(input) {
        SalaryCalculator.calculate(input)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "记录工时",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        // 工作档案和日期
        item {
            Card {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "工作档案",
                        style = MaterialTheme.typography.titleMedium
                    )

                    ExposedDropdownMenuBox(
                        expanded = profileExpanded,
                        onExpandedChange = {
                            profileExpanded = it
                        }
                    ) {
                        OutlinedTextField(
                            value = profile?.name ?: "暂无工作档案",
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text("选择工作档案")
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    profileExpanded
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = profileExpanded,
                            onDismissRequest = {
                                profileExpanded = false
                            }
                        ) {
                            profiles.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Text(item.name)
                                    },
                                    onClick = {
                                        selectedProfile = item
                                        profileExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    workDate = LocalDate.of(
                                        year,
                                        month + 1,
                                        day
                                    )
                                },
                                workDate.year,
                                workDate.monthValue - 1,
                                workDate.dayOfMonth
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("工作日期：$workDate")
                    }
                }
            }
        }

        // 工作时段和模板管理
        item {
            Card {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "工作时段",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(Modifier.weight(1f))

                        TextButton(
                            enabled = profile != null,
                            onClick = {
                                showTemplateDialog = true
                            }
                        ) {
                            Text("模板管理")
                        }

                        TextButton(
                            onClick = {
                                segments = segments + WorkSegment(
                                    id = UUID.randomUUID().toString(),
                                    startTime = "09:00",
                                    endTime = "18:00"
                                )
                            }
                        ) {
                            Icon(Icons.Default.Add, null)
                            Text("添加时段")
                        }
                    }

                    Text(
                        "周六、周日已勾选的加班时段会自动识别为休息日加班；未勾选的时段仍按正常工时计算。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        "结束时间早于开始时间时，按次日结束计算。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (segments.isEmpty()) {
                        Text("尚未添加工作时段")
                    }

                    segments.forEachIndexed { index, segment ->
                        WorkSegmentEditor(
                            index = index,
                            segment = segment,
                            workDate = workDate,
                            onChange = { changed ->
                                segments = segments.toMutableList().also {
                                    it[index] = changed
                                }
                            },
                            onDelete = {
                                segments = segments.filterIndexed { i, _ ->
                                    i != index
                                }
                            }
                        )
                    }

                    HorizontalDivider()

                    SummaryLine(
                        "正常工时",
                        formatHours(normalMinutes)
                    )

                    SummaryLine(
                        "平时加班",
                        formatHours(weekdayOvertimeMinutes)
                    )

                    SummaryLine(
                        "休息日加班",
                        formatHours(restDayOvertimeMinutes)
                    )

                    SummaryLine(
                        "法定节假日加班",
                        formatHours(holidayOvertimeMinutes)
                    )

                    SummaryLine(
                        "总工时",
                        formatHours(totalMinutes),
                        true
                    )

                    if (
                        totalMinutes > 0 &&
                        totalMinutes >
                        ((profile?.standardHoursPerDay ?: 8.0) * 60).toInt()
                    ) {
                        Text(
                            "当天工时超过档案标准工时。是否加班仍以你勾选的时段为准。",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // 工资项目
        item {
            Card {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "工资项目",
                        style = MaterialTheme.typography.titleMedium
                    )

                    NumberField("计件数量", pieceCount) {
                        pieceCount = it
                    }

                    NumberField("提成基数", commissionBase) {
                        commissionBase = it
                    }

                    NumberField("手动提成", commissionPay) {
                        commissionPay = it
                    }

                    NumberField("奖金", bonus) {
                        bonus = it
                    }

                    NumberField("补贴", allowance) {
                        allowance = it
                    }

                    NumberField("夜班时长", nightHours) {
                        nightHours = it
                    }

                    NumberField("夜班补贴", nightAllowance) {
                        nightAllowance = it
                    }

                    NumberField("扣款", deduction) {
                        deduction = it
                    }

                    NumberField("计薪单位", paidUnits) {
                        paidUnits = it
                    }
                }
            }
        }

        // 自定义收入
        item {
            Card {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "自定义收入",
                        style = MaterialTheme.typography.titleMedium
                    )

                    profile?.customIncomeNames
                        ?.split("\n")
                        ?.map { it.trim() }
                        ?.filter { it.isNotBlank() }
                        ?.distinct()
                        ?.forEach { name ->
                            NumberField(
                                name,
                                customIncome[name] ?: ""
                            ) {
                                customIncome = customIncome + (name to it)
                            }
                        }

                    HorizontalDivider()

                    Text(
                        "自定义扣款",
                        style = MaterialTheme.typography.titleMedium
                    )

                    profile?.customDeductionNames
                        ?.split("\n")
                        ?.map { it.trim() }
                        ?.filter { it.isNotBlank() }
                        ?.distinct()
                        ?.forEach { name ->
                            NumberField(
                                name,
                                customDeduction[name] ?: ""
                            ) {
                                customDeduction = customDeduction + (name to it)
                            }
                        }
                }
            }
        }

        // 工资预览
        item {
            Card {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "工资预览",
                        style = MaterialTheme.typography.titleMedium
                    )

                    SummaryLine(
                        "正常工资",
                        money(calculation.normalPay)
                    )

                    SummaryLine(
                        "加班工资",
                        money(calculation.overtimePay)
                    )

                    SummaryLine(
                        "其他收入",
                        money(calculation.extraPay)
                    )

                    SummaryLine(
                        "扣款",
                        money(calculation.deduction)
                    )

                    HorizontalDivider()

                    SummaryLine(
                        "预计应得工资",
                        money(calculation.netPay),
                        true
                    )
                }
            }
        }

        // 备注
        item {
            OutlinedTextField(
                value = notes,
                onValueChange = {
                    notes = it
                },
                label = {
                    Text("备注")
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }

        // 保存记录
        item {
            Button(
                onClick = {
                    val currentProfile = profile

                    if (currentProfile == null) {
                        statusMessage = "请先创建工作档案"
                        return@Button
                    }

                    if (segments.any {
                            it.durationMinutes() <= 0
                        }
                    ) {
                        statusMessage = "请检查工作时段的开始和结束时间"
                        return@Button
                    }

                    scope.launch {
                        isSaving = true

                        try {
                            // 保存前再次根据日期自动识别加班类型
                            val finalSegments = classifySegmentsForDate(
                                segments,
                                workDate
                            )

                            segments = finalSegments

                            val finalNormalMinutes = finalSegments
                                .filter { !it.isOvertime }
                                .sumOf { it.durationMinutes() }

                            val finalWeekdayMinutes = finalSegments
                                .filter {
                                    it.isOvertime &&
                                            it.actualOvertimeType() == OVERTIME_WEEKDAY
                                }
                                .sumOf { it.durationMinutes() }

                            val finalRestDayMinutes = finalSegments
                                .filter {
                                    it.isOvertime &&
                                            it.actualOvertimeType() == OVERTIME_REST_DAY
                                }
                                .sumOf { it.durationMinutes() }

                            val finalHolidayMinutes = finalSegments
                                .filter {
                                    it.isOvertime &&
                                            it.actualOvertimeType() == OVERTIME_HOLIDAY
                                }
                                .sumOf { it.durationMinutes() }

                            val finalInput = input.copy(
                                normalMinutes = finalNormalMinutes,
                                weekdayOvertimeMinutes = finalWeekdayMinutes,
                                restDayOvertimeMinutes = finalRestDayMinutes,
                                holidayOvertimeMinutes = finalHolidayMinutes
                            )

                            val finalCalculation =
                                SalaryCalculator.calculate(finalInput)

                            val startMillis = finalSegments
                                .firstOrNull()
                                ?.let {
                                    timeToMillis(
                                        workDate,
                                        it.startTime
                                    )
                                }

                            val endMillis = finalSegments
                                .lastOrNull()
                                ?.let {
                                    timeToMillis(
                                        workDate,
                                        it.endTime,
                                        it.startTime
                                    )
                                }

                            val overtimeTypes = finalSegments
                                .filter { it.isOvertime }
                                .map { it.actualOvertimeType() }
                                .distinct()

                            val overtimeType = when {
                                overtimeTypes.isEmpty() -> OVERTIME_NONE
                                overtimeTypes.size == 1 -> overtimeTypes.first()
                                else -> "MIXED"
                            }

                            val old = existingRecord

                            val record = WorkRecordEntity(
                                id = old?.id ?: 0L,
                                profileId = currentProfile.id,
                                workDate = workDate.toString(),
                                startTimeMillis = startMillis,
                                endTimeMillis = endMillis,
                                breakMinutes = 0,
                                normalMinutes = finalNormalMinutes,
                                overtimeMinutes = finalWeekdayMinutes +
                                        finalRestDayMinutes +
                                        finalHolidayMinutes,
                                overtimeType = overtimeType,
                                pieceCount = pieceCount.toDoubleOrNull() ?: 0.0,
                                piecePay = finalCalculation.piecePay,
                                commissionBase = commissionBase.toDoubleOrNull() ?: 0.0,
                                commissionPay = finalCalculation.commissionPay,
                                bonus = bonus.toDoubleOrNull() ?: 0.0,
                                allowance = allowance.toDoubleOrNull() ?: 0.0,
                                nightHours = nightHours.toDoubleOrNull() ?: 0.0,
                                nightAllowance = nightAllowance.toDoubleOrNull() ?: 0.0,
                                deduction = deduction.toDoubleOrNull() ?: 0.0,
                                paidUnits = paidUnits.toDoubleOrNull() ?: 1.0,
                                customIncomeValues = encodeValues(customIncome),
                                customDeductionValues = encodeValues(customDeduction),
                                notes = notes,
                                snapshotHourlyRate = currentProfile.hourlyRate,
                                snapshotOvertimeMultiplier = currentProfile.weekdayOvertimeMultiplier,
                                snapshotPieceRate = currentProfile.pieceRate,
                                salaryRuleSnapshot = profileSnapshot(currentProfile),
                                calculationDetails = "",
                                normalPay = finalCalculation.normalPay,
                                overtimePay = finalCalculation.overtimePay,
                                totalPay = finalCalculation.netPay,
                                workSegmentsJson = WorkSegmentJson.encode(finalSegments),
                                updatedAt = System.currentTimeMillis()
                            )

                            if (old == null) {
                                repository.saveRecord(record)
                            } else {
                                repository.updateRecord(record)
                            }

                            existingRecord = record
                            statusMessage = "保存成功"

                        } catch (e: Exception) {
                            statusMessage = "保存失败：${e.message}"
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving && profile != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (isSaving) "保存中..." else "保存记录"
                )
            }

            if (statusMessage.isNotBlank()) {
                Text(
                    statusMessage,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // 模板管理弹窗
    if (showTemplateDialog && profile != null) {
        WorkSegmentTemplateDialog(
            templates = templates,
            currentSegments = segments,
            onDismiss = {
                showTemplateDialog = false
            },
            onSaveTemplate = { name ->
                val newTemplate = WorkSegmentTemplate(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    segments = segments.map {
                        it.copy(id = UUID.randomUUID().toString())
                    }
                )

                templates = templates + newTemplate

                saveTemplates(
                    context,
                    profile.id,
                    templates
                )
            },
            onApplyTemplate = { template ->
                segments = classifySegmentsForDate(
                    template.segments.map {
                        it.copy(id = UUID.randomUUID().toString())
                    },
                    workDate
                )

                showTemplateDialog = false
                statusMessage = "已应用模板：${template.name}"
            },
            onOverwriteTemplate = { template ->
                templates = templates.map {
                    if (it.id == template.id) {
                        it.copy(
                            segments = segments.map { segment ->
                                segment.copy(
                                    id = UUID.randomUUID().toString()
                                )
                            }
                        )
                    } else {
                        it
                    }
                }

                saveTemplates(
                    context,
                    profile.id,
                    templates
                )

                statusMessage = "模板已更新：${template.name}"
            },
            onDeleteTemplate = { template ->
                templates = templates.filter {
                    it.id != template.id
                }

                saveTemplates(
                    context,
                    profile.id,
                    templates
                )
            }
        )
    }
}

/**
 * 工作时段模板管理弹窗
 */
@Composable
private fun WorkSegmentTemplateDialog(
    templates: List<WorkSegmentTemplate>,
    currentSegments: List<WorkSegment>,
    onDismiss: () -> Unit,
    onSaveTemplate: (String) -> Unit,
    onApplyTemplate: (WorkSegmentTemplate) -> Unit,
    onOverwriteTemplate: (WorkSegmentTemplate) -> Unit,
    onDeleteTemplate: (WorkSegmentTemplate) -> Unit
) {
    var templateName by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("工作时段模板管理")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "将当前工作时段保存为模板，下次记录时可以直接应用。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = templateName,
                    onValueChange = {
                        templateName = it
                    },
                    label = {
                        Text("新模板名称")
                    },
                    placeholder = {
                        Text("例如：正常白班、夜班、两班倒")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        onSaveTemplate(templateName.trim())
                        templateName = ""
                    },
                    enabled = templateName.isNotBlank() &&
                            currentSegments.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("保存当前时段为模板")
                }

                HorizontalDivider()

                Text(
                    "已保存的模板（${templates.size}）",
                    style = MaterialTheme.typography.titleSmall
                )

                if (templates.isEmpty()) {
                    Text(
                        "暂无模板，请先保存当前工作时段。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                templates.forEach { template ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                template.name,
                                style = MaterialTheme.typography.titleSmall
                            )

                            Text(
                                template.segments.joinToString("、") {
                                    "${it.startTime}-${it.endTime}" +
                                            if (it.isOvertime) "（加班）" else ""
                                },
                                style = MaterialTheme.typography.bodySmall
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        onOverwriteTemplate(template)
                                    }
                                ) {
                                    Text("覆盖")
                                }

                                TextButton(
                                    onClick = {
                                        onDeleteTemplate(template)
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null
                                    )
                                    Text("删除")
                                }

                                Button(
                                    onClick = {
                                        onApplyTemplate(template)
                                    }
                                ) {
                                    Text("应用")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成")
            }
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkSegmentEditor(
    index: Int,
    segment: WorkSegment,
    workDate: LocalDate,
    onChange: (WorkSegment) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "时段 ${index + 1}",
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(Modifier.weight(1f))

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除时段"
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val time = LocalTime.parse(segment.startTime)

                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                onChange(
                                    segment.copy(
                                        startTime = "%02d:%02d".format(
                                            hour,
                                            minute
                                        )
                                    )
                                )
                            },
                            time.hour,
                            time.minute,
                            true
                        ).show()
                    }
                ) {
                    Text("开始 ${segment.startTime}")
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val time = LocalTime.parse(segment.endTime)

                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                onChange(
                                    segment.copy(
                                        endTime = "%02d:%02d".format(
                                            hour,
                                            minute
                                        )
                                    )
                                )
                            },
                            time.hour,
                            time.minute,
                            true
                        ).show()
                    }
                ) {
                    Text("结束 ${segment.endTime}")
                }
            }

            Text(
                "时长：${formatHours(segment.durationMinutes())}"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = segment.isOvertime,
                    onCheckedChange = { checked ->
                        val isWeekend =
                            workDate.dayOfWeek.value == 6 ||
                                    workDate.dayOfWeek.value == 7

                        val defaultType = if (isWeekend) {
                            OVERTIME_REST_DAY
                        } else {
                            OVERTIME_WEEKDAY
                        }

                        onChange(
                            segment.copy(
                                isOvertime = checked,
                                overtimeType = if (checked) {
                                    defaultType
                                } else {
                                    OVERTIME_NONE
                                }
                            )
                        )
                    }
                )

                Text("此时段计为加班")
            }

            if (segment.isOvertime) {
                var expanded by remember(segment.id) {
                    mutableStateOf(false)
                }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = {
                        expanded = it
                    }
                ) {
                    OutlinedTextField(
                        value = overtimeTypeName(
                            segment.actualOvertimeType()
                        ),
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text("加班类型")
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = {
                            expanded = false
                        }
                    ) {
                        listOf(
                            OVERTIME_WEEKDAY to "平时加班",
                            OVERTIME_REST_DAY to "休息日加班",
                            OVERTIME_HOLIDAY to "法定节假日加班"
                        ).forEach { (value, label) ->
                            DropdownMenuItem(
                                text = {
                                    Text(label)
                                },
                                onClick = {
                                    onChange(
                                        segment.copy(
                                            overtimeType = value
                                        )
                                    )

                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (
                input.isEmpty() ||
                input.matches(Regex("^\\d*\\.?\\d*$"))
            ) {
                onValueChange(input)
            }
        },
        label = {
            Text(label)
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal
        ),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SummaryLine(
    label: String,
    value: String,
    strong: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)

        Text(
            value,
            style = if (strong) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyMedium
            }
        )
    }
}

private fun formatHours(minutes: Int): String {
    return "%.2f 小时".format(minutes / 60.0)
}

private fun money(value: Double): String {
    return "¥%.2f".format(value)
}

private fun overtimeTypeName(type: String): String {
    return when (type) {
        OVERTIME_REST_DAY -> "休息日加班"
        OVERTIME_HOLIDAY -> "法定节假日加班"
        else -> "平时加班"
    }
}

private fun timeToMillis(
    date: LocalDate,
    time: String,
    startTime: String? = null
): Long {
    val parsedTime = LocalTime.parse(time)
    var dateTime = date.atTime(parsedTime)

    if (
        startTime != null &&
        parsedTime <= LocalTime.parse(startTime)
    ) {
        dateTime = dateTime.plusDays(1)
    }

    return dateTime
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

private fun encodeValues(
    values: Map<String, String>
): String {
    return JSONObject(
        values.filterValues {
            it.isNotBlank()
        }.mapValues {
            it.value.toDoubleOrNull() ?: 0.0
        }
    ).toString()
}

private fun decodeValues(
    json: String
): Map<String, String> {
    return try {
        val obj = JSONObject(json)

        buildMap {
            val keys = obj.keys()

            while (keys.hasNext()) {
                val key = keys.next()
                put(key, obj.optDouble(key).toString())
            }
        }
    } catch (_: Exception) {
        emptyMap()
    }
}

private fun profileSnapshot(
    profile: WorkProfileEntity
): String {
    return JSONObject().apply {
        put("salaryType", profile.salaryType)
        put("monthlySalary", profile.monthlySalary)
        put("hourlyRate", profile.hourlyRate)
        put("dailyRate", profile.dailyRate)
        put("weeklyRate", profile.weeklyRate)
        put("pieceRate", profile.pieceRate)
        put("commissionRate", profile.commissionRate)
        put("monthlyAllocationDays", profile.monthlyAllocationDays)
        put("weeklyAllocationDays", profile.weeklyAllocationDays)
        put("standardHoursPerDay", profile.standardHoursPerDay)
        put(
            "weekdayOvertimeMultiplier",
            profile.weekdayOvertimeMultiplier
        )
        put(
            "restDayOvertimeMultiplier",
            profile.restDayOvertimeMultiplier
        )
        put(
            "holidayOvertimeMultiplier",
            profile.holidayOvertimeMultiplier
        )
        put("nightAllowancePerHour", profile.nightAllowancePerHour)
    }.toString()
}