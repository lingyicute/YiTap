package app.yitap.ui.preferences.destinations

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.Preferences
import app.yitap.preferences.PreferenceManager
import app.yitap.preferences.getAdapter
import app.yitap.preferences.preferenceManager
import app.yitap.preferences2.PreferenceManager2
import app.yitap.preferences2.preferenceManager2
import app.yitap.ui.preferences.components.controls.ClickablePreference
import app.yitap.ui.preferences.components.controls.MainSwitchPreference
import app.yitap.ui.preferences.components.controls.SwitchPreference
import app.yitap.ui.preferences.components.controls.TextPreference
import app.yitap.ui.preferences.components.layout.PreferenceGroup
import app.yitap.ui.preferences.components.layout.PreferenceLayout
import com.android.launcher3.settings.SettingsActivity
import com.android.launcher3.uioverrides.flags.DeveloperOptionsFragment
import com.patrykmichalik.opto.domain.Preference

/**
 * A screen to house unfinished preferences and debug flags
 */
@Composable
fun DebugMenuPreferences(
    modifier: Modifier = Modifier,
) {
    val prefs = preferenceManager()
    val prefs2 = preferenceManager2()
    val flags = remember { prefs.debugFlags }
    val flags2 = remember { prefs2.debugFlags }
    val textFlags = remember { prefs2.textFlags }
    val context = LocalContext.current

    val enableDebug = prefs.enableDebugMenu.getAdapter()

    PreferenceLayout(
        label = "Debug Menu",
        modifier = modifier,
    ) {
        MainSwitchPreference(adapter = enableDebug, label = "Show Debug Menu") {
            PreferenceGroup {
                ClickablePreference(
                    label = "Feature Flags",
                    onClick = {
                        Intent(context, SettingsActivity::class.java)
                            .putExtra(
                                ":settings:fragment",
                                DeveloperOptionsFragment::class.java.name,
                            )
                            .also { context.startActivity(it) }
                    },
                )
                ClickablePreference(
                    label = "Crash Launcher",
                    onClick = { throw RuntimeException("User triggered crash") },
                )
            }

            PreferenceGroup(heading = "Debug Flags") {
                flags2.forEach {
                    SwitchPreference(
                        adapter = it.getAdapter(),
                        label = it.key.name,
                    )
                }
                flags.forEach {
                    SwitchPreference(
                        adapter = it.getAdapter(),
                        label = it.key,
                    )
                }
                textFlags.forEach {
                    TextPreference(
                        adapter = it.getAdapter(),
                        label = it.key.name,
                    )
                }
            }
        }
    }
}

private val PreferenceManager2.debugFlags: List<Preference<Boolean, Boolean, Preferences.Key<Boolean>>>
    get() = listOf(showComponentNames)

private val PreferenceManager2.textFlags: List<Preference<String, String, Preferences.Key<String>>>
    get() = listOf(additionalFonts)

private val PreferenceManager.debugFlags
    get() = listOf(
        deviceSearch,
        searchResultShortcuts,
        searchResultPeople,
        searchResultPixelTips,
        searchResultSettings,
        ignoreFeedWhitelist,
    )