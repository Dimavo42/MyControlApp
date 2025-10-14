package com.example.mycontrolapp.logic.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.mycontrolapp.logic.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY name COLLATE NOCASE ASC")
    fun getAllFlow(): Flow<List<User>>

    @Query("SELECT * FROM users")
    suspend fun getAllOnce(): List<User>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun upsert(user: User)

    @Update(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun update(user: User)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: String)


}