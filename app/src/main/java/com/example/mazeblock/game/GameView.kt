package com.example.mazeblock.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.content.ContextCompat
import com.example.mazeblock.R

class GameView(context: Context, private val map: Array<IntArray>, private val onWin: () -> Unit) :
    SurfaceView(context), SurfaceHolder.Callback {

    private var thread: GameThread? = null

    // Colors
    private val paintWall = Paint().apply { color = ContextCompat.getColor(context, R.color.pastel_azure) }
    private val paintFloor = Paint().apply { color = ContextCompat.getColor(context, R.color.white) }
    private val paintGoal = Paint().apply { color = ContextCompat.getColor(context, R.color.pastel_lavender) }
    private val paintPlayer = Paint().apply { color = ContextCompat.getColor(context, R.color.soft_pink) }
    private val paintGrid = Paint().apply {
        color = ContextCompat.getColor(context, R.color.pastel_mint)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    // Dimensions
    private var cellSize = 0f
    private var offsetX = 0f
    private var offsetY = 0f

    // Game State
    private var playerX = 1
    private var playerY = 1
    private var isMoving = false
    private var targetX = 1
    private var targetY = 1
    private var moveSpeed = 0.4f // Speed of sliding animation
    private var animX = 1f
    private var animY = 1f
    private var won = false

    // Input
    private var touchX = 0f
    private var touchY = 0f
    private val swipeThreshold = 50f

    init {
        holder.addCallback(this)
        isFocusable = true
        // Set initial animation pos
        animX = playerX.toFloat()
        animY = playerY.toFloat()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        thread = GameThread(holder, this)
        thread?.running = true
        thread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Auto-scaling logic
        val mapHeight = map.size
        val mapWidth = map[0].size

        val cellW = width.toFloat() / mapWidth
        val cellH = height.toFloat() / mapHeight

        cellSize = minOf(cellW, cellH) * 0.9f

        val totalW = cellSize * mapWidth
        val totalH = cellSize * mapHeight

        offsetX = (width - totalW) / 2
        offsetY = (height - totalH) / 2
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        var retry = true
        while (retry) {
            try {
                thread?.running = false
                thread?.join()
                retry = false
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    fun update() {
        if (won) return

        if (isMoving) {
            val dx = targetX - animX
            val dy = targetY - animY

            if (Math.abs(dx) < moveSpeed && Math.abs(dy) < moveSpeed) {
                // Arrived
                playerX = targetX
                playerY = targetY
                animX = playerX.toFloat()
                animY = playerY.toFloat()
                isMoving = false

                // Check win condition
                if (map[playerY][playerX] == 2) {
                    won = true
                    post { onWin() }
                }
            } else {
                // Move towards target
                animX += Math.signum(dx) * moveSpeed
                animY += Math.signum(dy) * moveSpeed
            }
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (canvas == null) return

        canvas.drawColor(ContextCompat.getColor(context, R.color.white))

        val rows = map.size
        val cols = map[0].size

        for (y in 0 until rows) {
            for (x in 0 until cols) {
                val left = offsetX + x * cellSize
                val top = offsetY + y * cellSize
                val right = left + cellSize
                val bottom = top + cellSize

                val type = map[y][x]

                // Draw Floor
                canvas.drawRect(left, top, right, bottom, paintFloor)

                // Draw Element
                when (type) {
                    1 -> canvas.drawRect(left, top, right, bottom, paintWall)
                    2 -> {
                        canvas.drawRect(left, top, right, bottom, paintFloor)
                        // Draw goal as a smaller inner rect or circle
                        val inset = cellSize * 0.2f
                        canvas.drawOval(left + inset, top + inset, right - inset, bottom - inset, paintGoal)
                    }
                }

                // Draw Grid Lines
                canvas.drawRect(left, top, right, bottom, paintGrid)
            }
        }

        // Draw Player
        val pLeft = offsetX + animX * cellSize
        val pTop = offsetY + animY * cellSize
        val pRight = pLeft + cellSize
        val pBottom = pTop + cellSize
        val pInset = cellSize * 0.1f

        val rectF = RectF(pLeft + pInset, pTop + pInset, pRight - pInset, pBottom - pInset)
        canvas.drawRoundRect(rectF, cellSize/4, cellSize/4, paintPlayer)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isMoving || won) return true

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchX = event.x
                touchY = event.y
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - touchX
                val dy = event.y - touchY

                if (Math.abs(dx) > Math.abs(dy)) {
                    // Horizontal
                    if (Math.abs(dx) > swipeThreshold) {
                        if (dx > 0) tryMove(1, 0) else tryMove(-1, 0)
                    }
                } else {
                    // Vertical
                    if (Math.abs(dy) > swipeThreshold) {
                        if (dy > 0) tryMove(0, 1) else tryMove(0, -1)
                    }
                }
            }
        }
        return true
    }

    private fun tryMove(dx: Int, dy: Int) {
        var tempX = playerX
        var tempY = playerY

        val rows = map.size
        val cols = map[0].size

        // Slide until hit wall
        while (true) {
            val nextX = tempX + dx
            val nextY = tempY + dy

            // Boundary Check
            if (nextX < 0 || nextX >= cols || nextY < 0 || nextY >= rows) break
            // Wall Check
            if (map[nextY][nextX] == 1) break

            tempX = nextX
            tempY = nextY
            if (map[tempY][tempX] == 2) break
        }

        if (tempX != playerX || tempY != playerY) {
            targetX = tempX
            targetY = tempY
            isMoving = true
        }
    }
}