package com.example.mycontrolapp.logic

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.mycontrolapp.logic.dao.ActivityDao
import com.example.mycontrolapp.logic.dao.ActivityRoleRequirementDao
import com.example.mycontrolapp.logic.dao.ActivityTimeSplitDao
import com.example.mycontrolapp.logic.dao.AssignmentDao
import com.example.mycontrolapp.logic.dao.UserDao
import com.example.mycontrolapp.logic.dao.UserProfessionDao

@Database(
    entities = [Activity::class,
        User::class,
        Assignment::class,
        ActivityRoleRequirement::class,
        UserProfession::class,
        ActivityTimeSplit::class],
    version = 28,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun userDao(): UserDao
    abstract fun assignmentDao(): AssignmentDao
    abstract fun userProfessionDao(): UserProfessionDao
    abstract fun activityRoleRequirementDao(): ActivityRoleRequirementDao
    abstract fun  activityTimeSplitDao(): ActivityTimeSplitDao


}