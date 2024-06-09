package app.yitap.allapps.views

import android.content.Context
import android.util.AttributeSet
import app.yitap.search.YitapSearchUiDelegate
import com.android.launcher3.allapps.LauncherAllAppsContainerView

class SearchContainerView(context: Context?, attrs: AttributeSet?) :
    LauncherAllAppsContainerView(context, attrs) {

    override fun createSearchUiDelegate() = YitapSearchUiDelegate(this)
}
