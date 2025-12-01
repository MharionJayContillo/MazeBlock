package com.example.mazeblock

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.mazeblock.game.GameView
import com.example.mazeblock.game.SharedPrefs

class GameActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private var currentLevelId: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen setup
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        supportActionBar?.hide()

        // Get the level passed from the menu
        currentLevelId = intent.getIntExtra("LEVEL_ID", 1)

        // Initialize the GameView
        gameView = GameView(this, currentLevelId)

        // Create a layout to hold the Game + Restart Button
        val container = FrameLayout(this)
        container.addView(gameView)

        // --- RESTART BUTTON ---
        val restartBtn = Button(this)
        restartBtn.text = "RESTART"
        restartBtn.textSize = 14f
        restartBtn.setTextColor(Color.WHITE)
        restartBtn.setBackgroundColor(Color.parseColor("#88000000")) // Semi-transparent black

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.TOP or Gravity.END
        params.setMargins(24, 24, 24, 24)
        restartBtn.layoutParams = params

        restartBtn.setOnClickListener {
            // Reset the current level
            gameView.setLevel(currentLevelId)
        }

        container.addView(restartBtn)
        setContentView(container)
    }

    override fun onPause() {
        super.onPause()
        gameView.pause()
    }

    override fun onResume() {
        super.onResume()
        gameView.resume()
    }

    // --- THIS FUNCTION MUST BE PUBLIC ---
    // It is called by GameView when the player reaches the goal
    fun onLevelComplete() {
        val nextLevel = currentLevelId + 1
        val highest = SharedPrefs.getHighestUnlockedLevel(this)

        // Save progress if this is a new record
        if (nextLevel > highest) {
            SharedPrefs.setHighestUnlockedLevel(this, nextLevel)
        }

        // Check if there are more levels (Updated to 10)
        if (nextLevel <= 10) {
            currentLevelId = nextLevel
            gameView.setLevel(nextLevel)
        } else {
            // Game Over / All Levels Complete -> Return to Menu
            finish()
        }
    }
}