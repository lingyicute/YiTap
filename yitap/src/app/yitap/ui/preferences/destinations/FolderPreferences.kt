/*
 * Copyright 2021, Yitap
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.yitap.preferences.getAdapter
import app.yitap.preferences.preferenceManager
import app.yitap.preferences2.preferenceManager2
import app.yitap.ui.preferences.LocalIsExpandedScreen
import app.yitap.ui.preferences.components.colorpreference.ColorPreference
import app.yitap.ui.preferences.components.controls.SliderPreference
import app.yitap.ui.preferences.components.controls.SwitchPreference
import app.yitap.ui.preferences.components.layout.ExpandAndShrink
import app.yitap.ui.preferences.components.layout.PreferenceGroup
import app.yitap.ui.preferences.components.layout.PreferenceLayout
import com.android.launcher3.R

@Composable
fun FolderPreferences(
    modifier: Modifier = Modifier,
) {
    PreferenceLayout(
        label = stringResource(id = R.string.folders_label),
        backArrowVisible = !LocalIsExpandedScreen.current,
        modifier = modifier,
    ) {
        val prefs = preferenceManager()
        val prefs2 = preferenceManager2()
        PreferenceGroup(heading = stringResource(id = R.string.general_label)) {
            ColorPreference(preference = prefs2.folderColor)
            SliderPreference(
                label = stringResource(id = R.string.folder_preview_bg_opacity_label),
                adapter = prefs2.folderPreviewBackgroundOpacity.getAdapter(),
                step = 0.1F,
                valueRange = 0F..1F,
                showAsPercentage = true,
            )
        }
        PreferenceGroup(heading = stringResource(id = R.string.grid)) {
            SliderPreference(
                label = stringResource(id = R.string.max_folder_columns),
                adapter = prefs2.folderColumns.getAdapter(),
                step = 1,
                valueRange = 2..5,
            )
            SliderPreference(
                label = stringResource(id = R.string.max_folder_rows),
                adapter = prefs.folderRows.getAdapter(),
                step = 1,
                valueRange = 2..5,
            )
        }

        PreferenceGroup(heading = stringResource(id = R.string.icons)) {
            val homeScreenLabelsAdapter = prefs2.showIconLabelsOnHomeScreenFolder.getAdapter()
            SwitchPreference(
                adapter = homeScreenLabelsAdapter,
                label = stringResource(id = R.string.show_home_labels),
            )
            ExpandAndShrink(visible = homeScreenLabelsAdapter.state.value) {
                SliderPreference(
                    label = stringResource(id = R.string.label_size),
                    adapter = prefs2.homeIconLabelFolderSizeFactor.getAdapter(),
                    step = 0.1f,
                    valueRange = 0.5F..1.5F,
                    showAsPercentage = true,
                )
            }
        }
    }
}
