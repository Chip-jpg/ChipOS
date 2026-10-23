package app.chipos.launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * Two sources of wallpapers:
 *  - built in: image files in app/src/main/assets/wallpapers/ (ship with the app);
 *  - your own: image files in the app's external folder,
 *    Android/data/app.chipos.launcher/files/wallpapers/ on the stick. scripts\push-wallpapers.bat
 *    copies a folder of photos there. No storage permission is needed.
 *
 * Names are plain file names for built-in ones, and "file:<full path>" for your own.
 */
object WallpaperSource {
    private const val DIR = "wallpapers"
    private const val FILE_PREFIX = "file:"
    private val extensions = listOf(".jpg", ".jpeg", ".png", ".webp")

    private fun isImage(name: String) = extensions.any { name.lowercase().endsWith(it) }

    private fun builtIn(context: Context): List<String> =
        (context.assets.list(DIR) ?: emptyArray()).filter { isImage(it) }.sorted()

    /** Full paths of the user's own photos. */
    fun userFiles(context: Context): List<String> =
        context.getExternalFilesDir(DIR)
            ?.listFiles()
            ?.filter { it.isFile && isImage(it.name) }
            ?.sortedBy { it.name.lowercase() }
            ?.map { FILE_PREFIX + it.absolutePath }
            ?: emptyList()

    /** [mode]: 0 = built-in, 1 = your own (falls back to built-in if there are none), 2 = both. */
    fun list(context: Context, mode: Int): List<String> {
        val builtIn = builtIn(context)
        val mine = userFiles(context)
        return when (mode) {
            1 -> mine.ifEmpty { builtIn }
            2 -> builtIn + mine
            else -> builtIn
        }
    }

    /** Blocking; call from a background dispatcher. Decodes at roughly the screen size. */
    fun decode(context: Context, name: String, targetWidth: Int, targetHeight: Int): ImageBitmap? {
        val open: () -> InputStream = if (name.startsWith(FILE_PREFIX)) {
            { FileInputStream(File(name.removePrefix(FILE_PREFIX))) }
        } else {
            { context.assets.open("$DIR/$name") }
        }
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            open().use { BitmapFactory.decodeStream(it, null, bounds) }

            var sample = 1
            while (bounds.outWidth / (sample * 2) >= targetWidth &&
                bounds.outHeight / (sample * 2) >= targetHeight
            ) {
                sample *= 2
            }

            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            open().use { BitmapFactory.decodeStream(it, null, options) }?.asImageBitmap()
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }
}
