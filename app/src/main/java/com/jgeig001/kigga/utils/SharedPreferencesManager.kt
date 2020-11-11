package com.jgeig001.kigga.utils

import android.content.Context

object SharedPreferencesManager {

    const val DEFAULT_INT = 0
    const val DEFAULT_STRING = ""
    const val DEFAULT_BOOL = false

    private const val SHARED_FILE = "SHARED_FILE"

    fun writeBoolean(context: Context, key: String, value: Boolean) {
        val sharedPref = context.getSharedPreferences(SHARED_FILE, Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(context: Context, key: String): Boolean {
        val sharedPref = context.getSharedPreferences(SHARED_FILE, Context.MODE_PRIVATE)
        return sharedPref.getBoolean(key, DEFAULT_BOOL)
    }

    fun writeInt(context: Context, key: String, value: Int) {
        val sharedPref = context.getSharedPreferences(SHARED_FILE, Context.MODE_PRIVATE)
        sharedPref.edit().putInt(key, value).apply()
    }

    fun getInt(context: Context, key: String): Int {
        val sharedPref = context.getSharedPreferences(SHARED_FILE, Context.MODE_PRIVATE)
        return sharedPref.getInt(key, DEFAULT_INT)
    }

    /**
     * saves a String in SharedPreferences
     */
    fun writeString(context: Context, key: String, value: String) {
        val sharedPref = context.getSharedPreferences(SHARED_FILE, Context.MODE_PRIVATE)
        sharedPref.edit().putString(key, value).apply()
    }

    /**
     * get saved String from getSharedPreferences
     * @param context context
     * @param key key
     * @return returns value:String if set, default value is empty String ""
     */
    fun getString(context: Context, key: String): String? {
        val sharedPref = context.getSharedPreferences(SHARED_FILE, Context.MODE_PRIVATE)
        return sharedPref.getString(key, DEFAULT_STRING)
    }

}