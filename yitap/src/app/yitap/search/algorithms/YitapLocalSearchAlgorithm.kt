package app.yitap.search.algorithms

import android.content.Context
import android.os.Handler
import app.yitap.preferences.PreferenceManager
import app.yitap.preferences2.PreferenceManager2
import app.yitap.search.adapter.CALCULATOR
import app.yitap.search.adapter.CONTACT
import app.yitap.search.adapter.ERROR
import app.yitap.search.adapter.FILES
import app.yitap.search.adapter.GenerateSearchTarget
import app.yitap.search.adapter.HEADER_JUSTIFY
import app.yitap.search.adapter.HISTORY
import app.yitap.search.adapter.LOADING
import app.yitap.search.adapter.SETTINGS
import app.yitap.search.adapter.SPACE
import app.yitap.search.adapter.SearchResult
import app.yitap.search.adapter.SearchTargetCompat
import app.yitap.search.adapter.WEB_SUGGESTION
import app.yitap.search.adapter.createSearchTarget
import app.yitap.search.algorithms.data.Calculation
import app.yitap.search.algorithms.data.ContactInfo
import app.yitap.search.algorithms.data.IFileInfo
import app.yitap.search.algorithms.data.RecentKeyword
import app.yitap.search.algorithms.data.SettingInfo
import app.yitap.search.algorithms.data.WebSearchProvider
import app.yitap.search.algorithms.data.calculateEquationFromString
import app.yitap.search.algorithms.data.findContactsByName
import app.yitap.search.algorithms.data.findSettingsByNameAndAction
import app.yitap.search.algorithms.data.getRecentKeyword
import app.yitap.search.algorithms.data.queryFilesInMediaStore
import app.yitap.util.checkAndRequestFilesPermission
import app.yitap.util.isDefaultLauncher
import app.yitap.util.requestContactPermissionGranted
import com.android.launcher3.LauncherAppState
import com.android.launcher3.R
import com.android.launcher3.allapps.BaseAllAppsAdapter
import com.android.launcher3.model.AllAppsList
import com.android.launcher3.model.BaseModelUpdateTask
import com.android.launcher3.model.BgDataModel
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.search.SearchCallback
import com.android.launcher3.util.Executors
import com.patrykmichalik.opto.core.onEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class YitapLocalSearchAlgorithm(context: Context) : YitapSearchAlgorithm(context) {

    private val appState = LauncherAppState.getInstance(context)
    private val resultHandler = Handler(Executors.MAIN_EXECUTOR.looper)
    private val generateSearchTarget = GenerateSearchTarget(context)

    private var hiddenApps: Set<String> = emptySet()

    private var hiddenAppsInSearch = ""

    private var searchApps = true
    private var enableFuzzySearch = false
    private var useWebSuggestions = true
    private var webSuggestionsProvider = ""

    private val prefs: PreferenceManager = PreferenceManager.getInstance(context)
    private val pref2 = PreferenceManager2.getInstance(context)

    private var maxAppResultsCount = 5
    private var maxPeopleCount = 10
    private var maxWebSuggestionsCount = 3
    private var maxFilesCount = 3
    private var maxSettingsEntryCount = 5
    private var maxRecentResultCount = 2
    private var maxWebSuggestionDelay = 200

    val coroutineScope = CoroutineScope(context = Dispatchers.IO + SupervisorJob())

    init {
        pref2.enableFuzzySearch.onEach(launchIn = coroutineScope) {
            enableFuzzySearch = it
        }
        pref2.hiddenApps.onEach(launchIn = coroutineScope) {
            hiddenApps = it
        }
        pref2.hiddenAppsInSearch.onEach(launchIn = coroutineScope) {
            hiddenAppsInSearch = it
        }

        useWebSuggestions = prefs.searchResultStartPageSuggestion.get()
        searchApps = prefs.searchResultApps.get()

        pref2.webSuggestionProvider.onEach(launchIn = coroutineScope) {
            webSuggestionsProvider = it.toString()
        }

        pref2.maxAppSearchResultCount.onEach(launchIn = coroutineScope) {
            maxAppResultsCount = it
        }
        pref2.maxFileResultCount.onEach(launchIn = coroutineScope) {
            maxFilesCount = it
        }
        pref2.maxPeopleResultCount.onEach(launchIn = coroutineScope) {
            maxPeopleCount = it
        }
        pref2.maxWebSuggestionResultCount.onEach(launchIn = coroutineScope) {
            maxWebSuggestionsCount = it
        }
        pref2.maxSettingsEntryResultCount.onEach(launchIn = coroutineScope) {
            maxSettingsEntryCount = it
        }
        pref2.maxRecentResultCount.onEach(launchIn = coroutineScope) {
            maxRecentResultCount = it
        }
        pref2.maxWebSuggestionDelay.onEach(launchIn = coroutineScope) {
            maxWebSuggestionDelay = it
        }
    }

    private val searchUtils = SearchUtils(maxAppResultsCount, hiddenApps, hiddenAppsInSearch)

    override fun doSearch(query: String, callback: SearchCallback<BaseAllAppsAdapter.AdapterItem>) {
        appState.model.enqueueModelUpdateTask(object : BaseModelUpdateTask() {
            override fun execute(app: LauncherAppState, dataModel: BgDataModel, apps: AllAppsList) {
                coroutineScope.launch(Dispatchers.Main) {
                    getAllSearchResults(apps.data, query, prefs).collect { allResults ->
                        callback.onSearchResult(query, ArrayList(allResults))
                    }
                }
            }
        })
    }

    override fun cancel(interruptActiveRequests: Boolean) {
        if (interruptActiveRequests) {
            resultHandler.removeCallbacksAndMessages(null)
        }
    }

    private fun getAllSearchResults(
        apps: MutableList<AppInfo>,
        query: String,
        prefs: PreferenceManager,
    ): Flow<List<BaseAllAppsAdapter.AdapterItem>> = channelFlow {
        val allResults = mutableListOf<BaseAllAppsAdapter.AdapterItem>()
        var appIndex = 0

        launch {
            getAppSearchResults(apps, query).collect { appResults ->
                allResults.addAll(appResults)
                appIndex = appResults.size
                send(allResults.toList())
            }
        }

        launch {
            getLocalSearchResults(query, prefs).collect { localResults ->
                // Insert local results at the appropriate position
                val insertIndex = appIndex
                if (insertIndex >= 0) {
                    allResults.addAll(insertIndex, localResults)
                } else {
                    allResults.addAll(localResults)
                }
                send(allResults.toList())
            }
        }

        launch {
            getSearchLinks(query).collect { otherResults ->
                allResults.addAll(otherResults)
                send(allResults.toList())
            }
        }
    }

    private fun getAppSearchResults(
        apps: MutableList<AppInfo>,
        query: String,
    ): Flow<List<BaseAllAppsAdapter.AdapterItem>> = flow {
        val searchTargets = mutableListOf<SearchTargetCompat>()
        val appResults = performAppSearch(apps, query)

        parseAppSearchResults(appResults, searchTargets)

        setFirstItemQuickLaunch(searchTargets)
        emit(transformSearchResults(searchTargets))
    }

    private fun getLocalSearchResults(
        query: String,
        prefs: PreferenceManager,
    ): Flow<List<BaseAllAppsAdapter.AdapterItem>> = flow {
        val searchTargets = mutableListOf<SearchTargetCompat>()
        val localSearchResults = performDeviceLocalSearch(query, prefs)
        parseLocalSearchResults(localSearchResults, searchTargets)
        emit(transformSearchResults(searchTargets))
    }

    private fun getSearchLinks(
        query: String,
    ): Flow<List<BaseAllAppsAdapter.AdapterItem>> = flow {
        val searchTargets = mutableListOf<SearchTargetCompat>()

        searchTargets.add(generateSearchTarget.getHeaderTarget(SPACE))
        if (useWebSuggestions) {
            withContext(Dispatchers.IO) {
                searchTargets.add(generateSearchTarget.getWebSearchItem(query, webSuggestionsProvider))
            }
        }
        generateSearchTarget.getMarketSearchItem(query)?.let { searchTargets.add(it) }
        emit(transformSearchResults(searchTargets))
    }

    private fun parseAppSearchResults(
        appResults: List<AppInfo>,
        searchTargets: MutableList<SearchTargetCompat>,
    ) {
        if (appResults.isNotEmpty()) {
            if (appResults.size == 1 && context.isDefaultLauncher()) {
                val singleAppResult = appResults.firstOrNull()
                val shortcuts = singleAppResult?.let { searchUtils.getShortcuts(it, context) }
                if (shortcuts != null) {
                    if (shortcuts.isNotEmpty()) {
                        searchTargets.add(generateSearchTarget.getHeaderTarget(SPACE))
                        searchTargets.add(createSearchTarget(singleAppResult, true))
                        searchTargets.addAll(shortcuts.map(::createSearchTarget))
                    }
                }
            } else {
                appResults.mapTo(searchTargets, ::createSearchTarget)
            }
        }
    }

    private fun parseLocalSearchResults(
        localSearchResults: MutableList<SearchResult>,
        searchTargets: MutableList<SearchTargetCompat>,
    ) {
        val suggestionProvider = webSuggestionsProvider
        val suggestions = filterByType(localSearchResults, WEB_SUGGESTION)
        if (suggestions.isNotEmpty()) {
            val suggestionsHeader =
                generateSearchTarget.getHeaderTarget(context.getString(R.string.all_apps_search_result_suggestions))
            searchTargets.add(suggestionsHeader)
            searchTargets.addAll(
                suggestions.map {
                    generateSearchTarget.getSuggestionTarget(it.resultData as String, suggestionProvider)
                },
            )
        }

        val calculator = filterByType(localSearchResults, CALCULATOR).firstOrNull()
        val calcData = calculator?.resultData as? Calculation
        if (calcData != null && calcData.isValid) {
            val calculatorHeader =
                generateSearchTarget.getHeaderTarget(context.getString(R.string.all_apps_search_result_calculator))
            searchTargets.add(calculatorHeader)
            searchTargets.add(
                generateSearchTarget.getCalculationTarget(calcData),
            )
        }

        val contacts = filterByType(localSearchResults, CONTACT)
        if (contacts.isNotEmpty()) {
            val contactsHeader =
                generateSearchTarget.getHeaderTarget(context.getString(R.string.all_apps_search_result_contacts_from_device))
            searchTargets.add(contactsHeader)
            searchTargets.addAll(contacts.map { generateSearchTarget.getContactSearchItem(it.resultData as ContactInfo) })
        }

        val settings = filterByType(localSearchResults, SETTINGS)
        if (settings.isNotEmpty()) {
            val settingsHeader =
                generateSearchTarget.getHeaderTarget(context.getString(R.string.all_apps_search_result_settings_entry_from_device))
            searchTargets.add(settingsHeader)
            searchTargets.addAll(settings.mapNotNull { generateSearchTarget.getSettingSearchItem(it.resultData as SettingInfo) })
        }

        // todo refactor to only show when search is first clicked
        val recentKeyword = filterByType(localSearchResults, HISTORY)
        if (recentKeyword.isNotEmpty()) {
            val recentKeywordHeader = generateSearchTarget.getHeaderTarget(
                context.getString(R.string.search_pref_result_history_title),
                HEADER_JUSTIFY,
            )
            searchTargets.add(recentKeywordHeader)
            searchTargets.addAll(recentKeyword.map { generateSearchTarget.getRecentKeywordTarget(it.resultData as RecentKeyword, suggestionProvider) })
        }

        val files = filterByType(localSearchResults, FILES)
        if (files.isNotEmpty()) {
            val filesHeader =
                generateSearchTarget.getHeaderTarget(context.getString(R.string.all_apps_search_result_files))
            searchTargets.add(filesHeader)
            searchTargets.addAll(files.map { generateSearchTarget.getFileInfoSearchItem(it.resultData as IFileInfo) })
        }
    }

    private fun performAppSearch(
        apps: MutableList<AppInfo>,
        query: String,
    ) = if (enableFuzzySearch) {
        searchUtils.fuzzySearch(apps, query)
    } else {
        searchUtils.normalSearch(apps, query)
    }

    private suspend fun performDeviceLocalSearch(query: String, prefs: PreferenceManager): MutableList<SearchResult> =
        withContext(Dispatchers.IO) {
            val results = ArrayList<SearchResult>()

            if (prefs.searchResultCalculator.get()) {
                val calculations = calculateEquationFromString(query)
                results.add(SearchResult(CALCULATOR, calculations))
            }

            val contactDeferred = async {
                if (prefs.searchResultPeople.get() && requestContactPermissionGranted(
                        context,
                        prefs,
                    )
                ) {
                    findContactsByName(context, query, maxPeopleCount)
                        .map { SearchResult(CONTACT, it) }
                } else {
                    emptyList()
                }
            }

            val filesDeferred = async {
                if (prefs.searchResultFiles.get() && checkAndRequestFilesPermission(
                        context,
                        prefs,
                    )
                ) {
                    queryFilesInMediaStore(context, keyword = query, maxResult = maxFilesCount)
                        .toList()
                        .map { SearchResult(FILES, it) }
                } else {
                    emptyList()
                }
            }

            val settingsDeferred = async {
                if (prefs.searchResultSettingsEntry.get()) {
                    findSettingsByNameAndAction(query, maxSettingsEntryCount)
                        .map { SearchResult(SETTINGS, it) }
                } else {
                    emptyList()
                }
            }

            val startPageSuggestionsDeferred = async {
                try {
                    val timeout = maxWebSuggestionDelay.toLong()
                    val result = withTimeoutOrNull(timeout) {
                        if (prefs.searchResultStartPageSuggestion.get()) {
                            WebSearchProvider.fromString(webSuggestionsProvider).getSuggestions(query, maxWebSuggestionsCount).map {
                                SearchResult(
                                    WEB_SUGGESTION,
                                    it,
                                )
                            }
                        } else {
                            emptyList()
                        }
                    }
                    result ?: emptyList()
                } catch (e: TimeoutCancellationException) {
                    emptyList()
                }
            }

            if (prefs.searchResulRecentSuggestion.get()) {
                getRecentKeyword(
                    context,
                    query,
                    maxRecentResultCount,
                    object : app.yitap.search.algorithms.data.SearchCallback {
                        override fun onSearchLoaded(items: List<Any>) {
                            results.addAll(items.map { SearchResult(HISTORY, it) })
                        }

                        override fun onSearchFailed(error: String) {
                            results.add(SearchResult(ERROR, error))
                        }

                        override fun onLoading() {
                            results.add(SearchResult(LOADING, "Loading"))
                        }
                    },
                )
            }

            results.addAll(contactDeferred.await())
            results.addAll(filesDeferred.await())
            results.addAll(settingsDeferred.await())
            results.addAll(startPageSuggestionsDeferred.await())

            results
        }

    private fun filterByType(results: List<SearchResult>, type: String): List<SearchResult> {
        return results.filter { it.resultType == type }
    }
}
