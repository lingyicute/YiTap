package app.yitap.theme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.PatternMatcher
import androidx.core.graphics.ColorUtils
import app.yitap.preferences2.PreferenceManager2
import app.yitap.theme.color.AndroidColor
import app.yitap.theme.color.ColorOption
import app.yitap.theme.color.ColorStyle
import app.yitap.theme.color.MonetColorSchemeCompat
import app.yitap.theme.color.SystemColorScheme
import app.yitap.ui.theme.getSystemAccent
import app.yitap.wallpaper.WallpaperManagerCompat
import com.android.launcher3.Utilities
import com.android.launcher3.util.MainThreadInitializedObject
import com.android.systemui.monet.Style
import com.patrykmichalik.opto.core.firstBlocking
import com.patrykmichalik.opto.core.onEach
import dev.kdrag0n.colorkt.Color
import dev.kdrag0n.colorkt.conversion.ConversionGraph.convert
import dev.kdrag0n.colorkt.rgb.Srgb
import dev.kdrag0n.monet.theme.ColorScheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class ThemeProvider(private val context: Context) {
    private val preferenceManager2 = PreferenceManager2.getInstance(context)
    private val wallpaperManager = WallpaperManagerCompat.INSTANCE.get(context)
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private var accentColor: ColorOption = preferenceManager2.accentColor.firstBlocking()
    private var colorStyle: ColorStyle = preferenceManager2.colorStyle.firstBlocking()

    private val colorSchemeMap = HashMap<Pair<Int, Style>, ColorScheme>()
    private val listeners = mutableListOf<ColorSchemeChangeListener>()

    init {
        if (Utilities.ATLEAST_S) {
            colorSchemeMap[Pair(0, Style.TONAL_SPOT)] = SystemColorScheme(context)
            registerOverlayChangedListener()
        }
        wallpaperManager.addOnChangeListener(object : WallpaperManagerCompat.OnColorsChangedListener {
            override fun onColorsChanged() {
                if (accentColor is ColorOption.WallpaperPrimary) {
                    notifyColorSchemeChanged()
                }
            }
        })
        preferenceManager2.accentColor.onEach(launchIn = coroutineScope) {
            accentColor = it
            notifyColorSchemeChanged()
        }
        preferenceManager2.colorStyle.onEach(launchIn = coroutineScope) {
            colorStyle = it
            notifyColorSchemeChanged()
        }
    }

    private fun registerOverlayChangedListener() {
        val packageFilter = IntentFilter("android.intent.action.OVERLAY_CHANGED")
        packageFilter.addDataScheme("package")
        packageFilter.addDataSchemeSpecificPart("android", PatternMatcher.PATTERN_LITERAL)
        context.registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    colorSchemeMap[Pair(0, Style.TONAL_SPOT)] = SystemColorScheme(context)
                    if (accentColor is ColorOption.SystemAccent) {
                        notifyColorSchemeChanged()
                    }
                }
            },
            packageFilter,
            null,
            Handler(Looper.getMainLooper()),
        )
    }

    val colorScheme get() = when (val accentColor = this.accentColor) {
        is ColorOption.SystemAccent -> systemColorScheme
        is ColorOption.WallpaperPrimary -> {
            val wallpaperPrimary = wallpaperManager.wallpaperColors?.primaryColor
            getColorScheme(wallpaperPrimary ?: ColorOption.YitapBlue.color, colorStyle.style)
        }
        is ColorOption.CustomColor -> getColorScheme(accentColor.color, colorStyle.style)
        else -> getColorScheme(ColorOption.YitapBlue.color, colorStyle.style)
    }

    private val systemColorScheme get() = when {
        Utilities.ATLEAST_S -> getColorScheme(0, colorStyle.style)
        else -> getColorScheme(context.getSystemAccent(darkTheme = false), colorStyle.style)
    }

    private fun getColorScheme(
        colorInt: Int,
        colorStyle: Style,
    ): ColorScheme {
        val key = Pair(colorInt, colorStyle)
        var colorScheme = colorSchemeMap[key]
        if (colorScheme == null) {
            colorScheme = MonetColorSchemeCompat(colorInt, colorStyle)
            colorSchemeMap[key] = colorScheme
        }
        return colorScheme
    }

    fun addListener(listener: ColorSchemeChangeListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: ColorSchemeChangeListener) {
        listeners.remove(listener)
    }

    private fun notifyColorSchemeChanged() {
        ArrayList(listeners)
            .forEach(ColorSchemeChangeListener::onColorSchemeChanged)
    }

    companion object {
        @JvmField
        val INSTANCE = MainThreadInitializedObject(::ThemeProvider)
    }

    sealed interface ColorSchemeChangeListener {
        fun onColorSchemeChanged()
    }
}

fun Color.toAndroidColor(): Int {
    return when (this) {
        is AndroidColor -> color
        is Srgb -> ColorUtils.setAlphaComponent(toRgb8(), 255)
        else -> convert<Srgb>().toAndroidColor()
    }
}
