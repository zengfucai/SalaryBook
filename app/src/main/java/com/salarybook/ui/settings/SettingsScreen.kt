
package com.salarybook.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.salarybook.data.SalaryRepository
import com.salarybook.data.local.SalaryDatabase
import com.salarybook.data.local.WorkProfileEntity
import kotlinx.coroutines.launch

private val SettingsBackground = Color(0xFFF5F7FB)
private val SettingsSecondary = Color(0xFF8792A5)
private val SettingsGreen = Color(0xFF159B70)

@Composable
fun SettingsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    val repository = remember {
        SalaryRepository(SalaryDatabase.getInstance(context))
    }

    val profiles by repository.getAllProfiles()
        .collectAsState(initial = emptyList())

    val activeProfile by repository.getActiveProfile()
        .collectAsState(initial = null)

    var editorVisible by remember { mutableStateOf(false) }
    var editingProfile by remember {
        mutableStateOf<WorkProfileEntity?>(null)
    }
    var deletingProfile by remember {
        mutableStateOf<WorkProfileEntity?>(null)
    }

    var darkMode by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsBackground)
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Text(
            "我的",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(18.dp))

        SettingsCard {
            Text(
                "工资工时",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(5.dp))

            Text(
                "个人工资与工时管理工具",
                color = SettingsSecondary,
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "工作档案",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                TextButton(
                    onClick = {
                        editingProfile = null
                        editorVisible = true
                    }
                ) {
                    Text("+ 新建档案")
                }
            }

            if (profiles.isEmpty()) {
                Text(
                    "暂无工作档案，请先创建一个。",
                    color = SettingsSecondary,
                    fontSize = 13.sp
                )
            }

            profiles.forEach { profile ->
                HorizontalDivider(
                    color = Color(0xFFECEFF3),
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                profile.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                salaryTypeName(profile.salaryType),
                                color = SettingsSecondary,
                                fontSize = 12.sp
                            )
                        }

                        if (profile.id == activeProfile?.id) {
                            Text(
                                "当前使用",
                                color = SettingsGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(7.dp))

                    Text(
                        profileSalaryDescription(profile),
                        color = SettingsSecondary,
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (profile.id != activeProfile?.id) {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            repository.switchProfile(profile.id)
                                            message = "已切换到：${profile.name}"
                                        } catch (e: Exception) {
                                            message = "切换失败：${e.message}"
                                        }
                                    }
                                }
                            ) {
                                Text("设为当前")
                            }
                        }

                        TextButton(
                            onClick = {
                                editingProfile = profile
                                editorVisible = true
                            }
                        ) {
                            Text("编辑")
                        }

                        TextButton(
                            onClick = {
                                if (profiles.size <= 1) {
                                    message = "至少需要保留一个工作档案，不能删除最后一个档案。"
                                } else {
                                    deletingProfile = profile
                                }
                            },
                            enabled = profiles.size > 1,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("删除")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsCard {
            Text(
                "应用设置",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("深色模式")
                    Text(
                        "跟随个人使用偏好",
                        color = SettingsSecondary,
                        fontSize = 12.sp
                    )
                }

                Switch(
                    checked = darkMode,
                    onCheckedChange = { darkMode = it }
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = Color(0xFFECEFF3)
            )

            SettingItem(
                "数据备份与恢复",
                "备份、恢复个人工资记录"
            )

            SettingItem(
                "关于应用",
                "SalaryBook 版本 1.0"
            )
        }

        if (message.isNotBlank()) {
            Spacer(Modifier.height(12.dp))

            Text(
                message,
                color = SettingsGreen,
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "zengyu6239@gamil.com",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = Color(0xFF9AA3B0),
            fontSize = 12.sp
        )

        Spacer(Modifier.height(20.dp))
    }

    if (editorVisible) {
        ProfileEditorDialog(
            profile = editingProfile,
            onDismiss = {
                editorVisible = false
            },
            onSave = { profile ->
                scope.launch {
                    try {
                        if (profile.id == 0L) {
                            repository.addProfile(profile)
                        } else {
                            repository.updateProfile(profile)
                        }

                        message = "工资方案保存成功"
                        editorVisible = false
                    } catch (e: Exception) {
                        message = "保存失败：${e.message}"
                    }
                }
            }
        )
    }

    deletingProfile?.let { profile ->
        AlertDialog(
            onDismissRequest = {
                deletingProfile = null
            },
            title = {
                Text("删除工作档案")
            },
            text = {
                Text(
                    "确定删除「${profile.name}」吗？\n\n" +
                            "删除后，该档案将不再显示在工作档案列表中。" +
                            "但该档案关联的历史工资记录会完整保留，不会被删除。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                if (profiles.size <= 1) {
                                    message = "至少需要保留一个工作档案。"
                                    deletingProfile = null
                                    return@launch
                                }

                                // 如果删除的是当前档案，先切换到其他档案。
                                if (activeProfile?.id == profile.id) {
                                    val replacement = profiles.firstOrNull {
                                        it.id != profile.id
                                    }

                                    if (replacement == null) {
                                        message = "没有可切换的工作档案，无法删除。"
                                        deletingProfile = null
                                        return@launch
                                    }

                                    repository.switchProfile(replacement.id)
                                }

                                // 数据库已移除工资记录与档案之间的级联删除关系。
                                // 此处只删除档案，不删除历史工资记录。
                                repository.deleteProfile(profile)

                                message = "档案「${profile.name}」已删除，历史工资记录已保留。"
                                deletingProfile = null
                            } catch (e: Exception) {
                                message = "删除失败：${e.message}"
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deletingProfile = null
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ProfileEditorDialog(
    profile: WorkProfileEntity?,
    onDismiss: () -> Unit,
    onSave: (WorkProfileEntity) -> Unit
) {
    var name by remember(profile?.id) {
        mutableStateOf(profile?.name ?: "")
    }

    var salaryType by remember(profile?.id) {
        mutableStateOf(profile?.salaryType ?: "HOURLY")
    }

    var monthlySalary by remember(profile?.id) {
        mutableStateOf((profile?.monthlySalary ?: 0.0).toString())
    }

    var hourlyRate by remember(profile?.id) {
        mutableStateOf((profile?.hourlyRate ?: 0.0).toString())
    }

    var dailyRate by remember(profile?.id) {
        mutableStateOf((profile?.dailyRate ?: 0.0).toString())
    }

    var weeklyRate by remember(profile?.id) {
        mutableStateOf((profile?.weeklyRate ?: 0.0).toString())
    }

    var pieceRate by remember(profile?.id) {
        mutableStateOf((profile?.pieceRate ?: 0.0).toString())
    }

    var commissionRate by remember(profile?.id) {
        mutableStateOf((profile?.commissionRate ?: 0.0).toString())
    }

    var monthlyAllocationDays by remember(profile?.id) {
        mutableStateOf((profile?.monthlyAllocationDays ?: 0.0).toString())
    }

    var weeklyAllocationDays by remember(profile?.id) {
        mutableStateOf((profile?.weeklyAllocationDays ?: 0.0).toString())
    }

    var standardHours by remember(profile?.id) {
        mutableStateOf((profile?.standardHoursPerDay ?: 8.0).toString())
    }

    var weekdayMultiplier by remember(profile?.id) {
        mutableStateOf((profile?.weekdayOvertimeMultiplier ?: 1.5).toString())
    }

    var restDayMultiplier by remember(profile?.id) {
        mutableStateOf((profile?.restDayOvertimeMultiplier ?: 2.0).toString())
    }

    var holidayMultiplier by remember(profile?.id) {
        mutableStateOf((profile?.holidayOvertimeMultiplier ?: 3.0).toString())
    }

    var nightAllowancePerHour by remember(profile?.id) {
        mutableStateOf((profile?.nightAllowancePerHour ?: 0.0).toString())
    }

    var customIncomeNames by remember(profile?.id) {
        mutableStateOf(profile?.customIncomeNames ?: "")
    }

    var customDeductionNames by remember(profile?.id) {
        mutableStateOf(profile?.customDeductionNames ?: "")
    }

    var errorMessage by remember { mutableStateOf("") }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (profile == null) "新建工作档案" else "编辑工作档案",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                ProfileInput("档案名称", name) { name = it }

                Text(
                    "工资类型",
                    fontSize = 13.sp,
                    color = SettingsSecondary
                )

                Box {
                    OutlinedButton(
                        onClick = { typeMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(salaryTypeName(salaryType))
                    }

                    DropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false }
                    ) {
                        listOf(
                            "MONTHLY" to "月薪",
                            "HOURLY" to "时薪",
                            "DAILY" to "日薪",
                            "WEEKLY" to "周薪",
                            "PIECEWORK" to "计件工资",
                            "MIXED" to "混合工资"
                        ).forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.second) },
                                onClick = {
                                    salaryType = item.first
                                    typeMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    "工资标准",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                if (salaryType == "MONTHLY" || salaryType == "MIXED") {
                    ProfileInput("月薪（元）", monthlySalary) {
                        monthlySalary = it
                    }

                    ProfileInput(
                        "月薪分摊天数（自行设置）",
                        monthlyAllocationDays
                    ) {
                        monthlyAllocationDays = it
                    }
                }

                if (salaryType == "HOURLY" || salaryType == "MIXED") {
                    ProfileInput("时薪（元）", hourlyRate) {
                        hourlyRate = it
                    }
                }

                if (salaryType == "DAILY" || salaryType == "MIXED") {
                    ProfileInput("日薪（元）", dailyRate) {
                        dailyRate = it
                    }
                }

                if (salaryType == "WEEKLY" || salaryType == "MIXED") {
                    ProfileInput("周薪（元）", weeklyRate) {
                        weeklyRate = it
                    }

                    ProfileInput(
                        "周薪分摊天数（自行设置）",
                        weeklyAllocationDays
                    ) {
                        weeklyAllocationDays = it
                    }
                }

                if (salaryType == "PIECEWORK" || salaryType == "MIXED") {
                    ProfileInput("每件单价（元）", pieceRate) {
                        pieceRate = it
                    }
                }

                if (salaryType == "MIXED") {
                    ProfileInput(
                        "提成比例（例如 0.05 表示 5%）",
                        commissionRate
                    ) {
                        commissionRate = it
                    }
                }

                HorizontalDivider()

                Text(
                    "工时与加班规则",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                ProfileInput("每日标准工时", standardHours) {
                    standardHours = it
                }

                ProfileInput("平时加班倍率", weekdayMultiplier) {
                    weekdayMultiplier = it
                }

                ProfileInput("休息日加班倍率", restDayMultiplier) {
                    restDayMultiplier = it
                }

                ProfileInput("法定节假日加班倍率", holidayMultiplier) {
                    holidayMultiplier = it
                }

                ProfileInput(
                    "夜班每小时补贴（元）",
                    nightAllowancePerHour
                ) {
                    nightAllowancePerHour = it
                }

                HorizontalDivider()

                Text(
                    "自定义收入项目",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Text(
                    "每行填写一个名称，例如：交通补助、餐费补助、全勤奖。",
                    fontSize = 12.sp,
                    color = SettingsSecondary
                )

                OutlinedTextField(
                    value = customIncomeNames,
                    onValueChange = { customIncomeNames = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    placeholder = { Text("交通补助\n餐费补助") }
                )

                Text(
                    "自定义扣款项目",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Text(
                    "每行填写一个名称，例如：迟到扣款、餐费扣款。",
                    fontSize = 12.sp,
                    color = SettingsSecondary
                )

                OutlinedTextField(
                    value = customDeductionNames,
                    onValueChange = { customDeductionNames = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    placeholder = { Text("迟到扣款\n其他扣款") }
                )

                if (errorMessage.isNotBlank()) {
                    Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }

                    Button(
                        onClick = {
                            val monthly = monthlySalary.toDoubleOrNull()
                            val hourly = hourlyRate.toDoubleOrNull()
                            val daily = dailyRate.toDoubleOrNull()
                            val weekly = weeklyRate.toDoubleOrNull()
                            val piece = pieceRate.toDoubleOrNull()
                            val commission = commissionRate.toDoubleOrNull()
                            val monthlyDays =
                                monthlyAllocationDays.toDoubleOrNull()
                            val weeklyDays =
                                weeklyAllocationDays.toDoubleOrNull()
                            val standard = standardHours.toDoubleOrNull()
                            val weekday = weekdayMultiplier.toDoubleOrNull()
                            val restDay = restDayMultiplier.toDoubleOrNull()
                            val holiday = holidayMultiplier.toDoubleOrNull()
                            val night = nightAllowancePerHour.toDoubleOrNull()

                            val values = listOf(
                                monthly,
                                hourly,
                                daily,
                                weekly,
                                piece,
                                commission,
                                monthlyDays,
                                weeklyDays,
                                standard,
                                weekday,
                                restDay,
                                holiday,
                                night
                            )

                            if (name.isBlank()) {
                                errorMessage = "请输入工作档案名称"
                                return@Button
                            }

                            if (values.any {
                                    it == null || !it.isFinite() || it < 0.0
                                }
                            ) {
                                errorMessage = "工资、工时和倍率必须填写有效的非负数字"
                                return@Button
                            }

                            if (standard == 0.0) {
                                errorMessage = "每日标准工时必须大于 0"
                                return@Button
                            }

                            val newProfile = WorkProfileEntity(
                                id = profile?.id ?: 0L,
                                name = name.trim(),
                                salaryType = salaryType,
                                monthlySalary = monthly!!,
                                hourlyRate = hourly!!,
                                dailyRate = daily!!,
                                weeklyRate = weekly!!,
                                pieceRate = piece!!,
                                commissionRate = commission!!,
                                monthlyAllocationDays = monthlyDays!!,
                                weeklyAllocationDays = weeklyDays!!,
                                standardHoursPerDay = standard!!,
                                weekdayOvertimeMultiplier = weekday!!,
                                restDayOvertimeMultiplier = restDay!!,
                                holidayOvertimeMultiplier = holiday!!,
                                nightAllowancePerHour = night!!,
                                customIncomeNames = customIncomeNames.trim(),
                                customDeductionNames = customDeductionNames.trim(),
                                isActive = profile?.isActive ?: true,
                                createdAt = profile?.createdAt
                                    ?: System.currentTimeMillis()
                            )

                            onSave(newProfile)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            label,
            fontSize = 12.sp,
            color = SettingsSecondary
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}

@Composable
private fun SettingItem(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(4.dp))

        Text(
            description,
            fontSize = 12.sp,
            color = SettingsSecondary
        )
    }
}

private fun salaryTypeName(type: String): String {
    return when (type) {
        "MONTHLY" -> "月薪制"
        "HOURLY" -> "时薪制"
        "DAILY" -> "日薪制"
        "WEEKLY" -> "周薪制"
        "PIECEWORK" -> "计件工资"
        "MIXED" -> "混合工资"
        else -> "自定义工资"
    }
}

private fun profileSalaryDescription(
    profile: WorkProfileEntity
): String {
    return when (profile.salaryType) {
        "MONTHLY" ->
            "月薪：¥${profile.monthlySalary} · 分摊天数：${profile.monthlyAllocationDays}"

        "HOURLY" ->
            "时薪：¥${profile.hourlyRate}"

        "DAILY" ->
            "日薪：¥${profile.dailyRate}"

        "WEEKLY" ->
            "周薪：¥${profile.weeklyRate} · 分摊天数：${profile.weeklyAllocationDays}"

        "PIECEWORK" ->
            "每件：¥${profile.pieceRate}"

        "MIXED" ->
            "月薪 ¥${profile.monthlySalary} · 时薪 ¥${profile.hourlyRate} · 日薪 ¥${profile.dailyRate}"

        else -> "自定义工资方案"
    }
}
