package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.CommandHistoryEntity
import com.example.data.entity.RoutineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JarvisDao {
    // Command History
    @Query("SELECT * FROM command_history ORDER BY timestamp DESC LIMIT 100")
    fun getAllHistory(): Flow<List<CommandHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: CommandHistoryEntity): Long

    @Query("DELETE FROM command_history")
    suspend fun clearHistory()

    @Query("DELETE FROM command_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    // Routines
    @Query("SELECT * FROM routines ORDER BY id ASC")
    fun getAllRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE isEnabled = 1")
    suspend fun getActiveRoutines(): List<RoutineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Delete
    suspend fun deleteRoutine(routine: RoutineEntity)

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getRoutineById(id: Long): RoutineEntity?
}
