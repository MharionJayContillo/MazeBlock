package com.example.mazeblock

import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mazeblock.game.GameView
import com.example.mazeblock.game.SharedPrefs
import com.example.mazeblock.levels.GameLevels

class GameActivity : AppCompatActivity() {

    private var currentLevel = 1
    private lateinit var gameContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        gameContainer = findViewById(R.id.gameContainer)
        val btnRestart = findViewById<Button>(R.id.btnRestart)

        currentLevel = intent.getIntExtra("LEVEL_ID", 1)

        loadLevel()

        btnRestart.setOnClickListener {
            loadLevel() // Reload logic
        }
    }

    private fun loadLevel() {
        if (gameContainer.childCount > 1) {
            gameContainer.removeViewAt(0)
        }

        val levelData = GameLevels.getLevel(currentLevel)

        val gameView = GameView(this, levelData) {
            onLevelComplete()
        }

        gameContainer.addView(gameView, 0)
    }

    private fun onLevelComplete() {
        runOnUiThread {
            Toast.makeText(this, "Level Complete! Congratulations!", Toast.LENGTH_SHORT).show()
            SharedPrefs.unlockLevel(this, currentLevel + 1)
            finish() // Go back to level select
        }
    }
}