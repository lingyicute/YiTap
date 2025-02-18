package app.yitap.qsb.providers

import com.android.launcher3.R

data object Wikipedia : QsbSearchProvider(
    id = "wikipedia",
    name = R.string.search_provider_wikipedia,
    icon = R.drawable.ic_wikipedia,
    packageName = "org.wikipedia",
    className = "org.wikipedia.search.SearchActivity",
    website = "https://wikipedia.com/",
)
