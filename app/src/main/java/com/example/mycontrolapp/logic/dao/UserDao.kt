package com.example.mycontrolapp.logic.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.mycontrolapp.logic.User
import com.example.mycontrolapp.logic.sharedEnums.Profession
import com.example.mycontrolapp.logic.sharedEnums.Team
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

    @Query("""
        SELECT u.* FROM users u
        WHERE u.isActive = 1
          AND u.team = :team
        ORDER BY u.name COLLATE NOCASE
    """)
    suspend fun getActiveUsersByTeam(team: Team): List<User>

    @Query("""
        SELECT u.* FROM users u
        LEFT JOIN user_professions up ON up.userId = u.id
        WHERE u.isActive = 1
          AND u.team = :team
          AND (:role IS NULL OR u.canFillAnyRole = 1 OR up.profession = :role)
        GROUP BY u.id
        ORDER BY u.name COLLATE NOCASE
    """)
    suspend fun getEligibleUsersForTeamAndRole(team: Team, role: Profession?): List<User>


}