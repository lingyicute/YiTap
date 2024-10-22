package app.yitap.allapps

import android.content.Context
import android.util.Log
import androidx.lifecycle.lifecycleScope
import app.yitap.launcher
import app.yitap.preferences2.PreferenceManager2
import com.android.launcher3.allapps.AllAppsStore
import com.android.launcher3.allapps.AlphabeticalAppsList
import com.android.launcher3.allapps.WorkProfileManager
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.views.ActivityContext
import com.patrykmichalik.opto.core.onEach
import java.util.function.Predicate

class YitapAlphabeticalAppsList<T>(
    context: T,
    appsStore: AllAppsStore<T>,
    workProfileManager: WorkProfileManager?,
) : AlphabeticalAppsList<T>(context, appsStore, workProfileManager)
    where T : Context, T : ActivityContext {

    private var hiddenApps: Set<String> = setOf()
    private val prefs = PreferenceManager2.getInstance(context)

    init {
        try {
            prefs.hiddenApps.onEach(launchIn = context.launcher.lifecycleScope) {
                hiddenApps = it
                onAppsUpdated()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed initialize ignore: ", t)
        }
    }

    override fun updateItemFilter(itemFilter: Predicate<ItemInfo>?) {
        this.mItemFilter = Predicate { info ->
            require(info is AppInfo) { "`info` must be an instance of `AppInfo`." }
            val componentKey = info.toComponentKey().toString()
            itemFilter?.test(info) != false && !hiddenApps.contains(componentKey)
        }
        onAppsUpdated()
    }
}
