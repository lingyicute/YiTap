/*
 * Copyright 2022, Yitap
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.yitap.ui.preferences.destinations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.yitap.data.iconoverride.IconOverrideRepository
import app.yitap.nexuslauncher.OverlayCallbackImpl
import app.yitap.preferences.getAdapter
import app.yitap.preferences.preferenceManager
import app.yitap.preferences2.preferenceManager2
import app.yitap.theme.color.ColorMode
import app.yitap.ui.preferences.LocalIsExpandedScreen
import app.yitap.ui.preferences.components.FeedPreference
import app.yitap.ui.preferences.components.GestureHandlerPreference
import app.yitap.ui.preferences.components.NavigationActionPreference
import app.yitap.ui.preferences.components.controls.ClickablePreference
import app.yitap.ui.preferences.components.controls.ListPreference
import app.yitap.ui.preferences.components.controls.SliderPreference
import app.yitap.ui.preferences.components.controls.SwitchPreference
import app.yitap.ui.preferences.components.layout.DividerColumn
import app.yitap.ui.preferences.components.layout.ExpandAndShrink
import app.yitap.ui.preferences.components.layout.PreferenceGroup
import app.yitap.ui.preferences.components.layout.PreferenceLayout
import app.yitap.util.collectAsStateBlocking
import com.android.launcher3.R
import com.android.launcher3.Utilities
import kotlinx.coroutines.launch

object HomeScreenRoutes {
    const val GRID = "grid"
}

@Composable
fun HomeScreenPreferences(
    modifier: Modifier = Modifier,
) {
    val prefs = preferenceManager()
    val prefs2 = preferenceManager2()
    val scope = rememberCoroutineScope()
    PreferenceLayout(
        label = stringResource(id = R.string.home_screen_label),
        backArrowVisible = !LocalIsExpandedScreen.current,
        modifier = modifier,
    ) {
        val lockHomeScreenAdapter = prefs2.lockHomeScreen.getAdapter()
        PreferenceGroup(heading = stringResource(id = R.string.general_label)) {
            val addIconToHomeAdapter = prefs.addIconToHome.getAdapter()
            SwitchPreference(
                checked = !lockHomeScreenAdapter.state.value && addIconToHomeAdapter.state.value,
                onCheckedChange = addIconToHomeAdapter::onChange,
                label = stringResource(id = R.string.auto_add_shortcuts_label),
                description = if (lockHomeScreenAdapter.state.value) stringResource(id = R.string.home_screen_locked) else null,
                enabled = lockHomeScreenAdapter.state.value.not(),
            )
            GestureHandlerPreference(
                adapter = prefs2.doubleTapGestureHandler.getAdapter(),
                label = stringResource(id = R.string.gesture_double_tap),
            )
            HomeScreenTextColorPreference()
        }
        PreferenceGroup(heading = stringResource(id = R.string.minus_one)) {
            val feedAvailable = OverlayCallbackImpl.minusOneAvailable(LocalContext.current)
            val enableFeedAdapter = prefs2.enableFeed.getAdapter()
            SwitchPreference(
                adapter = enableFeedAdapter,
                label = stringResource(id = R.string.minus_one_enable),
                description = if (feedAvailable) null else stringResource(id = R.string.minus_one_unavailable),
                enabled = feedAvailable,
            )
            ExpandAndShrink(visible = feedAvailable && enableFeedAdapter.state.value) {
                FeedPreference()
            }
        }
        PreferenceGroup(heading = stringResource(id = R.string.wallpaper)) {
            SwitchPreference(
                prefs.wallpaperScrolling.getAdapter(),
                label = stringResource(id = R.string.wallpaper_scrolling_label),
            )
            if (Utilities.ATLEAST_R) {
                SwitchPreference(
                    prefs2.wallpaperDepthEffect.getAdapter(),
                    label = stringResource(id = R.string.wallpaper_depth_effect_label),
                    description = stringResource(id = R.string.wallpaper_depth_effect_description),
                )
            }
            SwitchPreference(
                adapter = prefs2.showTopShadow.getAdapter(),
                label = stringResource(id = R.string.show_sys_ui_scrim),
            )
        }
        PreferenceGroup(heading = stringResource(id = R.string.layout)) {
            val columns by prefs.workspaceColumns.getAdapter()
            val rows by prefs.workspaceRows.getAdapter()
            NavigationActionPreference(
                label = stringResource(id = R.string.home_screen_grid),
                destination = HomeScreenRoutes.GRID,
                subtitle = stringResource(id = R.string.x_by_y, columns, rows),
            )
            DividerColumn {
                SwitchPreference(
                    adapter = lockHomeScreenAdapter,
                    label = stringResource(id = R.string.home_screen_lock),
                    description = stringResource(id = R.string.home_screen_lock_description),
                )
                SwitchPreference(
                    adapter = prefs2.enableDotPagination.getAdapter(),
                    label = stringResource(id = R.string.show_dot_pagination_label),
                    description = stringResource(id = R.string.show_dot_pagination_description),
                )
            }
        }
        PreferenceGroup(heading = stringResource(id = R.string.popup_menu)) {
            SwitchPreference(
                adapter = prefs2.enableMaterialUPopUp.getAdapter(),
                label = stringResource(id = R.string.show_material_u_popup_label),
                description = stringResource(id = R.string.show_material_u_popup_description),
            )
            SwitchPreference(
                adapter = prefs2.lockHomeScreenButtonOnPopUp.getAdapter(),
                label = stringResource(id = R.string.home_screen_lock_toggle_from_home_popup),
            )
            SwitchPreference(
                adapter = prefs2.showSystemSettingsEntryOnPopUp.getAdapter(),
                label = stringResource(id = R.string.show_system_settings_entry),
            )
            SwitchPreference(
                adapter = prefs2.editHomeScreenButtonOnPopUp.getAdapter(),
                label = stringResource(id = R.string.home_screen_edit_toggle_from_home_popup),
                enabled = lockHomeScreenAdapter.state.value.not(),
            )
        }
        PreferenceGroup(heading = stringResource(id = R.string.status_bar_label)) {
            val showStatusBarAdapter = prefs2.showStatusBar.getAdapter()
            SwitchPreference(
                adapter = showStatusBarAdapter,
                label = stringResource(id = R.string.show_status_bar),
            )
            ExpandAndShrink(visible = showStatusBarAdapter.state.value) {
                SwitchPreference(
                    adapter = prefs2.darkStatusBar.getAdapter(),
                    label = stringResource(id = R.string.dark_status_bar_label),
                )
            }
        }
        PreferenceGroup(heading = stringResource(id = R.string.icons)) {
            SliderPreference(
                label = stringResource(id = R.string.icon_size),
                adapter = prefs2.homeIconSizeFactor.getAdapter(),
                step = 0.1f,
                valueRange = 0.5F..1.5F,
                showAsPercentage = true,
            )
            val homeScreenLabelsAdapter = prefs2.showIconLabelsOnHomeScreen.getAdapter()
            SwitchPreference(
                adapter = homeScreenLabelsAdapter,
                label = stringResource(id = R.string.show_home_labels),
            )
            ExpandAndShrink(visible = homeScreenLabelsAdapter.state.value) {
                SliderPreference(
                    label = stringResource(id = R.string.label_size),
                    adapter = prefs2.homeIconLabelSizeFactor.getAdapter(),
                    step = 0.1f,
                    valueRange = 0.5F..1.5F,
                    showAsPercentage = true,
                )
            }
        }
        val overrideRepo = IconOverrideRepository.INSTANCE.get(LocalContext.current)
        val customIconsCount by remember { overrideRepo.observeCount() }.collectAsStateBlocking()
        ExpandAndShrink(visible = customIconsCount > 0) {
            PreferenceGroup {
                ClickablePreference(
                    label = stringResource(id = R.string.reset_custom_icons),
                    confirmationText = stringResource(id = R.string.reset_custom_icons_confirmation),
                    onClick = { scope.launch { overrideRepo.deleteAll() } },
                )
            }
        }
        PreferenceGroup(heading = stringResource(id = R.string.widget_button_text)) {
            SwitchPreference(
                adapter = prefs2.roundedWidgets.getAdapter(),
                label = stringResource(id = R.string.force_rounded_widgets),
            )
            SwitchPreference(
                adapter = prefs2.allowWidgetOverlap.getAdapter(),
                label = stringResource(id = R.string.allow_widget_overlap),
            )
            SwitchPreference(
                adapter = prefs2.widgetUnlimitedSize.getAdapter(),
                label = stringResource(id = R.string.widget_unlimited_size_label),
                description = stringResource(id = R.string.widget_unlimited_size_description),
            )
            SwitchPreference(
                adapter = prefs2.forceWidgetResize.getAdapter(),
                label = stringResource(id = R.string.force_widget_resize_label),
                description = stringResource(id = R.string.force_widget_resize_description),
            )
        }
    }
}

@Composable
fun HomeScreenTextColorPreference(
    modifier: Modifier = Modifier,
) {
    ListPreference(
        adapter = preferenceManager2().workspaceTextColor.getAdapter(),
        entries = ColorMode.entries(),
        label = stringResource(id = R.string.home_screen_text_color),
        modifier = modifier,
    )
}