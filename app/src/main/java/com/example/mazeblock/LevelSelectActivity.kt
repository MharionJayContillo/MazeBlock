package com.example.mazeblock

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.GridLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.mazeblock.game.SharedPrefs

class LevelSelectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_level_select)
    }

    override fun onResume() {
        super.onResume()
        setupGrid()
    }

    private fun setupGrid() {
        val gridLayout = findViewById<GridLayout>(R.id.gridLevels)
        gridLayout.removeAllViews()

        val highestLevel = SharedPrefs.getHighestLevel(this)
        val totalLevels = 10

        for (i in 1..totalLevels) {
            val btn = Button(this)
            val params = GridLayout.LayoutParams().apply {
                width = 300
                height = 300
                setMargins(24, 24, 24, 24)
                setGravity(Gravity.CENTER)
            }
            btn.layoutParams = params
            btn.text = "Level $i"
            btn.textSize = 18f
            btn.setTextColor(ContextCompat.getColor(this, R.color.dark_charcoal))

            if (i <= highestLevel) {
                btn.background = ContextCompat.getDrawable(this, R.drawable.btn_unlocked)
                btn.isEnabled = true
                btn.alpha = 1.0f
                btn.setOnClickListener {
                    startGame(i)
                }
            } else {
                btn.background = ContextCompat.getDrawable(this, R.drawable.btn_locked)
                btn.isEnabled = false
                btn.alpha = 0.5f
            }

            gridLayout.addView(btn)
        }
    }

    private fun startGame(level: Int) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("LEVEL_ID", level)
        startActivity(intent)
    }
}