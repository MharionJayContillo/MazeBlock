package com.example.mazeblock.game

import android.content.Context
import android.content.SharedPreferences

object SharedPrefs {
    private const val PREF_NAME = "MazeBlockPrefs"
    private const val KEY_HIGHEST_LEVEL = "highest_level"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getHighestLevel(context: Context): Int {
        return getPrefs(context).getInt(KEY_HIGHEST_LEVEL, 1)
    }

    fun unlockLevel(context: Context, level: Int) {
        val current = getHighestLevel(context)
        if (level > current) {
            getPrefs(context).edit().putInt(KEY_HIGHEST_LEVEL, level).apply()
        }
    }
}