
package com.salarybook.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WorkProfileEntity::class,
        WorkRecordEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class SalaryDatabase : RoomDatabase() {

    abstract fun workProfileDao(): WorkProfileDao

    abstract fun workRecordDao(): WorkRecordDao

    companion object {

        @Volatile
        private var INSTANCE: SalaryDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    "ALTER TABLE work_profiles ADD COLUMN monthlyAllocationDays REAL NOT NULL DEFAULT 0.0"
                )

                db.execSQL(
                    "ALTER TABLE work_profiles ADD COLUMN weeklyAllocationDays REAL NOT NULL DEFAULT 0.0"
                )

                db.execSQL(
                    "ALTER TABLE work_profiles ADD COLUMN customIncomeNames TEXT NOT NULL DEFAULT ''"
                )

                db.execSQL(
                    "ALTER TABLE work_profiles ADD COLUMN customDeductionNames TEXT NOT NULL DEFAULT ''"
                )

                db.execSQL(
                    "ALTER TABLE work_records ADD COLUMN commissionBase REAL NOT NULL DEFAULT 0.0"
                )

                db.execSQL(
                    "ALTER TABLE work_records ADD COLUMN nightHours REAL NOT NULL DEFAULT 0.0"
                )

                db.execSQL(
                    "ALTER TABLE work_records ADD COLUMN paidUnits REAL NOT NULL DEFAULT 1.0"
                )

                db.execSQL(
                    "ALTER TABLE work_records ADD COLUMN customIncomeValues TEXT NOT NULL DEFAULT ''"
                )

                db.execSQL(
                    "ALTER TABLE work_records ADD COLUMN customDeductionValues TEXT NOT NULL DEFAULT ''"
                )

                db.execSQL(
                    "ALTER TABLE work_records ADD COLUMN salaryRuleSnapshot TEXT NOT NULL DEFAULT ''"
                )

                db.execSQL(
                    "ALTER TABLE work_records ADD COLUMN calculationDetails TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE work_records ADD COLUMN workSegmentsJson TEXT NOT NULL DEFAULT '[]'"
                )
            }
        }

        /**
         * 数据库 3 -> 4
         *
         * 1. 移除 work_records 对 work_profiles 的级联外键。
         * 2. 新增 profileNameSnapshot。
         * 3. 完整复制旧工资记录。
         * 4. 为旧记录补充原档案名称。
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {

            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
                    CREATE TABLE work_records_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        profileId INTEGER NOT NULL,
                        workDate TEXT NOT NULL,
                        startTimeMillis INTEGER,
                        endTimeMillis INTEGER,
                        breakMinutes INTEGER NOT NULL,
                        normalMinutes INTEGER NOT NULL,
                        overtimeMinutes INTEGER NOT NULL,
                        overtimeType TEXT NOT NULL,
                        workSegmentsJson TEXT NOT NULL DEFAULT '[]',
                        pieceCount REAL NOT NULL,
                        piecePay REAL NOT NULL,
                        commissionBase REAL NOT NULL DEFAULT 0.0,
                        commissionPay REAL NOT NULL,
                        bonus REAL NOT NULL,
                        allowance REAL NOT NULL,
                        nightAllowance REAL NOT NULL,
                        nightHours REAL NOT NULL DEFAULT 0.0,
                        deduction REAL NOT NULL,
                        paidUnits REAL NOT NULL DEFAULT 1.0,
                        customIncomeValues TEXT NOT NULL DEFAULT '',
                        customDeductionValues TEXT NOT NULL DEFAULT '',
                        notes TEXT NOT NULL,
                        snapshotHourlyRate REAL NOT NULL,
                        snapshotOvertimeMultiplier REAL NOT NULL,
                        snapshotPieceRate REAL NOT NULL,
                        salaryRuleSnapshot TEXT NOT NULL DEFAULT '',
                        calculationDetails TEXT NOT NULL DEFAULT '',
                        normalPay REAL NOT NULL,
                        overtimePay REAL NOT NULL,
                        totalPay REAL NOT NULL,
                        receivedPay REAL,
                        receivedAt INTEGER,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        profileNameSnapshot TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO work_records_new (
                        id,
                        profileId,
                        workDate,
                        startTimeMillis,
                        endTimeMillis,
                        breakMinutes,
                        normalMinutes,
                        overtimeMinutes,
                        overtimeType,
                        workSegmentsJson,
                        pieceCount,
                        piecePay,
                        commissionBase,
                        commissionPay,
                        bonus,
                        allowance,
                        nightAllowance,
                        nightHours,
                        deduction,
                        paidUnits,
                        customIncomeValues,
                        customDeductionValues,
                        notes,
                        snapshotHourlyRate,
                        snapshotOvertimeMultiplier,
                        snapshotPieceRate,
                        salaryRuleSnapshot,
                        calculationDetails,
                        normalPay,
                        overtimePay,
                        totalPay,
                        receivedPay,
                        receivedAt,
                        createdAt,
                        updatedAt,
                        profileNameSnapshot
                    )
                    SELECT
                        r.id,
                        r.profileId,
                        r.workDate,
                        r.startTimeMillis,
                        r.endTimeMillis,
                        r.breakMinutes,
                        r.normalMinutes,
                        r.overtimeMinutes,
                        r.overtimeType,
                        r.workSegmentsJson,
                        r.pieceCount,
                        r.piecePay,
                        r.commissionBase,
                        r.commissionPay,
                        r.bonus,
                        r.allowance,
                        r.nightAllowance,
                        r.nightHours,
                        r.deduction,
                        r.paidUnits,
                        r.customIncomeValues,
                        r.customDeductionValues,
                        r.notes,
                        r.snapshotHourlyRate,
                        r.snapshotOvertimeMultiplier,
                        r.snapshotPieceRate,
                        r.salaryRuleSnapshot,
                        r.calculationDetails,
                        r.normalPay,
                        r.overtimePay,
                        r.totalPay,
                        r.receivedPay,
                        r.receivedAt,
                        r.createdAt,
                        r.updatedAt,
                        COALESCE(
                            (
                                SELECT p.name
                                FROM work_profiles p
                                WHERE p.id = r.profileId
                                LIMIT 1
                            ),
                            ''
                        )
                    FROM work_records r
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE work_records")

                db.execSQL(
                    "ALTER TABLE work_records_new RENAME TO work_records"
                )

                db.execSQL(
                    "CREATE INDEX index_work_records_profileId ON work_records(profileId)"
                )

                db.execSQL(
                    "CREATE INDEX index_work_records_workDate ON work_records(workDate)"
                )
            }
        }

        fun getInstance(context: Context): SalaryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SalaryDatabase::class.java,
                    "salary_book.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4
                    )
                    .build()
                    .also {
                        INSTANCE = it
                    }
            }
        }
    }
}
