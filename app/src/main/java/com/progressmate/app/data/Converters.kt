package com.progressmate.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromPriority(priority: Priority): String = priority.name

    @TypeConverter
    fun toPriority(value: String): Priority = runCatching { Priority.valueOf(value) }.getOrDefault(Priority.MEDIUM)

    @TypeConverter
    fun fromSessionType(type: SessionType): String = type.name

    @TypeConverter
    fun toSessionType(value: String): SessionType = runCatching { SessionType.valueOf(value) }.getOrDefault(SessionType.WORK)
}
