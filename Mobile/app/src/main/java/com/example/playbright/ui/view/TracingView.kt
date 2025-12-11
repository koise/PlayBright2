package com.example.playbright.ui.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.playbright.R
import com.example.playbright.data.model.DrawingPoint

/**
 * Custom View for tracing activities
 * Allows users to draw over a template image and compares with reference drawing
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

    private val referencePaint = Paint().apply {
        color = Color.parseColor("#CC000000") // Semi-transparent black
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f) // Dashed line
    }

    private val drawingPath = Path()
    private val drawingPaths = mutableListOf<Path>()
    private val drawingPoints = mutableListOf<PointF>() // Store all drawing points
    private var isDrawing = false
    private var startX = 0f
    private var startY = 0f

    private var templateBitmap: Bitmap? = null
    private var templateRect: RectF? = null
    private var referencePoints: List<DrawingPoint>? = null
    private var scaleX = 1f
    private var scaleY = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    var onDrawingStarted: (() -> Unit)? = null
    var onDrawingCompleted: (() -> Unit)? = null
    var onSimilarityCalculated: ((Int) -> Unit)? = null // Callback for similarity percentage

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateScaleAndOffset()
        invalidate()
    }

    fun setTemplateBitmap(bitmap: Bitmap?) {
        templateBitmap = bitmap
        templateRect = bitmap?.let {
            RectF(0f, 0f, it.width.toFloat(), it.height.toFloat())
        }
        calculateScaleAndOffset()
        invalidate()
    }

    fun setReferencePoints(points: List<DrawingPoint>?) {
        referencePoints = points
        calculateScaleAndOffset()
        invalidate()
    }

    private fun calculateScaleAndOffset() {
        templateRect?.let { rect ->
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
            offsetX = (viewWidth - scaledWidth) / 2
            offsetY = (viewHeight - scaledHeight) / 2
            scaleX = scale
            scaleY = scale
        } ?: run {
            // If no template, use view dimensions for reference points
            // Assume reference points are in 400x400 coordinate space (from web canvas)
            val viewWidth = width.toFloat()
            val viewHeight = height.toFloat()
            val refWidth = 400f
            val refHeight = 400f

            val scale = minOf(
                viewWidth / refWidth,
                viewHeight / refHeight
            )

            val scaledWidth = refWidth * scale
            val scaledHeight = refHeight * scale
            offsetX = (viewWidth - scaledWidth) / 2
            offsetY = (viewHeight - scaledHeight) / 2
            scaleX = scale
            scaleY = scale
        }
    }

    fun clearDrawing() {
        drawingPaths.clear()
        drawingPath.reset()
        drawingPoints.clear()
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

        // Draw reference drawing points if available (as dashed line)
        referencePoints?.let { points ->
            if (points.isNotEmpty()) {
                // Recalculate scale if needed (in case view size changed)
                if (scaleX == 1f && scaleY == 1f && offsetX == 0f && offsetY == 0f) {
                    calculateScaleAndOffset()
                }
                
                val referencePath = Path()
                val firstPoint = points[0]
                val scaledX = (firstPoint.x * scaleX) + offsetX
                val scaledY = (firstPoint.y * scaleY) + offsetY
                referencePath.moveTo(scaledX, scaledY)
                
                for (i in 1 until points.size) {
                    val point = points[i]
                    val x = (point.x * scaleX) + offsetX
                    val y = (point.y * scaleY) + offsetY
                    referencePath.lineTo(x, y)
                }
                canvas.drawPath(referencePath, referencePaint)
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
                drawingPoints.add(PointF(startX, startY))
                isDrawing = true
                onDrawingStarted?.invoke()
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDrawing) {
                    drawingPath.lineTo(event.x, event.y)
                    drawingPoints.add(PointF(event.x, event.y))
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
                    
                    // Calculate similarity if reference points are available
                    calculateSimilarity()
                    invalidate()
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun calculateSimilarity() {
        val reference = referencePoints ?: return
        if (reference.isEmpty() || drawingPoints.isEmpty()) {
            onSimilarityCalculated?.invoke(0)
            return
        }

        // Normalize both drawings to same coordinate space
        val normalizedReference = normalizePoints(reference)
        val normalizedStudent = normalizePoints(drawingPoints.map { DrawingPoint(it.x, it.y) })

        // Calculate similarity using point distance
        val similarity = compareDrawings(normalizedReference, normalizedStudent)
        onSimilarityCalculated?.invoke(similarity)
    }

    private fun normalizePoints(points: List<DrawingPoint>): List<PointF> {
        if (points.isEmpty()) return emptyList()

        val minX = points.minOfOrNull { it.x } ?: 0f
        val maxX = points.maxOfOrNull { it.x } ?: 1f
        val minY = points.minOfOrNull { it.y } ?: 0f
        val maxY = points.maxOfOrNull { it.y } ?: 1f

        val width = (maxX - minX).coerceAtLeast(1f)
        val height = (maxY - minY).coerceAtLeast(1f)
        val scale = maxOf(width, height)

        return points.map { point ->
            PointF(
                (point.x - minX) / scale,
                (point.y - minY) / scale
            )
        }
    }

    private fun compareDrawings(ref: List<PointF>, student: List<PointF>): Int {
        if (ref.isEmpty() || student.isEmpty()) return 0

        // Resample both to same number of points
        val targetPoints = 50
        val resampledRef = resamplePoints(ref, targetPoints)
        val resampledStudent = resamplePoints(student, targetPoints)

        // Calculate average distance
        var totalDistance = 0f
        for (i in resampledRef.indices) {
            val dx = resampledRef[i].x - resampledStudent[i].x
            val dy = resampledRef[i].y - resampledStudent[i].y
            totalDistance += kotlin.math.sqrt(dx * dx + dy * dy)
        }

        val avgDistance = totalDistance / targetPoints
        // Convert distance to similarity percentage (0-100)
        val similarity = ((1f - avgDistance.coerceIn(0f, 1f)) * 100f).toInt()
        return similarity.coerceIn(0, 100)
    }

    private fun resamplePoints(points: List<PointF>, n: Int): List<PointF> {
        if (points.isEmpty() || n <= 0) return emptyList()
        if (points.size <= n) return points

        // Calculate total path length
        var totalLength = 0f
        for (i in 1 until points.size) {
            val dx = points[i].x - points[i - 1].x
            val dy = points[i].y - points[i - 1].y
            totalLength += kotlin.math.sqrt(dx * dx + dy * dy)
        }

        if (totalLength == 0f) return points.take(n)

        val segmentLength = totalLength / (n - 1)
        val resampled = mutableListOf<PointF>()
        resampled.add(points[0])

        var currentLength = 0f
        var targetLength = segmentLength

        for (i in 1 until points.size) {
            val dx = points[i].x - points[i - 1].x
            val dy = points[i].y - points[i - 1].y
            val segDist = kotlin.math.sqrt(dx * dx + dy * dy)
            currentLength += segDist

            while (currentLength >= targetLength && resampled.size < n) {
                val ratio = (targetLength - (currentLength - segDist)) / segDist.coerceAtLeast(0.001f)
                val newPoint = PointF(
                    points[i - 1].x + ratio * dx,
                    points[i - 1].y + ratio * dy
                )
                resampled.add(newPoint)
                targetLength += segmentLength
            }
        }

        // Ensure we have exactly n points
        while (resampled.size < n) {
            resampled.add(points.last())
        }

        return resampled.take(n)
    }

    fun getDrawingPaths(): List<Path> {
        return drawingPaths.toList()
    }

    fun getDrawingPoints(): List<PointF> {
        return drawingPoints.toList()
    }

    fun hasDrawing(): Boolean {
        return drawingPaths.isNotEmpty() || isDrawing
    }
}


