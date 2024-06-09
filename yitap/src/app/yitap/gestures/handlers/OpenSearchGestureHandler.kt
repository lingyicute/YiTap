package app.yitap.gestures.handlers

import android.content.Context
import app.yitap.YitapLauncher
import app.yitap.preferences2.PreferenceManager2
import app.yitap.qsb.LawnQsbLayout

class OpenSearchGestureHandler(context: Context) : GestureHandler(context) {

    override suspend fun onTrigger(launcher: YitapLauncher) {
        val prefs = PreferenceManager2.getInstance(launcher)
        val searchProvider = LawnQsbLayout.getSearchProvider(launcher, prefs)
        searchProvider.launch(launcher)
    }
}
