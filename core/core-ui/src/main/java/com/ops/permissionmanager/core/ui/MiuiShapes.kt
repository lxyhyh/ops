package com.ops.permissionmanager.core.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.min

object MiuiShapes {

    private const val CURVATURE = 0.9f

    fun squircle(radius: Dp): Shape = SquircleShape(radius)

    fun squircle(radius: Dp, size: Dp): Shape =
        SquircleShape(if (radius + radius > size) size / 2 else radius)

    private data class SquircleShape(private val radius: Dp) : Shape {

        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density
        ): Outline {
            val r = (radius * density.density).value.coerceAtMost(min(size.width, size.height) / 2f)
            if (r <= 0f) {
                return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
            }

            val c = r * (1f - CURVATURE)
            val w = size.width
            val h = size.height

            val path = Path().apply {
                moveTo(r, 0f)
                lineTo(w - r, 0f)
                cubicTo(w - c, 0f, w, c, w, r)
                lineTo(w, h - r)
                cubicTo(w, h - c, w - c, h, w - r, h)
                lineTo(r, h)
                cubicTo(c, h, 0f, h - c, 0f, h - r)
                lineTo(0f, r)
                cubicTo(0f, c, c, 0f, r, 0f)
                close()
            }

            return Outline.Generic(path)
        }
    }
}