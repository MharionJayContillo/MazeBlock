package com.example.mazeblock.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.mazeblock.GameActivity
import kotlin.math.abs

class GameView(context: Context, startLevel: Int) : SurfaceView(context), SurfaceHolder.Callback, GestureDetector.OnGestureListener {

    private val thread: GameThread
    private val gestureDetector: GestureDetector

    // Game Objects
    private var map: Array<IntArray> = emptyArray()
    private var playerRow = 1
    private var playerCol = 1
    private var cellSize = 0f
    private var xOffset = 0f
    private var yOffset = 0f

    // State
    private var isLevelComplete = false
    private var transitionTimer = 0
    private var levelStartTime = 0L
    private var currentTime = 0L
    private var levelId = startLevel

    // Paints
    private val wallPaint = Paint().apply { color = Color.DKGRAY }
    private val floorPaint = Paint().apply { color = Color.LTGRAY }
    private val goalPaint = Paint().apply { color = Color.GREEN }
    private val playerPaint = Paint().apply { color = Color.RED }

    // Text Paint for In-Game UI
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 60f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    init {
        holder.addCallback(this)
        thread = GameThread(holder, this)
        gestureDetector = GestureDetector(context, this)
        loadLevelData(levelId)
    }

    fun setLevel(newLevel: Int) {
        levelId = newLevel
        loadLevelData(levelId)
        isLevelComplete = false
        transitionTimer = 0
    }

    private fun loadLevelData(level: Int) {
        // Reset timer when level loads
        levelStartTime = System.currentTimeMillis()
        currentTime = 0L

        // Load the specific level map using the helper function
        map = getLevel(level)

        // Find the player start position (optional logic, but for now defaults to 1,1)
        // If your levels have specific start points, you can scan for them here.
        // Defaulting to 1,1 based on your arrays:
        playerRow = 1
        playerCol = 1

        // Force recalculate layout since map size changed
        if (width > 0 && height > 0) {
            calculateDimensions(width, height)
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        resume()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        calculateDimensions(width, height)
    }

    private fun calculateDimensions(width: Int, height: Int) {
        if (map.isEmpty()) return

        val rows = map.size
        val cols = map[0].size
        val cellW = width.toFloat() / cols
        val cellH = height.toFloat() / rows

        // Use 95% of available space to keep it large
        cellSize = minOf(cellW, cellH) * 0.95f

        // Center the board
        xOffset = (width - (cols * cellSize)) / 2
        yOffset = (height - (rows * cellSize)) / 2
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        pause()
    }

    fun update() {
        if (isLevelComplete) {
            // Wait about 3 seconds (90 frames) before going to next level
            transitionTimer++
            if (transitionTimer > 90) {
                (context as? GameActivity)?.runOnUiThread {
                    (context as GameActivity).onLevelComplete()
                }
                transitionTimer = 0
                isLevelComplete = false
            }
        } else {
            // Count up timer (milliseconds)
            currentTime = System.currentTimeMillis() - levelStartTime
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        canvas.drawColor(Color.WHITE)

        val seconds = currentTime / 1000

        // Always draw the running timer at the top
        textPaint.color = Color.BLACK
        textPaint.textSize = 60f
        canvas.drawText("Time: ${seconds}s  Level: $levelId", width / 2f, 100f, textPaint)

        // Draw Map
        for (r in map.indices) {
            for (c in map[r].indices) {
                val tile = map[r][c]
                val left = xOffset + c * cellSize
                val top = yOffset + r * cellSize
                val right = left + cellSize
                val bottom = top + cellSize

                when (tile) {
                    1 -> canvas.drawRect(left, top, right, bottom, wallPaint)
                    2 -> canvas.drawRect(left, top, right, bottom, goalPaint)
                    0 -> canvas.drawRect(left, top, right, bottom, floorPaint)
                }
            }
        }

        // Draw Player
        val pLeft = xOffset + playerCol * cellSize + 5
        val pTop = yOffset + playerRow * cellSize + 5
        val pRight = pLeft + cellSize - 5
        val pBottom = pTop + cellSize - 5
        canvas.drawRect(pLeft, pTop, pRight, pBottom, playerPaint)

        // Draw VICTORY SCREEN
        if (isLevelComplete) {
            // Dim background
            canvas.drawColor(Color.argb(200, 0, 0, 0))

            // Draw "LEVEL COMPLETE!"
            textPaint.color = Color.GREEN
            textPaint.textSize = 80f
            canvas.drawText("LEVEL COMPLETE!", width / 2f, height / 2f - 60f, textPaint)

            // Draw Final Time below it
            textPaint.color = Color.WHITE
            textPaint.textSize = 70f
            canvas.drawText("Time: ${seconds}s", width / 2f, height / 2f + 60f, textPaint)
        }
    }

    fun pause() {
        thread.setRunning(false)
        try {
            thread.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun resume() {
        thread.setRunning(true)
        if (thread.state == Thread.State.TERMINATED) {
            // In proper app, recreate thread logic would go here
        } else if (thread.state == Thread.State.NEW) {
            thread.start()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    override fun onDown(e: MotionEvent): Boolean = true
    override fun onShowPress(e: MotionEvent) {}
    override fun onSingleTapUp(e: MotionEvent): Boolean = false
    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distOrX: Float, distOrY: Float): Boolean = false
    override fun onLongPress(e: MotionEvent) {}

    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        if (isLevelComplete) return true

        val dx = e2.x - (e1?.x ?: 0f)
        val dy = e2.y - (e1?.y ?: 0f)

        if (abs(dx) > abs(dy)) {
            if (abs(dx) > 100 && abs(velocityX) > 100) {
                if (dx > 0) movePlayer(0, 1) // Right
                else movePlayer(0, -1) // Left
            }
        } else {
            if (abs(dy) > 100 && abs(velocityY) > 100) {
                if (dy > 0) movePlayer(1, 0) // Down
                else movePlayer(-1, 0) // Up
            }
        }
        return true
    }

    private fun movePlayer(dRow: Int, dCol: Int) {
        var tempRow = playerRow
        var tempCol = playerCol

        while (true) {
            val nextRow = tempRow + dRow
            val nextCol = tempCol + dCol

            if (nextRow < 0 || nextRow >= map.size || nextCol < 0 || nextCol >= map[0].size) break

            if (map[nextRow][nextCol] == 1) break

            tempRow = nextRow
            tempCol = nextCol

            if (map[tempRow][tempCol] == 2) {
                isLevelComplete = true
                break
            }
        }

        playerRow = tempRow
        playerCol = tempCol
    }

    // --- LEVEL DATA & HELPER FUNCTIONS ---

    private fun getLevel(level: Int): Array<IntArray> {
        return when (level) {
            1 -> level1
            2 -> level2
            3 -> level3
            4 -> level4
            5 -> level5
            6 -> level6
            7 -> level7
            8 -> level8
            9 -> level9
            10 -> level10
            else -> level1
        }
    }

    // --- LEVEL 1 (10x10) ---
    private val level1 = arrayOf(
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 0, 0, 0, 1, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 1, 0, 1, 0, 1, 1, 0, 1),
        intArrayOf(1, 0, 1, 0, 0, 0, 0, 1, 0, 1),
        intArrayOf(1, 0, 1, 1, 1, 1, 0, 1, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 1, 0, 0, 0, 1),
        intArrayOf(1, 1, 1, 1, 0, 1, 1, 1, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 2, 1),
        intArrayOf(1, 0, 1, 1, 1, 1, 0, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
    )

    // --- LEVEL 2 (12x12) ---
    private val level2 = arrayOf(
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1),
        intArrayOf(1, 0, 1, 1, 0, 1, 0, 1, 0, 0, 0, 1),
        intArrayOf(1, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1),
        intArrayOf(1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1),
        intArrayOf(1, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1),
        intArrayOf(1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
    )

    // --- LEVEL 3 (14x14) ---
    private val level3 = arrayOf(
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,0,0,0,0,1,0,0,0,0,0,0,0,1),
        intArrayOf(1,1,1,1,0,1,0,1,1,1,1,1,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,1,1,1,1,1,1,1,1,1,1,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,1,1,1,1,1,1,0,1,1,1,1,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,1,1,1,1,1,1,0,1,1,1,1,1),
        intArrayOf(1,0,0,0,1,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,1,1,0,1,0,1,1,1,1,1,1,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,2,1),
        intArrayOf(1,0,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1)
    )

    // --- LEVEL 4 (16x16) ---
    private val level4 = arrayOf(
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,0,0,0,0,0,0,1,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,1,1,1,1,0,1,0,1,1,1,1,1,0,1),
        intArrayOf(1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,1,0,1,1,1,1,0,1,1,1,1,1,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,1,1,1,1,1,1,1,0,1,1,1,1,1,1,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,1,1,1,1,1,1,0,1,1,1,1,1,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,1,1,1,1,1,0,1,1,1,1,1,1,1,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,1,1,1,1,1,1,1,1,1,1,1,1,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,1,0,0,0,0,0,2,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,0,1,1,1,1,1,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1)
    )

    // --- LEVEL 5 (18x18) ---
    private val level5 = arrayOf(
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,1,1,1,1,1,0,1,0,1,0,1,1,1,1,0,1),
        intArrayOf(1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,1,0,1),
        intArrayOf(1,0,1,0,1,1,1,1,1,1,1,1,1,1,0,1,0,1),
        intArrayOf(1,0,1,0,1,0,0,0,0,0,0,0,0,0,0,1,0,1),
        intArrayOf(1,0,1,0,1,0,1,1,1,0,1,1,1,1,0,1,0,1),
        intArrayOf(1,0,0,0,0,0,1,0,0,0,0,0,0,1,0,0,0,1),
        intArrayOf(1,1,1,1,1,0,1,0,1,2,1,1,0,1,0,1,1,1), // Goal in center
        intArrayOf(1,0,0,0,0,0,0,0,1,0,0,1,0,0,0,0,0,1),
        intArrayOf(1,1,1,1,1,0,1,0,1,1,0,1,0,1,0,1,1,1),
        intArrayOf(1,0,0,0,0,0,1,0,0,0,0,1,0,1,0,0,0,1),
        intArrayOf(1,0,1,0,1,0,1,1,1,0,1,1,0,1,0,1,0,1),
        intArrayOf(1,0,1,0,1,0,0,0,1,0,0,0,0,0,0,1,0,1),
        intArrayOf(1,0,1,0,1,1,1,1,1,0,1,1,1,1,1,1,0,1),
        intArrayOf(1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1)
    )

    // --- LEVEL 6 (20x20) ---
    private val level6 = arrayOf(
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,0,0,0,1,0,0,0,1,0,0,0,0,0,1,0,0,0,1,1),
        intArrayOf(1,0,1,1,1,0,1,1,1,0,1,1,1,1,1,0,1,1,1,1),
        intArrayOf(1,0,1,0,0,0,1,0,0,0,1,0,0,0,0,0,1,0,0,1),
        intArrayOf(1,0,1,0,1,1,1,0,1,1,1,0,1,1,1,0,1,1,0,1),
        intArrayOf(1,0,0,0,1,0,0,0,1,0,0,0,1,0,0,0,0,0,0,1),
        intArrayOf(1,1,1,0,1,0,1,1,1,0,1,1,1,0,1,1,1,1,1,1),
        intArrayOf(1,0,0,0,1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,1,1,1,1,1,1,1,0,1,0,1,1,1,1,1,1,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,1,0,1,0,0,0,0,1,0,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,0,1,0,1,0,1,1,0,1,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,1,0,0,0,1,0,1,0,0,1,0,1),
        intArrayOf(1,0,1,1,1,1,1,0,1,1,1,1,1,0,1,0,1,1,0,1),
        intArrayOf(1,0,0,0,0,0,1,0,0,0,0,0,0,0,1,0,1,0,0,1),
        intArrayOf(1,1,1,1,1,0,1,1,1,1,1,1,1,1,1,0,1,0,1,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1)
    )

    // --- LEVEL 7 (22x22) ---
    private val level7 = arrayOf(
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,1,1,1,1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,1,0,1,1,1,1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,1),
        intArrayOf(1,0,1,1,1,1,1,1,1,1,1,1,1,0,1,1,1,1,1,1,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,1,0,1,1,1,1,1,1,1,0,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,1,1,1,1,1,1,1,1,0,1,1,1,1,1,1,1,1,1,0,1),
        intArrayOf(1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,1,0,1,1,1,1,1,1,0,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,1,1,1,1,1,1,1,0,1,1,1,1,1,1,1,1,1,0,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,1,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,2,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1)
    )

    // --- LEVEL 8 (24x24) ---
    private val level8 = arrayOf(
        intArrayOf(1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1),
        intArrayOf(1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 1),
        intArrayOf(1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1),
        intArrayOf(1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1),
        intArrayOf(1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 1),
        intArrayOf(1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1),
        intArrayOf(1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1),
        intArrayOf(1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 1),
        intArrayOf(1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1),
        intArrayOf(1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 1),
        intArrayOf(1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1, 1),
        intArrayOf(1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 1),
        intArrayOf(1, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1),
        intArrayOf(1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1),
        intArrayOf(1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1),
        intArrayOf(1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1),
        intArrayOf(1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1)
    )

    // --- LEVEL 9 (26x26) ---
    private val level9 = arrayOf(
        intArrayOf(1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1),
        intArrayOf(1, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1),
        intArrayOf(1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 1, 1),
        intArrayOf(1, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1),
        intArrayOf(1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1),
        intArrayOf(1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1),
        intArrayOf(1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1),
        intArrayOf(1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1),
        intArrayOf(1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1),
        intArrayOf(1, 0, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1, 1),
        intArrayOf(1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1),
        intArrayOf(1, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1),
        intArrayOf(1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1),
        intArrayOf(1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1, 1),
        intArrayOf(1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1),
        intArrayOf(1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
    )

    // --- LEVEL 10 (28x28)
    private val level10 = arrayOf(
        intArrayOf(1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1),
        intArrayOf(1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1),
        intArrayOf(1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1),
        intArrayOf(1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1),
        intArrayOf(1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1),
        intArrayOf(1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1),
        intArrayOf(1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 1),
        intArrayOf(1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 1),
        intArrayOf(1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1),
        intArrayOf(1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1),
        intArrayOf(1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1),
        intArrayOf(1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 1),
        intArrayOf(1, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1),
        intArrayOf(1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1),
        intArrayOf(1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1),
        intArrayOf(1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 1),
        intArrayOf(1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1),
        intArrayOf(1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1),
        intArrayOf(1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1)
    )
}