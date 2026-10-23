package app.chipos.launcher.data

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.util.concurrent.ConcurrentHashMap

/** Artwork for an app tile. Banners (16:9) fill the tile; plain icons are centered. */
data class TileArt(
    val image: ImageBitmap,
    val isBanner: Boolean,
    /** Tile background (top and bottom of a gradient), tinted from the icon's own colors. */
    val backdropTop: Color = NeutralTop,
    val backdropBottom: Color = NeutralBottom,
)

private val NeutralTop = Color(0xFF2A2A2D)
private val NeutralBottom = Color(0xFF161618)

object IconRepository {
    private const val BANNER_WIDTH = 320
    private const val BANNER_HEIGHT = 180
    private const val ICON_SIZE = 192

    private val cache = ConcurrentHashMap<String, TileArt>()

    fun cached(packageName: String): TileArt? = cache[packageName]

    /** Blocking; call from a background dispatcher. */
    fun load(context: Context, entry: AppEntry): TileArt? {
        cache[entry.packageName]?.let { return it }

        val pm = context.packageManager
        val component = ComponentName(entry.packageName, entry.activityName)

        val banner: Drawable? = try {
            pm.getActivityBanner(component) ?: pm.getApplicationBanner(entry.packageName)
        } catch (e: Exception) {
            null
        }

        val art = if (banner != null) {
            TileArt(banner.toBitmap(BANNER_WIDTH, BANNER_HEIGHT).asImageBitmap(), isBanner = true)
        } else {
            val icon: Drawable = try {
                pm.getActivityIcon(component)
            } catch (e: Exception) {
                return null
            }
            val bitmap = icon.toBitmap(ICON_SIZE, ICON_SIZE)
            val (top, bottom) = backdropFor(bitmap)
            TileArt(bitmap.asImageBitmap(), isBanner = false, backdropTop = top, backdropBottom = bottom)
        }

        cache[entry.packageName] = art
        return art
    }

    /**
     * A dark gradient in the icon's average color, so icon-only tiles are not a flat grey box.
     * Near-grey icons get a neutral dark gradient.
     */
    private fun backdropFor(bitmap: Bitmap): Pair<Color, Color> {
        val small = Bitmap.createScaledBitmap(bitmap, 16, 16, true)
        var red = 0f
        var green = 0f
        var blue = 0f
        var weight = 0f
        for (y in 0 until 16) {
            for (x in 0 until 16) {
                val pixel = small.getPixel(x, y)
                val alpha = (pixel ushr 24) / 255f
                if (alpha < 0.1f) continue
                red += ((pixel shr 16) and 0xFF) * alpha
                green += ((pixel shr 8) and 0xFF) * alpha
                blue += (pixel and 0xFF) * alpha
                weight += alpha
            }
        }
        if (weight < 1f) return NeutralTop to NeutralBottom

        val hsv = FloatArray(3)
        AndroidColor.RGBToHSV((red / weight).toInt(), (green / weight).toInt(), (blue / weight).toInt(), hsv)
        if (hsv[1] < 0.12f) return NeutralTop to NeutralBottom

        val saturation = hsv[1].coerceAtMost(0.7f)
        val top = Color(AndroidColor.HSVToColor(floatArrayOf(hsv[0], saturation, 0.42f)))
        val bottom = Color(AndroidColor.HSVToColor(floatArrayOf(hsv[0], saturation, 0.20f)))
        return top to bottom
    }
}
