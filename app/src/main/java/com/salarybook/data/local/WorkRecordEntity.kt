
package com.salarybook.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "work_records",
    indices = [
        Index("profileId"),
        Index("workDate")
    ]
)
data class WorkRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val profileId: Long,
    val workDate: String,

    val startTimeMillis: Long? = null,
    val endTimeMillis: Long? = null,

    val breakMinutes: Int = 0,
    val normalMinutes: Int = 0,
    val overtimeMinutes: Int = 0,

    val overtimeType: String = "NONE",

    @ColumnInfo(defaultValue = "'[]'")
    val workSegmentsJson: String = "[]",

    val pieceCount: Double = 0.0,
    val piecePay: Double = 0.0,

    @ColumnInfo(defaultValue = "0.0")
    val commissionBase: Double = 0.0,

    val commissionPay: Double = 0.0,

    val bonus: Double = 0.0,
    val allowance: Double = 0.0,
    val nightAllowance: Double = 0.0,

    @ColumnInfo(defaultValue = "0.0")
    val nightHours: Double = 0.0,

    val deduction: Double = 0.0,

    @ColumnInfo(defaultValue = "1.0")
    val paidUnits: Double = 1.0,

    @ColumnInfo(defaultValue = "''")
    val customIncomeValues: String = "",

    @ColumnInfo(defaultValue = "''")
    val customDeductionValues: String = "",

    val notes: String = "",

    val snapshotHourlyRate: Double = 0.0,
    val snapshotOvertimeMultiplier: Double = 1.0,
    val snapshotPieceRate: Double = 0.0,

    @ColumnInfo(defaultValue = "''")
    val salaryRuleSnapshot: String = "",

    @ColumnInfo(defaultValue = "''")
    val calculationDetails: String = "",

    val normalPay: Double = 0.0,
    val overtimePay: Double = 0.0,
    val totalPay: Double = 0.0,

    val receivedPay: Double? = null,
    val receivedAt: Long? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // 记录创建时的档案名称，档案删除后仍可识别历史记录
    @ColumnInfo(defaultValue = "''")
    val profileNameSnapshot: String = ""
)
