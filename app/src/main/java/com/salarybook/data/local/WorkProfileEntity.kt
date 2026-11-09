package com.salarybook.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_profiles")
data class WorkProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    // MONTHLY、HOURLY、DAILY、WEEKLY、PIECEWORK、MIXED
    val salaryType: String = "HOURLY",

    val monthlySalary: Double = 0.0,
    val hourlyRate: Double = 0.0,
    val dailyRate: Double = 0.0,
    val weeklyRate: Double = 0.0,
    val pieceRate: Double = 0.0,

    // 提成比例：0.05 表示 5%
    val commissionRate: Double = 0.0,

    // 由用户配置，0 表示尚未配置
    @ColumnInfo(defaultValue = "0.0")
    val monthlyAllocationDays: Double = 0.0,

    @ColumnInfo(defaultValue = "0.0")
    val weeklyAllocationDays: Double = 0.0,

    val standardHoursPerDay: Double = 8.0,

    val weekdayOvertimeMultiplier: Double = 1.5,
    val restDayOvertimeMultiplier: Double = 2.0,
    val holidayOvertimeMultiplier: Double = 3.0,

    val nightAllowancePerHour: Double = 0.0,

    // 每行一个自定义项目名称
    @ColumnInfo(defaultValue = "''")
    val customIncomeNames: String = "",

    @ColumnInfo(defaultValue = "''")
    val customDeductionNames: String = "",

    val isActive: Boolean = true,

    val createdAt: Long = System.currentTimeMillis()
)