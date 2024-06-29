package app.yitap.search.algorithms

import android.content.Context
import app.yitap.YitapApp
import app.yitap.allapps.views.SearchItemBackground
import app.yitap.allapps.views.SearchResultView.Companion.EXTRA_QUICK_LAUNCH
import app.yitap.preferences.PreferenceManager
import app.yitap.preferences2.PreferenceManager2
import app.yitap.search.YitapSearchAdapterProvider
import app.yitap.search.adapter.SearchAdapterItem
import app.yitap.search.adapter.SearchTargetCompat
import app.yitap.search.adapter.SearchTargetCompat.Companion.RESULT_TYPE_APPLICATION
import app.yitap.search.adapter.SearchTargetCompat.Companion.RESULT_TYPE_SHORTCUT
import com.android.app.search.LayoutType.CALCULATOR
import com.android.app.search.LayoutType.EMPTY_DIVIDER
import com.android.app.search.LayoutType.HORIZONTAL_MEDIUM_TEXT
import com.android.app.search.LayoutType.ICON_HORIZONTAL_TEXT
import com.android.app.search.LayoutType.ICON_SINGLE_VERTICAL_TEXT
import com.android.app.search.LayoutType.ICON_SLICE
import com.android.app.search.LayoutType.PEOPLE_TILE
import com.android.app.search.LayoutType.SMALL_ICON_HORIZONTAL_TEXT
import com.android.app.search.LayoutType.TEXT_HEADER
import com.android.app.search.LayoutType.THUMBNAIL
import com.android.app.search.LayoutType.WIDGET_LIVE
import com.android.launcher3.BuildConfig
import com.android.launcher3.Utilities
import com.android.launcher3.allapps.BaseAllAppsAdapter
import com.android.launcher3.search.SearchAlgorithm
import com.patrykmichalik.opto.core.firstBlocking

sealed class YitapSearchAlgorithm(
    protected val context: Context,
) : SearchAlgorithm<BaseAllAppsAdapter.AdapterItem> {

    private val iconBackground = SearchItemBackground(
        context,
        showBackground = false,
        roundTop = true,
        roundBottom = true,
    )
    private val normalBackground = SearchItemBackground(
        context,
        showBackground = true,
        roundTop = true,
        roundBottom = true,
    )
    private val topBackground = SearchItemBackground(
        context,
        showBackground = true,
        roundTop = true,
        roundBottom = false,
    )
    private val centerBackground = SearchItemBackground(
        context,
        showBackground = true,
        roundTop = false,
        roundBottom = false,
    )
    private val bottomBackground = SearchItemBackground(
        context,
        showBackground = true,
        roundTop = false,
        roundBottom = true,
    )

    protected fun transformSearchResults(results: List<SearchTargetCompat>): List<SearchAdapterItem> {
        val filtered = results
            .asSequence()
            .filter { it.packageName != BuildConfig.APPLICATION_ID }
            .filter { YitapSearchAdapterProvider.viewTypeMap[it.layoutType] != null }
            .removeDuplicateDividers()
            .toList()

        val smallIconIndices = findIndices(filtered, SMALL_ICON_HORIZONTAL_TEXT)
        val iconRowIndices = findIndices(filtered, ICON_HORIZONTAL_TEXT)
        val peopleTileIndices = findIndices(filtered, PEOPLE_TILE)
        val suggestionIndices = findIndices(filtered, HORIZONTAL_MEDIUM_TEXT)
        val fileIndices = findIndices(filtered, THUMBNAIL)
        val settingIndices = findIndices(filtered, ICON_SLICE)
        val recentIndices = findIndices(filtered, WIDGET_LIVE)
        val calculator = findIndices(filtered, CALCULATOR)

        return filtered.mapIndexedNotNull { index, target ->
            val isFirst = index == 0 || filtered[index - 1].isDivider
            val isLast = index == filtered.lastIndex || filtered[index + 1].isDivider

            if (target.extras.getBoolean(EXTRA_QUICK_LAUNCH, false)) {
                SearchAdapterItem.createAdapterItem(target, normalBackground)
            } else {
                val background = getBackground(
                    target.layoutType,
                    index,
                    isFirst,
                    isLast,
                    smallIconIndices,
                    iconRowIndices,
                    peopleTileIndices,
                    suggestionIndices,
                    fileIndices,
                    settingIndices,
                    recentIndices,
                    calculator,
                )
                SearchAdapterItem.createAdapterItem(target, background)
            }
        }
    }

    protected fun setFirstItemQuickLaunch(searchTargets: List<SearchTargetCompat>) {
        val hasQuickLaunch = searchTargets.any { it.extras.getBoolean(EXTRA_QUICK_LAUNCH, false) }
        if (!hasQuickLaunch) {
            searchTargets.firstOrNull()?.extras?.apply {
                putBoolean(EXTRA_QUICK_LAUNCH, true)
            }
        }
    }

    private fun findIndices(filtered: List<SearchTargetCompat>, layoutType: String): List<Int> {
        return filtered.indices.filter {
            filtered[it].layoutType == layoutType
        }
    }

    private fun getBackground(
        layoutType: String,
        index: Int,
        isFirst: Boolean,
        isLast: Boolean,
        smallIconIndices: List<Int>,
        iconRowIndices: List<Int>,
        peopleTileIndices: List<Int>,
        suggestionIndices: List<Int>,
        fileIndices: List<Int>,
        settingIndices: List<Int>,
        recentIndices: List<Int>,
        calculator: List<Int>,
    ): SearchItemBackground = when {
        layoutType == TEXT_HEADER || layoutType == ICON_SINGLE_VERTICAL_TEXT || layoutType == EMPTY_DIVIDER -> iconBackground
        layoutType == SMALL_ICON_HORIZONTAL_TEXT -> getGroupedBackground(index, smallIconIndices)
        layoutType == ICON_HORIZONTAL_TEXT -> getGroupedBackground(index, iconRowIndices)
        layoutType == PEOPLE_TILE -> getGroupedBackground(index, peopleTileIndices)
        layoutType == HORIZONTAL_MEDIUM_TEXT -> getGroupedBackground(index, suggestionIndices)
        layoutType == THUMBNAIL -> getGroupedBackground(index, fileIndices)
        layoutType == ICON_SLICE -> getGroupedBackground(index, settingIndices)
        layoutType == WIDGET_LIVE -> getGroupedBackground(index, recentIndices)
        layoutType == CALCULATOR && calculator.isNotEmpty() -> normalBackground
        isFirst && isLast -> normalBackground
        isFirst -> topBackground
        isLast -> bottomBackground
        else -> centerBackground
    }

    private fun getGroupedBackground(index: Int, indices: List<Int>): SearchItemBackground = when {
        indices.size == 1 -> normalBackground
        index == indices.first() -> topBackground
        index == indices.last() -> bottomBackground
        else -> centerBackground
    }

    companion object {

        const val APP_SEARCH = "appSearch"
        const val LOCAL_SEARCH = "localSearch"
        const val ASI_SEARCH = "globalSearch"

        private var ranCompatibilityCheck = false

        fun isASISearchEnabled(context: Context): Boolean {
            if (!Utilities.ATLEAST_S) return false
            if (!YitapApp.isRecentsEnabled) return false

            val prefs = PreferenceManager.getInstance(context)
            if (!ranCompatibilityCheck) {
                ranCompatibilityCheck = true
                YitapASISearchAlgorithm.checkSearchCompatibility(context)
            }
            return prefs.deviceSearch.get()
        }

        fun create(context: Context): YitapSearchAlgorithm {
            val prefs = PreferenceManager2.getInstance(context)
            val searchAlgorithm = prefs.searchAlgorithm.firstBlocking()

            return when {
                searchAlgorithm == ASI_SEARCH && isASISearchEnabled(context) -> YitapASISearchAlgorithm(context)
                searchAlgorithm == LOCAL_SEARCH -> YitapLocalSearchAlgorithm(context)
                else -> YitapAppSearchAlgorithm(context)
            }
        }
    }
}
private fun Sequence<SearchTargetCompat>.removeDuplicateDividers(): Sequence<SearchTargetCompat> {
    var previousWasDivider = true
    return filter { item ->
        val isDivider = item.layoutType == EMPTY_DIVIDER
        val remove = isDivider && previousWasDivider
        previousWasDivider = isDivider
        !remove
    }
}

private val SearchTargetCompat.isApp get() = resultType == RESULT_TYPE_APPLICATION
private val SearchTargetCompat.isShortcut get() = resultType == RESULT_TYPE_SHORTCUT
private val SearchTargetCompat.isDivider get() = layoutType == EMPTY_DIVIDER
