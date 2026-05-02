package com.callvault.app.ui.components.neo

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.composed
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.NeoElevation
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect

/**
 * Dual-shadow draw modifier that powers every neumorphic surface.
 *
 * For [NeoElevation.Convex] the modifier paints:
 * - a light highlight offset to the **top-left** (virtual light source)
 * - a dark shadow offset to the **bottom-right**
 *
 * For [NeoElevation.Concave] both shadows are inverted and clipped inside the
 * shape, producing a pressed-into-surface effect.
 *
 * For [NeoElevation.Flat] the modifier is a no-op.
 *
 * The actual blur is implemented with [BlurMaskFilter] on a software-layer
 * [android.graphics.Paint] for predictable rendering across API 26+.
 *
 * @param elevation visual depth token
 * @param shape outline of the surface — only [RoundedCornerShape] is supported
 *              for now (every Neo* component uses rounded corners by spec)
 * @param lightColor light highlight tint, defaults to [NeoColors.Light]
 * @param darkColor dark shadow tint, defaults to [NeoColors.Dark]
 */
fun Modifier.neoShadow(
    elevation: NeoElevation,
    shape: Shape = RoundedCornerShape(16.dp),
    lightColor: Color = NeoColors.Light,
    darkColor: Color = NeoColors.Dark
): Modifier = composed {
    val density = LocalDensity.current
    if (elevation is NeoElevation.Flat) return@composed this

    drawBehind {
        val cornerPx = resolveCornerRadiusPx(shape, size, density, layoutDirection)
        val offsetPx = with(density) { elevation.offset.toPx() }
        val blurPx = with(density) { elevation.blur.toPx() }.coerceAtLeast(0.5f)

        when (elevation) {
            is NeoElevation.Convex -> {
                // Light highlight: top-left
                drawNeoShadow(
                    color = lightColor,
                    dx = -offsetPx,
                    dy = -offsetPx,
                    blur = blurPx,
                    cornerPx = cornerPx,
                    inset = false
                )
                // Dark shadow: bottom-right
                drawNeoShadow(
                    color = darkColor,
                    dx = offsetPx,
                    dy = offsetPx,
                    blur = blurPx,
                    cornerPx = cornerPx,
                    inset = false
                )
            }
            is NeoElevation.Concave -> {
                drawNeoShadow(
                    color = darkColor,
                    dx = offsetPx,
                    dy = offsetPx,
                    blur = blurPx,
                    cornerPx = cornerPx,
                    inset = true
                )
                drawNeoShadow(
                    color = lightColor,
                    dx = -offsetPx,
                    dy = -offsetPx,
                    blur = blurPx,
                    cornerPx = cornerPx,
                    inset = true
                )
            }
            NeoElevation.Flat -> Unit
        }
    }
}

private fun resolveCornerRadiusPx(
    shape: Shape,
    size: Size,
    density: Density,
    layoutDirection: LayoutDirection
): Float {
    if (shape is CornerBasedShape) {
        val outline = shape.createOutline(size, layoutDirection, density)
        // RoundedCornerShape always produces an Outline.Rounded
        if (outline is androidx.compose.ui.graphics.Outline.Rounded) {
            val rr = outline.roundRect
            return maxOf(rr.topLeftCornerRadius.x, rr.topRightCornerRadius.x)
        }
    }
    return with(density) { 16.dp.toPx() }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNeoShadow(
    color: Color,
    dx: Float,
    dy: Float,
    blur: Float,
    cornerPx: Float,
    inset: Boolean
) {
    drawIntoCanvas { canvas ->
        val frameworkPaint = Paint().apply {
            isAntiAlias = true
            this.color = color.toArgb()
            maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
        }
        val rect = RectF(0f, 0f, size.width, size.height)
        if (inset) {
            // Clip to the shape so the inset shadow stays inside the surface.
            canvas.save()
            canvas.nativeCanvas.clipPath(roundedPath(rect, cornerPx))
            // Draw a slightly larger rounded rect offset in the requested direction
            // and stroked with the blurred paint so the edge bleeds inward.
            val strokePaint = Paint(frameworkPaint).apply {
                style = Paint.Style.STROKE
                strokeWidth = blur * 2f
            }
            val r = RectF(
                rect.left + dx,
                rect.top + dy,
                rect.right + dx,
                rect.bottom + dy
            )
            canvas.nativeCanvas.drawRoundRect(r, cornerPx, cornerPx, strokePaint)
            canvas.restore()
        } else {
            val r = RectF(
                rect.left + dx,
                rect.top + dy,
                rect.right + dx,
                rect.bottom + dy
            )
            canvas.nativeCanvas.drawRoundRect(r, cornerPx, cornerPx, frameworkPaint)
        }
    }
}

private fun roundedPath(rect: RectF, corner: Float): android.graphics.Path =
    android.graphics.Path().apply {
        addRoundRect(rect, corner, corner, android.graphics.Path.Direction.CW)
    }

/**
 * Convenience: tiny no-op so call-sites can treat [Rect] / [CornerRadius]
 * imports as "used" when re-exported from this file in future iterations.
 */
@Suppress("unused")
private val keepImports: Pair<Rect, CornerRadius> =
    Rect(0f, 0f, 0f, 0f) to CornerRadius(0f)
