package app.chipos.launcher.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.roundToInt

/**
 * A small, heavily blurred copy of the current wallpaper (with the dark scrim already baked in),
 * covering the whole screen. Glass surfaces draw the part of it that lies behind them, which is
 * what makes them look frosted: Android 9 and 10 cannot blur what is behind a view, but the
 * wallpaper is the only thing behind the glass, so it can be blurred ahead of time.
 */
object GlassBackdrop {
    /** The blurred wallpaper, or null until the first one is ready. */
    var image by mutableStateOf<ImageBitmap?>(null)
        private set

    /** The previous one, kept while the wallpaper cross-fades. */
    var previous by mutableStateOf<ImageBitmap?>(null)
        private set

    /** 0 = fully [previous], 1 = fully [image]. */
    var mix by mutableFloatStateOf(1f)
        private set

    /** Size in pixels of the screen that [image] was made for. */
    var screenWidth = 1920f
        private set
    var screenHeight = 1080f
        private set

    fun setScreen(width: Int, height: Int) {
        screenWidth = width.toFloat()
        screenHeight = height.toFloat()
    }

    /** Fades from the current backdrop to [next], over the same time as the wallpaper cross-fade. */
    suspend fun crossfadeTo(next: ImageBitmap) {
        if (image == null) {
            image = next
            mix = 1f
            return
        }
        previous = image
        image = next
        mix = 0f
        animate(0f, 1f, animationSpec = tween(2000)) { value, _ -> mix = value }
        previous = null
    }

    /**
     * Blocking; call from a background dispatcher. Crops [source] the way ContentScale.Crop does,
     * averages it down to a tiny image, blurs it and darkens it with the same scrim the screen has.
     */
    fun build(source: ImageBitmap, screenWidth: Int, screenHeight: Int): ImageBitmap? {
        return try {
            val src = source.asAndroidBitmap()
            val sw = src.width
            val sh = src.height
            if (sw < 2 || sh < 2 || screenWidth < 2 || screenHeight < 2) return null

            val pixels = IntArray(sw * sh)
            src.getPixels(pixels, 0, sw, 0, 0, sw, sh)

            val scale = maxOf(screenWidth.toFloat() / sw, screenHeight.toFloat() / sh)
            val cropW = screenWidth / scale
            val cropH = screenHeight / scale
            val left = (sw - cropW) / 2f
            val top = (sh - cropH) / 2f

            val tw = 96
            val th = (tw * screenHeight / screenWidth.toFloat()).roundToInt().coerceAtLeast(2)
            val red = FloatArray(tw * th)
            val green = FloatArray(tw * th)
            val blue = FloatArray(tw * th)

            for (ty in 0 until th) {
                val y0 = (top + ty * cropH / th).toInt().coerceIn(0, sh - 1)
                val y1 = (top + (ty + 1) * cropH / th).toInt().coerceIn(y0 + 1, sh)
                val yStep = maxOf(1, (y1 - y0) / 6)
                for (tx in 0 until tw) {
                    val x0 = (left + tx * cropW / tw).toInt().coerceIn(0, sw - 1)
                    val x1 = (left + (tx + 1) * cropW / tw).toInt().coerceIn(x0 + 1, sw)
                    val xStep = maxOf(1, (x1 - x0) / 6)
                    var sumR = 0
                    var sumG = 0
                    var sumB = 0
                    var count = 0
                    var y = y0
                    while (y < y1) {
                        var x = x0
                        while (x < x1) {
                            val c = pixels[y * sw + x]
                            sumR += (c shr 16) and 0xFF
                            sumG += (c shr 8) and 0xFF
                            sumB += c and 0xFF
                            count++
                            x += xStep
                        }
                        y += yStep
                    }
                    val i = ty * tw + tx
                    red[i] = sumR / count.toFloat()
                    green[i] = sumG / count.toFloat()
                    blue[i] = sumB / count.toFloat()
                }
            }

            // Two passes of a box blur look like a Gaussian. At this size, 3 pixels is about 60 on screen.
            for (channel in listOf(red, green, blue)) {
                blur(channel, tw, th, 3)
                blur(channel, tw, th, 3)
            }

            val out = IntArray(tw * th)
            for (ty in 0 until th) {
                // The same scrim WallpaperLayer draws: 25% black at the top, none in the middle, 60% at the bottom.
                val t = ty / (th - 1f)
                val scrim = if (t < 0.5f) 0.25f * (1f - t / 0.5f) else 0.60f * ((t - 0.5f) / 0.5f)
                val k = 1f - scrim
                for (tx in 0 until tw) {
                    val i = ty * tw + tx
                    val r = (red[i] * k).toInt().coerceIn(0, 255)
                    val g = (green[i] * k).toInt().coerceIn(0, 255)
                    val b = (blue[i] * k).toInt().coerceIn(0, 255)
                    out[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
            Bitmap.createBitmap(out, tw, th, Bitmap.Config.ARGB_8888).asImageBitmap()
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    /** Box blur of one channel, in place. Edges repeat. */
    private fun blur(a: FloatArray, w: Int, h: Int, radius: Int) {
        val tmp = FloatArray(a.size)
        val span = (2 * radius + 1).toFloat()
        for (y in 0 until h) {
            for (x in 0 until w) {
                var sum = 0f
                for (k in -radius..radius) sum += a[y * w + (x + k).coerceIn(0, w - 1)]
                tmp[y * w + x] = sum / span
            }
        }
        for (y in 0 until h) {
            for (x in 0 until w) {
                var sum = 0f
                for (k in -radius..radius) sum += tmp[(y + k).coerceIn(0, h - 1) * w + x]
                a[y * w + x] = sum / span
            }
        }
    }
}
