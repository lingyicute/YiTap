package app.yitap.qsb.providers

import app.yitap.qsb.ThemingMethod
import com.android.launcher3.R

data object Brave : QsbSearchProvider(
    id = "brave",
    name = R.string.search_provider_brave,
    icon = R.drawable.ic_brave,
    themedIcon = R.drawable.ic_brave_tinted,
    themingMethod = ThemingMethod.TINT,
    packageName = "com.brave.browser",
    className = "org.chromium.chrome.browser.searchwidget.SearchWidgetProviderActivity",
    website = "https://search.brave.com/",
)
