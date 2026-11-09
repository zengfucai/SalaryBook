
package com.salarybook.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: WorkProfileEntity): Long

    @Update
    suspend fun update(profile: WorkProfileEntity)

    @Delete
    suspend fun delete(profile: WorkProfileEntity)

    @Query("SELECT * FROM work_profiles ORDER BY createdAt DESC")
    fun getAllProfiles(): Flow<List<WorkProfileEntity>>

    @Query("SELECT * FROM work_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Long): WorkProfileEntity?

    @Query("SELECT * FROM work_profiles WHERE isActive = 1 LIMIT 1")
    fun getActiveProfile(): Flow<WorkProfileEntity?>

    @Query("UPDATE work_profiles SET isActive = 0")
    suspend fun deactivateAllProfiles()

    @Query("UPDATE work_profiles SET isActive = 1 WHERE id = :id")
    suspend fun activateProfile(id: Long)

    /**
     * 原子切换工作档案，避免出现多个当前档案。
     */
    @Transaction
    suspend fun setActiveProfile(id: Long) {
        deactivateAllProfiles()
        activateProfile(id)
    }
}
