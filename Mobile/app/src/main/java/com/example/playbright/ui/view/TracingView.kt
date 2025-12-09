package com.example.playbright.ui.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.playbright.R

/**
 * Custom View for tracing activities
 * Allows users to draw over a template image
 */
class TracingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val drawingPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.primary_blue)
        strokeWidth = 12f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    private val drawingPath = Path()
    private val drawingPaths = mutableListOf<Path>()
    private var isDrawing = false
    private var startX = 0f
    private var startY = 0f

    private var templateBitmap: Bitmap? = null
    private var templateRect: RectF? = null

    var onDrawingStarted: (() -> Unit)? = null
    var onDrawingCompleted: (() -> Unit)? = null

    fun setTemplateBitmap(bitmap: Bitmap?) {
        templateBitmap = bitmap
        templateRect = bitmap?.let {
            RectF(0f, 0f, it.width.toFloat(), it.height.toFloat())
        }
        invalidate()
    }

    fun clearDrawing() {
        drawingPaths.clear()
        drawingPath.reset()
        isDrawing = false
        invalidate()
    }

    fun undoLastStroke() {
        if (drawingPaths.isNotEmpty()) {
            drawingPaths.removeAt(drawingPaths.size - 1)
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw template image if available
        templateBitmap?.let { bitmap ->
            templateRect?.let { rect ->
                // Scale bitmap to fit view while maintaining aspect ratio
                val viewWidth = width.toFloat()
                val viewHeight = height.toFloat()
                val bitmapWidth = rect.width()
                val bitmapHeight = rect.height()

                val scale = minOf(
                    viewWidth / bitmapWidth,
                    viewHeight / bitmapHeight
                )

                val scaledWidth = bitmapWidth * scale
                val scaledHeight = bitmapHeight * scale
                val left = (viewWidth - scaledWidth) / 2
                val top = (viewHeight - scaledHeight) / 2

                val destRect = RectF(left, top, left + scaledWidth, top + scaledHeight)

                // Draw template with some transparency
                val templatePaint = Paint().apply {
                    alpha = 180 // Semi-transparent
                }
                canvas.drawBitmap(bitmap, null, destRect, templatePaint)
            }
        }

        // Draw all completed paths
        drawingPaths.forEach { path ->
            canvas.drawPath(path, drawingPaint)
        }

        // Draw current path being drawn
        if (isDrawing) {
            canvas.drawPath(drawingPath, drawingPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                drawingPath.reset()
                drawingPath.moveTo(startX, startY)
                isDrawing = true
                onDrawingStarted?.invoke()
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDrawing) {
                    drawingPath.lineTo(event.x, event.y)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDrawing) {
                    // Save the completed path
                    val completedPath = Path(drawingPath)
                    drawingPaths.add(completedPath)
                    drawingPath.reset()
                    isDrawing = false
                    onDrawingCompleted?.invoke()
                    invalidate()
                }
            }
        }
        return super.onTouchEvent(event)
    }

    fun getDrawingPaths(): List<Path> {
        return drawingPaths.toList()
    }

    fun hasDrawing(): Boolean {
        return drawingPaths.isNotEmpty() || isDrawing
    }
}

