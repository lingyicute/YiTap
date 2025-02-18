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

package app.yitap.ui.preferences

import app.yitap.ui.preferences.about.acknowledgements.OssLibrary
import app.yitap.ui.preferences.destinations.IconPackInfo
import kotlinx.coroutines.flow.StateFlow

sealed interface PreferenceInteractor {
    val ossLibraries: StateFlow<List<OssLibrary>>
    val iconPacks: StateFlow<List<IconPackInfo>>
    val themedIconPacks: StateFlow<List<IconPackInfo>>
}
