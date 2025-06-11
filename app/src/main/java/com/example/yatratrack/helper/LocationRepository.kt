package com.example.yatratrack.helper

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LocationRepository(private val context: Context) {
    private val sharedPrefs = context.getSharedPreferences("yatra_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        const val LOCATIONS_KEY = "tracked_locations"
    }

    fun getAllLocations(): List<LocationData> {
        val locationsJson = sharedPrefs.getString(LOCATIONS_KEY, "[]")
        return try {
            val type = object : TypeToken<List<LocationData>>() {}.type
            gson.fromJson(locationsJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearAllLocations() {
        sharedPrefs.edit().remove(LOCATIONS_KEY).apply()
    }

    fun getLocationCount(): Int {
        return getAllLocations().size
    }
}