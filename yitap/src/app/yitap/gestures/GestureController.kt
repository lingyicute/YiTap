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

package app.yitap.gestures

import androidx.lifecycle.lifecycleScope
import app.yitap.YitapApp
import app.yitap.YitapLauncher
import app.yitap.gestures.config.GestureHandlerConfig
import app.yitap.gestures.handlers.GestureHandler
import app.yitap.gestures.handlers.NoOpGestureHandler
import app.yitap.preferences2.PreferenceManager2
import com.android.launcher3.util.VibratorWrapper
import com.patrykmichalik.opto.domain.Preference
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

class GestureController(private val launcher: YitapLauncher) {
    private val prefs = PreferenceManager2.getInstance(launcher)
    private val scope = MainScope()

    private val doubleTapHandler = handler(prefs.doubleTapGestureHandler)
    private val swipeUpHandler = handler(prefs.swipeUpGestureHandler)
    private val swipeDownHandler = handler(prefs.swipeDownGestureHandler)
    private val homePressHandler = handler(prefs.homePressGestureHandler)
    private val backPressHandler = handler(prefs.backPressGestureHandler)

    fun onDoubleTap() {
        triggerHandler(doubleTapHandler)
    }

    fun onSwipeUp() {
        triggerHandler(swipeUpHandler)
    }

    fun onSwipeDown() {
        triggerHandler(swipeDownHandler)
    }

    fun onHomePressed() {
        triggerHandler(homePressHandler, YitapApp.isRecentsEnabled)
    }

    fun onBackPressed() {
        triggerHandler(backPressHandler, false)
    }

    private fun triggerHandler(handlerFlow: Flow<GestureHandler>, withHaptic: Boolean = true) {
        launcher.lifecycleScope.launch {
            val handler = handlerFlow.first()
            if (handler is NoOpGestureHandler) {
                return@launch
            }
            handler.onTrigger(launcher)
            if (withHaptic) {
                VibratorWrapper.INSTANCE.get(launcher).vibrate(VibratorWrapper.OVERVIEW_HAPTIC)
            }
        }
    }

    private fun handler(pref: Preference<GestureHandlerConfig, String, *>) = pref.get()
        .distinctUntilChanged()
        .map { it.createHandler(launcher) }
        .shareIn(
            scope,
            SharingStarted.Lazily,
            replay = 1,
        )
}
