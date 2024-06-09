package app.yitap.qsb.providers

import android.content.Intent
import app.yitap.qsb.ThemingMethod
import com.android.launcher3.R

data object Youtube : QsbSearchProvider(
    id = "youtube",
    name = R.string.search_provider_youtube,
    icon = R.drawable.ic_youtube,
    themingMethod = ThemingMethod.THEME_BY_LAYER_ID,
    packageName = "com.google.android.youtube",
    action = Intent.ACTION_SEARCH,
    supportVoiceIntent = false,
    website = "https://youtube.com/",
)
