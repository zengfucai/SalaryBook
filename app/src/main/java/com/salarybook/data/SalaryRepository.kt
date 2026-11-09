
package com.salarybook.data

import com.salarybook.data.local.SalaryDatabase
import com.salarybook.data.local.WorkProfileEntity
import com.salarybook.data.local.WorkRecordEntity
import kotlinx.coroutines.flow.Flow

class SalaryRepository(
    private val database: SalaryDatabase
) {

    private val profileDao = database.workProfileDao()
    private val recordDao = database.workRecordDao()

    // ---------------- 工作档案 ----------------

    fun getAllProfiles(): Flow<List<WorkProfileEntity>> {
        return profileDao.getAllProfiles()
    }

    fun getActiveProfile(): Flow<WorkProfileEntity?> {
        return profileDao.getActiveProfile()
    }

    suspend fun getProfileById(id: Long): WorkProfileEntity? {
        return profileDao.getProfileById(id)
    }

    suspend fun addProfile(profile: WorkProfileEntity): Long {
        val id = profileDao.insert(profile)

        if (profile.isActive) {
            profileDao.setActiveProfile(id)
        }

        return id
    }

    suspend fun updateProfile(profile: WorkProfileEntity) {
        profileDao.update(profile)

        if (profile.isActive) {
            profileDao.setActiveProfile(profile.id)
        }
    }

    // 删除档案不会删除工资记录
    suspend fun deleteProfile(profile: WorkProfileEntity) {
        profileDao.delete(profile)
    }

    suspend fun switchProfile(id: Long) {
        profileDao.setActiveProfile(id)
    }

    // ---------------- 每日工时记录 ----------------

    fun getAllRecords(): Flow<List<WorkRecordEntity>> {
        return recordDao.getAllRecords()
    }

    fun getRecordsByProfile(
        profileId: Long
    ): Flow<List<WorkRecordEntity>> {
        return recordDao.getRecordsByProfile(profileId)
    }

    suspend fun getRecordById(
        recordId: Long
    ): WorkRecordEntity? {
        return recordDao.getRecordById(recordId)
    }

    suspend fun getRecordsByDate(
        date: String
    ): List<WorkRecordEntity> {
        return recordDao.getRecordsByDate(date)
    }

    fun getRecordsByDateRange(
        profileId: Long,
        startDate: String,
        endDate: String
    ): Flow<List<WorkRecordEntity>> {
        return recordDao.getRecordsByDateRange(
            profileId,
            startDate,
            endDate
        )
    }

    fun getAllRecordsByDateRange(
        startDate: String,
        endDate: String
    ): Flow<List<WorkRecordEntity>> {
        return recordDao.getAllRecordsByDateRange(
            startDate,
            endDate
        )
    }

    suspend fun getRecordByDate(
        profileId: Long,
        date: String
    ): WorkRecordEntity? {
        return recordDao.getRecordByDate(profileId, date)
    }

    suspend fun saveRecord(record: WorkRecordEntity): Long {
        return recordDao.insert(record)
    }

    suspend fun updateRecord(record: WorkRecordEntity) {
        recordDao.update(record)
    }

    suspend fun deleteRecord(record: WorkRecordEntity) {
        recordDao.delete(record)
    }

    // ---------------- 工资统计 ----------------

    fun getTotalPay(
        profileId: Long,
        startDate: String,
        endDate: String
    ): Flow<Double> {
        return recordDao.getTotalPay(
            profileId,
            startDate,
            endDate
        )
    }

    fun getWorkDayCount(
        profileId: Long,
        startDate: String,
        endDate: String
    ): Flow<Int> {
        return recordDao.getWorkDayCount(
            profileId,
            startDate,
            endDate
        )
    }
}
