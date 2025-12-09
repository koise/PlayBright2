package com.example.playbright.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.playbright.R

class LineDrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.primary_blue)
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    private val correctLinePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.success_green)
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    private val currentPath = Path()
    private val drawnLines = mutableListOf<Line>()
    private var isDrawing = false
    private var startX = 0f
    private var startY = 0f
    private var endX = 0f
    private var endY = 0f

    data class Line(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val isCorrect: Boolean = false
    )

    var onLineDrawn: ((Float, Float, Float, Float) -> Unit)? = null
    var onTouchStart: ((Float, Float) -> Int)? = null // Returns card index or -1
    var onTouchEnd: ((Float, Float) -> Int)? = null // Returns card index or -1

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw all completed lines
        drawnLines.forEach { line ->
            val paint = if (line.isCorrect) correctLinePaint else linePaint
            canvas.drawLine(line.startX, line.startY, line.endX, line.endY, paint)
        }

        // Draw current line being drawn
        if (isDrawing) {
            canvas.drawPath(currentPath, linePaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val cardIndex = onTouchStart?.invoke(event.x, event.y) ?: -1
                if (cardIndex >= 0) {
                    startX = event.x
                    startY = event.y
                    currentPath.reset()
                    currentPath.moveTo(startX, startY)
                    isDrawing = true
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDrawing) {
                    currentPath.lineTo(event.x, event.y)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isDrawing) {
                    endX = event.x
                    endY = event.y
                    val endCardIndex = onTouchEnd?.invoke(event.x, event.y) ?: -1
                    isDrawing = false
                    
                    // Notify listener with actual coordinates
                    onLineDrawn?.invoke(startX, startY, endX, endY)
                    
                    currentPath.reset()
                    invalidate()
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                isDrawing = false
                currentPath.reset()
                invalidate()
            }
        }
        return super.onTouchEvent(event)
    }

    fun addLine(startX: Float, startY: Float, endX: Float, endY: Float, isCorrect: Boolean = false) {
        drawnLines.add(Line(startX, startY, endX, endY, isCorrect))
        invalidate()
    }

    fun clearLines() {
        drawnLines.clear()
        currentPath.reset()
        isDrawing = false
        invalidate()
    }

    fun markLineAsCorrect(startX: Float, startY: Float, endX: Float, endY: Float) {
        val lineIndex = drawnLines.indexOfFirst { 
            Math.abs(it.startX - startX) < 10 && 
            Math.abs(it.startY - startY) < 10 &&
            Math.abs(it.endX - endX) < 10 && 
            Math.abs(it.endY - endY) < 10
        }
        if (lineIndex >= 0) {
            val line = drawnLines[lineIndex]
            drawnLines[lineIndex] = line.copy(isCorrect = true)
            invalidate()
        }
    }

    fun removeLine(startX: Float, startY: Float, endX: Float, endY: Float) {
        drawnLines.removeAll { 
            Math.abs(it.startX - startX) < 10 && 
            Math.abs(it.startY - startY) < 10 &&
            Math.abs(it.endX - endX) < 10 && 
            Math.abs(it.endY - endY) < 10
        }
        invalidate()
    }
}

