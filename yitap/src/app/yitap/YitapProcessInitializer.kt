package app.yitap

import android.content.Context
import androidx.annotation.Keep
import androidx.arch.core.util.Function
import app.yitap.bugreport.YitapBugReporter
import app.yitap.theme.color.tokens.ColorTokens
import com.android.launcher3.Utilities
import com.android.launcher3.icons.ThemedIconDrawable
import com.android.quickstep.QuickstepProcessInitializer

@Keep
class YitapProcessInitializer(context: Context) : QuickstepProcessInitializer(context) {

    override fun init(context: Context) {
        YitapBugReporter.INSTANCE.get(context)
        ThemedIconDrawable.COLORS_LOADER = Function {
            if (Utilities.isDarkTheme(it)) {
                intArrayOf(
                    ColorTokens.Accent2_800.resolveColor(it),
                    ColorTokens.Accent1_200.resolveColor(it),
                )
            } else {
                intArrayOf(
                    ColorTokens.Accent1_100.resolveColor(it),
                    ColorTokens.Accent1_700.resolveColor(it),
                )
            }
        }
        super.init(context)
    }
}
