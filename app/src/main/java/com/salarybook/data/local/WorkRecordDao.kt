
package com.salarybook.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: WorkRecordEntity): Long

    @Update
    suspend fun update(record: WorkRecordEntity)

    @Delete
    suspend fun delete(record: WorkRecordEntity)

    // 查询全部档案的记录，包括档案已经删除的记录
    @Query(
        """
        SELECT * FROM work_records
        ORDER BY workDate DESC, id DESC
        """
    )
    fun getAllRecords(): Flow<List<WorkRecordEntity>>

    // 查询指定档案的记录
    @Query(
        """
        SELECT * FROM work_records
        WHERE profileId = :profileId
        ORDER BY workDate DESC, id DESC
        """
    )
    fun getRecordsByProfile(
        profileId: Long
    ): Flow<List<WorkRecordEntity>>

    // 查询指定档案指定日期的记录
    @Query(
        """
        SELECT * FROM work_records
        WHERE profileId = :profileId
        AND workDate = :date
        LIMIT 1
        """
    )
    suspend fun getRecordByDate(
        profileId: Long,
        date: String
    ): WorkRecordEntity?

    // 按记录 ID 查询
    @Query(
        """
        SELECT * FROM work_records
        WHERE id = :recordId
        LIMIT 1
        """
    )
    suspend fun getRecordById(
        recordId: Long
    ): WorkRecordEntity?

    // 查询全部档案指定日期的记录
    @Query(
        """
        SELECT * FROM work_records
        WHERE workDate = :date
        ORDER BY id DESC
        """
    )
    suspend fun getRecordsByDate(
        date: String
    ): List<WorkRecordEntity>

    // 查询指定档案的日期范围记录
    @Query(
        """
        SELECT * FROM work_records
        WHERE profileId = :profileId
        AND workDate BETWEEN :startDate AND :endDate
        ORDER BY workDate ASC
        """
    )
    fun getRecordsByDateRange(
        profileId: Long,
        startDate: String,
        endDate: String
    ): Flow<List<WorkRecordEntity>>

    // 全部档案的日期范围记录
    @Query(
        """
        SELECT * FROM work_records
        WHERE workDate BETWEEN :startDate AND :endDate
        ORDER BY workDate ASC, id ASC
        """
    )
    fun getAllRecordsByDateRange(
        startDate: String,
        endDate: String
    ): Flow<List<WorkRecordEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(totalPay), 0)
        FROM work_records
        WHERE profileId = :profileId
        AND workDate BETWEEN :startDate AND :endDate
        """
    )
    fun getTotalPay(
        profileId: Long,
        startDate: String,
        endDate: String
    ): Flow<Double>

    @Query(
        """
        SELECT COUNT(DISTINCT workDate)
        FROM work_records
        WHERE profileId = :profileId
        AND workDate BETWEEN :startDate AND :endDate
        """
    )
    fun getWorkDayCount(
        profileId: Long,
        startDate: String,
        endDate: String
    ): Flow<Int>
}
