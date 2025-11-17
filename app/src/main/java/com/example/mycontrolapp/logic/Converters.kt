package com.example.mycontrolapp.logic
import androidx.room.TypeConverter
import com.example.mycontrolapp.logic.sharedData.TimeSegment
import com.example.mycontrolapp.logic.sharedEnums.Profession
import com.example.mycontrolapp.logic.sharedEnums.Team
import com.example.mycontrolapp.logic.sharedEnums.TimeSplitMode
import java.time.LocalDate
import java.util.*


class Converters {

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

    @TypeConverter
    fun fromSegments(list: List<TimeSegment>): String =
        list.joinToString(";") { seg ->
            "${seg.start},${seg.end},${seg.userId}"
        }
    @TypeConverter
    fun toSegments(data: String): List<TimeSegment> =
        if (data.isBlank()) emptyList()
        else data.split(";").map { part ->
            val pieces = part.split(",")
            TimeSegment(
                start = pieces[0].toLong(),
                end = pieces[1].toLong(),
                userId = pieces[2]
            )
        }

}