package app.yitap.qsb.providers

import app.yitap.animateToAllApps
import app.yitap.preferences.PreferenceManager
import app.yitap.qsb.ThemingMethod
import com.android.launcher3.Launcher
import com.android.launcher3.R

data object Startpage : QsbSearchProvider(
    id = "startpage",
    name = R.string.search_provider_startpage,
    icon = R.drawable.ic_startpage,
    themingMethod = ThemingMethod.TINT,
    packageName = "",
    website = "https://startpage.com/?segment=startpage.yitap",
    type = QsbSearchProviderType.LOCAL,
    sponsored = true,
) {
    override suspend fun launch(launcher: Launcher, forceWebsite: Boolean) {
        val prefs = PreferenceManager.getInstance(launcher)
        val useWebSuggestions = prefs.searchResultStartPageSuggestion.get()

        if (useWebSuggestions) {
            launcher.animateToAllApps()
            launcher.appsView.searchUiManager.editText?.showKeyboard(true)
        } else {
            super.launch(launcher, forceWebsite)
        }
    }
}
