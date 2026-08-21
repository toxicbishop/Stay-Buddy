package com.example.staybuddy.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return gson.toJson(value ?: emptyList<String>())
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, Int>?): String {
        return gson.toJson(value ?: emptyMap<String, Int>())
    }

    @TypeConverter
    fun toStringMap(value: String): Map<String, Int> {
        val type = object : TypeToken<Map<String, Int>>() {}.type
        return gson.fromJson(value, type) ?: emptyMap()
    }
    
    @TypeConverter
    fun fromStringLongMap(value: Map<String, Long>?): String {
        return gson.toJson(value ?: emptyMap<String, Long>())
    }

    @TypeConverter
    fun toStringLongMap(value: String): Map<String, Long> {
        val type = object : TypeToken<Map<String, Long>>() {}.type
        return gson.fromJson(value, type) ?: emptyMap()
    }
    
    @TypeConverter
    fun fromStringListMap(value: Map<String, List<String>>?): String {
        return gson.toJson(value ?: emptyMap<String, List<String>>())
    }
    
    @TypeConverter
    fun toStringListMap(value: String): Map<String, List<String>> {
        val type = object : TypeToken<Map<String, List<String>>>() {}.type
        return gson.fromJson(value, type) ?: emptyMap()
    }
    
    @TypeConverter
    fun fromStringToStringMap(value: Map<String, String>?): String {
        return gson.toJson(value ?: emptyMap<String, String>())
    }

    @TypeConverter
    fun toStringToStringMap(value: String): Map<String, String> {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, type) ?: emptyMap()
    }
}
