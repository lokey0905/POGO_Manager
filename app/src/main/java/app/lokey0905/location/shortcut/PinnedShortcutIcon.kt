package app.lokey0905.location.shortcut

import android.content.Context
import android.content.pm.ShortcutManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Icon
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.color.MaterialColors

/**
 * Renders the app's monochrome shortcut drawables before passing them to the launcher.
 * Some MIUI launchers draw resource vectors without bounds, producing a blank icon.
 * An opaque themed bitmap avoids that path and does not depend on the launcher's theme.
 *
 * Invalid resources throw [Resources.NotFoundException] rather than pinning a blank icon.
 */
fun createPinnedShortcutIcon(
    context: Context,
    @DrawableRes iconResource: Int,
    shortcutManager: ShortcutManager
): Icon {
    val drawable = AppCompatResources.getDrawable(context, iconResource)?.mutate()
        ?: throw Resources.NotFoundException("Shortcut icon resource $iconResource was not found")
    val geometry = shortcutIconGeometry(
        shortcutManager.iconMaxWidth,
        shortcutManager.iconMaxHeight,
        drawable.intrinsicWidth,
        drawable.intrinsicHeight
    )
    val background = MaterialColors.getColor(
        context,
        com.google.android.material.R.attr.colorSurface,
        "PinnedShortcutIcon"
    )
    val foreground = MaterialColors.getColor(
        context,
        com.google.android.material.R.attr.colorOnSurface,
        "PinnedShortcutIcon"
    )
    val bitmap = Bitmap.createBitmap(geometry.size, geometry.size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(ColorUtils.setAlphaComponent(background, 255))
    DrawableCompat.setTint(drawable, ColorUtils.setAlphaComponent(foreground, 255))
    drawable.alpha = 255
    drawable.setBounds(geometry.left, geometry.top, geometry.right, geometry.bottom)
    drawable.draw(canvas)

    return Icon.createWithBitmap(bitmap)
}
