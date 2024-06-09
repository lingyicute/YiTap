package app.yitap.gestures.handlers

import android.content.Context
import app.yitap.YitapLauncher
import app.yitap.animateToAllApps

open class OpenAppDrawerGestureHandler(context: Context) : GestureHandler(context) {

    override suspend fun onTrigger(launcher: YitapLauncher) {
        launcher.animateToAllApps()
    }
}
