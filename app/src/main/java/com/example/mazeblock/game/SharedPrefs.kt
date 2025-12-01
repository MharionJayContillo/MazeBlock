package com.example.mazeblock.game

import android.content.Context
import android.content.SharedPreferences

object SharedPrefs {
    private const val PREFS_NAME = "MazeBlockPrefs"
    private const val KEY_UNLOCKED_LEVEL = "HighestUnlockedLevel"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }


    fun getHighestUnlockedLevel(context: Context): Int {
        return getPrefs(context).getInt(KEY_UNLOCKED_LEVEL, 1)
    }


    fun setHighestUnlockedLevel(context: Context, level: Int) {
        getPrefs(context).edit().putInt(KEY_UNLOCKED_LEVEL, level).apply()
    }
}
