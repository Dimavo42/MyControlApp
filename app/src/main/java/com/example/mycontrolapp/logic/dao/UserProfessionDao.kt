package com.example.mycontrolapp.logic.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.mycontrolapp.logic.User
import com.example.mycontrolapp.logic.UserProfession
import com.example.mycontrolapp.logic.sharedEnums.Profession
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfessionDao {

    @Query("SELECT profession FROM user_professions WHERE userId = :userId")
    fun professionsForUserFlow(userId: String): Flow<List<Profession>>


    // Eligible users for a role: active AND either primary matches OR cross-ref contains role
    @Query("""
        SELECT u.* 
        FROM users u
        JOIN user_professions up 
              ON up.userId = u.id AND up.profession = :role
        WHERE u.isActive = 1
        GROUP BY u.id
        ORDER BY u.name
    """)
    fun usersByProfessionFlow(role: Profession): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(ref: UserProfession)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(refs: List<UserProfession>)

    @Query("DELETE FROM user_professions WHERE userId = :userId AND profession = :profession")
    suspend fun delete(userId: String, profession: Profession)

    @Query("DELETE FROM user_professions WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Transaction
    suspend fun setUserProfessions(userId: String, professions: Set<Profession>) {
        deleteAllForUser(userId)
        if (professions.isNotEmpty()) {
            insertAll(professions.map { UserProfession(userId = userId, profession = it) })
        }
    }
    @Query("DELETE FROM user_professions")
    suspend fun deleteAll()


}