/*
 * copyright 2021, YiTap
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

package app.yitap.ui.preferences.components.controls

import android.R as AndroidR
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.yitap.preferences.PreferenceAdapter
import app.yitap.ui.ModalBottomSheetContent
import app.yitap.ui.preferences.components.layout.PreferenceDivider
import app.yitap.ui.preferences.components.layout.PreferenceTemplate
import app.yitap.ui.util.addIf
import app.yitap.ui.util.bottomSheetHandler
import kotlinx.collections.immutable.ImmutableList

@Composable
fun <T> ListPreference(
    adapter: PreferenceAdapter<T>,
    entries: ImmutableList<ListPreferenceEntry<T>>,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
    endWidget: (@Composable () -> Unit)? = null,
) {
    ListPreference(
        entries = entries,
        value = adapter.state.value,
        onValueChange = adapter::onChange,
        label = label,
        modifier = modifier,
        enabled = enabled,
        description = description,
        endWidget = endWidget,
    )
}

@Composable
fun <T> ListPreference(
    entries: ImmutableList<ListPreferenceEntry<T>>,
    value: T,
    onValueChange: (T) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
    endWidget: (@Composable () -> Unit)? = null,
) {
    val bottomSheetHandler = bottomSheetHandler
    val currentDescription = description ?: entries
        .firstOrNull { it.value == value }
        ?.label?.invoke()

    PreferenceTemplate(
        contentModifier = Modifier
            .fillMaxHeight()
            .padding(vertical = 16.dp)
            .padding(start = 16.dp),
        title = { Text(text = label) },
        description = { currentDescription?.let { Text(text = it) } },
        enabled = enabled,
        endWidget = endWidget,
        applyPaddings = false,
        modifier = modifier.clickable(enabled) {
            bottomSheetHandler.show {
                ModalBottomSheetContent(
                    title = { Text(label) },
                    buttons = {
                        OutlinedButton(onClick = { bottomSheetHandler.hide() }) {
                            Text(text = stringResource(id = AndroidR.string.cancel))
                        }
                    },
                ) {
                    LazyColumn {
                        itemsIndexed(entries) { index, item ->
                            if (index > 0) {
                                PreferenceDivider(startIndent = 40.dp)
                            }
                            PreferenceTemplate(
                                enabled = item.enabled,
                                title = { Text(item.label()) },
                                modifier = Modifier.clickable(item.enabled) {
                                    onValueChange(item.value)
                                    bottomSheetHandler.hide()
                                },
                                startWidget = {
                                    RadioButton(
                                        selected = item.value == value,
                                        onClick = null,
                                        enabled = item.enabled,
                                    )
                                },
                                endWidget = item.endWidget,
                            )
                        }
                    }
                }
            }
        }.addIf(endWidget != null) { padding(end = 16.dp) },
    )
}

class ListPreferenceEntry<T>(
    val value: T,
    val enabled: Boolean = true,
    val endWidget: (@Composable () -> Unit)? = null,
    val label: @Composable () -> String,
)
