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

package app.yitap.gestures.handlers

import android.content.Context
import app.yitap.YitapLauncher

sealed class GestureHandler(val context: Context) {
    abstract suspend fun onTrigger(launcher: YitapLauncher)
}

class NoOpGestureHandler(context: Context) : GestureHandler(context) {
    override suspend fun onTrigger(launcher: YitapLauncher) = Unit
}
