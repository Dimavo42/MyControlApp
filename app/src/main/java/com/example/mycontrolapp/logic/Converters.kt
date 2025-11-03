package com.example.mycontrolapp.logic
import androidx.room.TypeConverter
import com.example.mycontrolapp.logic.sharedEnums.Profession
import com.example.mycontrolapp.logic.sharedEnums.Team
import com.example.mycontrolapp.logic.sharedEnums.TimeSplitMode
import java.time.LocalDate
import java.util.*


class Converters {

    /* ---------- List<String> <-> CSV ---------- */
    @TypeConverter
    fun fromCsv(csv: String?): List<String> =
        csv?.takeIf { it.isNotBlank() }?.split("|") ?: emptyList()

    @TypeConverter
    fun toCsv(list: List<String>?): String =
        list?.joinToString("|") ?: ""

    /* ---------- java.util.Date <-> Long (millis) ---------- */
    @TypeConverter
    fun fromDate(d: Date?): Long? = d?.time

    @TypeConverter
    fun toDate(ms: Long?): Date? = ms?.let { Date(it) }

    /* ---------- java.time.LocalDate <-> Int (epochDay) ---------- */
    @TypeConverter
    fun fromLocalDate(d: LocalDate?): Int? = d?.toEpochDay()?.toInt()

    @TypeConverter
    fun toLocalDate(epoch: Int?): LocalDate? =
        epoch?.toLong()?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun fromTeam(value: Team?): String? = value?.name

    @TypeConverter
    fun toTeam(value: String?): Team? =
        value?.let { runCatching { Team.valueOf(it) }.getOrElse { Team.Unknown } }


    /* ---------- Profession <-> String ---------- */
    @TypeConverter
    fun fromProfession(p: Profession?): String? = p?.name

    @TypeConverter
    fun toProfession(s: String?): Profession? =
        s?.let { runCatching { Profession.valueOf(it) }.getOrElse { Profession.Unknown } }

    /* ---------- TimeSplitMode <-> String ---------- */
    @TypeConverter
    fun fromTimeSplitMode(p: TimeSplitMode?): String? = p?.name


    @TypeConverter
    fun toTimeSplitMode(value: String?): TimeSplitMode? =
        value?.let { runCatching { TimeSplitMode.valueOf(it) }.getOrElse { TimeSplitMode.NONE } }

}