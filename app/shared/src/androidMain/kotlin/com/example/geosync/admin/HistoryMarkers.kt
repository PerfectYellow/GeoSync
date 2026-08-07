package com.example.geosync.admin

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import com.example.geosync.admin.HistoryMapConfig

enum class HistoryMarkerStyle {
    CLASSIC_DOT,
    MODERN_PIN,
    SLEEK_RING
}

object HistoryMarkerFactory {

    fun createMarker(
        context: Context,
        isStart: Boolean,
        color: Int
    ): BitmapDrawable {
        return when (HistoryMapConfig.MARKER_STYLE) {
            HistoryMarkerStyle.CLASSIC_DOT -> createClassicDot(context, isStart, color)
            HistoryMarkerStyle.MODERN_PIN -> createModernPin(context, isStart, color)
            HistoryMarkerStyle.SLEEK_RING -> createSleekRing(context, isStart, color)
        }
    }

    private fun createClassicDot(context: Context, isStart: Boolean, color: Int): BitmapDrawable {
        val density = context.resources.displayMetrics.density
        val size = (if (isStart) 14f else 18f) * density
        val bitmap = Bitmap.createBitmap(size.toInt(), size.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Outer white border
        paint.color = Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Inner color
        paint.color = color
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f * density, paint)

        return BitmapDrawable(context.resources, bitmap)
    }

    private fun createModernPin(context: Context, isStart: Boolean, color: Int): BitmapDrawable {
        val density = context.resources.displayMetrics.density
        
        // We make the End marker physically different in height and offset to handle overlaps
        val baseSize = 28f * density
        val width = baseSize.toInt()
        val height = (baseSize * 1.5f).toInt()
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val centerX = width / 2f
        val centerY = baseSize / 2f + (if (isStart) baseSize * 0.3f else 0f) // Vertical offset for overlap visibility

        // 1. Draw Shadow
        paint.color = Color.BLACK
        paint.alpha = 30
        val fHeight = height.toFloat()
        canvas.drawOval(centerX - 6f*density, fHeight - 4f*density, centerX + 6f*density, fHeight, paint)

        // 2. Draw Pin Body
        val path = Path()
        val radius = (if (isStart) 10f else 13f) * density
        
        // Draw the circular head
        path.addCircle(centerX, centerY, radius, Path.Direction.CW)
        
        // Draw the pointed tip reaching to the bottom
        val tipPath = Path()
        tipPath.moveTo(centerX - radius * 0.7f, centerY + radius * 0.6f)
        tipPath.lineTo(centerX, height - 2f * density)
        tipPath.lineTo(centerX + radius * 0.7f, centerY + radius * 0.6f)
        tipPath.close()
        path.op(tipPath, Path.Op.UNION)

        paint.color = color
        paint.alpha = 255
        canvas.drawPath(path, paint)

        // 3. Inner White Detail (Symbolic)
        paint.color = Color.WHITE
        if (isStart) {
            // "Play" triangle for start
            val sPath = Path()
            val s = radius * 0.4f
            sPath.moveTo(centerX - s * 0.5f, centerY - s)
            sPath.lineTo(centerX + s, centerY)
            sPath.lineTo(centerX - s * 0.5f, centerY + s)
            sPath.close()
            canvas.drawPath(sPath, paint)
        } else {
            // "Stop" square for end
            val s = radius * 0.35f
            canvas.drawRect(centerX - s, centerY - s, centerX + s, centerY + s, paint)
        }

        return BitmapDrawable(context.resources, bitmap)
    }

    private fun createSleekRing(context: Context, isStart: Boolean, color: Int): BitmapDrawable {
        val density = context.resources.displayMetrics.density
        val size = 32f * density
        val bitmap = Bitmap.createBitmap(size.toInt(), size.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val center = size / 2f

        // Outer glow
        paint.color = color
        paint.alpha = 60
        canvas.drawCircle(center, center, center, paint)

        // Main ring
        paint.alpha = 255
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f * density
        canvas.drawCircle(center, center, center - 6f * density, paint)

        // Center dot
        paint.style = Paint.Style.FILL
        canvas.drawCircle(center, center, center - 12f * density, paint)
        
        // Label initial
        paint.color = Color.WHITE
        paint.textSize = 10f * density
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD
        val label = if (isStart) "S" else "E"
        val bounds = Rect()
        paint.getTextBounds(label, 0, 1, bounds)
        canvas.drawText(label, center, center + bounds.height() / 2f, paint)

        return BitmapDrawable(context.resources, bitmap)
    }
}
