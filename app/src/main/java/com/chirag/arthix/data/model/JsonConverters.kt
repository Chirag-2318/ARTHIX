package com.chirag.arthix.data.model

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room [TypeConverter]s for JSON-serialized fields on [ReportEntity].
 *
 * Uses Gson for simplicity — these are write-once, read-whole blobs (category
 * breakdown map and suggestions list), not independently queryable columns.
 * A normalized child table is not warranted on a hackathon timeline (PRD §2.6).
 */
class JsonConverters {

    private val gson = Gson()

    // Map<String, Long> — used by ReportEntity.categoryBreakdownJson
    @TypeConverter
    fun fromCategoryBreakdownMap(map: Map<String, Long>?): String? {
        return map?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toCategoryBreakdownMap(json: String?): Map<String, Long>? {
        if (json == null) return null
        val type = object : TypeToken<Map<String, Long>>() {}.type
        return gson.fromJson(json, type)
    }

    // List<String> — used by ReportEntity.suggestionsJson
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return list?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toStringList(json: String?): List<String>? {
        if (json == null) return null
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }
}
