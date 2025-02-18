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

package app.yitap

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import app.yitap.YitapApp.Companion.showQuickstepWarningIfNecessary
import app.yitap.compat.YitapQuickstepCompat
import app.yitap.factory.YitapWidgetHolder
import app.yitap.gestures.GestureController
import app.yitap.gestures.VerticalSwipeTouchController
import app.yitap.gestures.config.GestureHandlerConfig
import app.yitap.nexuslauncher.OverlayCallbackImpl
import app.yitap.preferences.PreferenceManager
import app.yitap.preferences2.PreferenceManager2
import app.yitap.root.RootHelperManager
import app.yitap.root.RootNotAvailableException
import app.yitap.theme.ThemeProvider
import app.yitap.ui.popup.YitapShortcut
import app.yitap.util.getThemedIconPacksInstalled
import app.yitap.util.unsafeLazy
import com.android.launcher3.AbstractFloatingView
import com.android.launcher3.BaseActivity
import com.android.launcher3.BubbleTextView
import com.android.launcher3.GestureNavContract
import com.android.launcher3.LauncherAppState
import com.android.launcher3.LauncherState
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.popup.SystemShortcut
import com.android.launcher3.statemanager.StateManager
import com.android.launcher3.uioverrides.QuickstepLauncher
import com.android.launcher3.uioverrides.states.AllAppsState
import com.android.launcher3.uioverrides.states.OverviewState
import com.android.launcher3.util.ActivityOptionsWrapper
import com.android.launcher3.util.Executors
import com.android.launcher3.util.RunnableList
import com.android.launcher3.util.SystemUiController.UI_STATE_BASE_WINDOW
import com.android.launcher3.util.Themes
import com.android.launcher3.util.TouchController
import com.android.launcher3.views.ComposeInitializer
import com.android.launcher3.views.FloatingSurfaceView
import com.android.launcher3.widget.LauncherWidgetHolder
import com.android.launcher3.widget.RoundedCornerEnforcement
import com.android.systemui.plugins.shared.LauncherOverlayManager
import com.android.systemui.shared.system.QuickStepContract
import com.kieronquinn.app.smartspacer.sdk.client.SmartspacerClient
import com.patrykmichalik.opto.core.firstBlocking
import com.patrykmichalik.opto.core.onEach
import dev.kdrag0n.monet.theme.ColorScheme
import java.util.stream.Stream
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class YitapLauncher :
    QuickstepLauncher(),
    LifecycleOwner,
    SavedStateRegistryOwner,
    ActivityResultRegistryOwner,
    OnBackPressedDispatcherOwner {

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val defaultOverlay by unsafeLazy { OverlayCallbackImpl(this) }
    private val prefs by unsafeLazy { PreferenceManager.getInstance(this) }
    private val preferenceManager2 by unsafeLazy { PreferenceManager2.getInstance(this) }
    private val insetsController by unsafeLazy { WindowInsetsControllerCompat(launcher.window, rootView) }
    private val themeProvider by unsafeLazy { ThemeProvider.INSTANCE.get(this) }
    private val noStatusBarStateListener = object : StateManager.StateListener<LauncherState> {
        override fun onStateTransitionStart(toState: LauncherState) {
            if (toState is OverviewState) {
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            }
        }
        override fun onStateTransitionComplete(finalState: LauncherState) {
            if (finalState !is OverviewState) {
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
            }
        }
    }
    private val rememberPositionStateListener = object : StateManager.StateListener<LauncherState> {
        override fun onStateTransitionStart(toState: LauncherState) {
            if (toState is AllAppsState) {
                mAppsView.activeRecyclerView.restoreScrollPosition()
            }
        }
        override fun onStateTransitionComplete(finalState: LauncherState) {}
    }
    private lateinit var colorScheme: ColorScheme
    private var hasBackGesture = false

    val gestureController by unsafeLazy { GestureController(this) }

    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry
    override val activityResultRegistry = object : ActivityResultRegistry() {
        override fun <I : Any?, O : Any?> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            val activity = this@YitapLauncher

            // Immediate result path
            val synchronousResult = contract.getSynchronousResult(activity, input)
            if (synchronousResult != null) {
                Handler(Looper.getMainLooper()).post {
                    dispatchResult(
                        requestCode,
                        synchronousResult.value,
                    )
                }
                return
            }

            // Start activity path
            val intent = contract.createIntent(activity, input)
            var optionsBundle: Bundle? = null
            // If there are any extras, we should defensively set the classLoader
            if (intent.extras != null && intent.extras!!.classLoader == null) {
                intent.setExtrasClassLoader(activity.classLoader)
            }
            if (intent.hasExtra(StartActivityForResult.EXTRA_ACTIVITY_OPTIONS_BUNDLE)) {
                optionsBundle =
                    intent.getBundleExtra(StartActivityForResult.EXTRA_ACTIVITY_OPTIONS_BUNDLE)
                intent.removeExtra(StartActivityForResult.EXTRA_ACTIVITY_OPTIONS_BUNDLE)
            } else if (options != null) {
                optionsBundle = options.toBundle()
            }
            if (RequestMultiplePermissions.ACTION_REQUEST_PERMISSIONS == intent.action) {
                // requestPermissions path
                var permissions =
                    intent.getStringArrayExtra(RequestMultiplePermissions.EXTRA_PERMISSIONS)
                if (permissions == null) {
                    permissions = arrayOfNulls(0)
                }
                ActivityCompat.requestPermissions(activity, permissions, requestCode)
            } else if (StartIntentSenderForResult.ACTION_INTENT_SENDER_REQUEST == intent.action) {
                val request: IntentSenderRequest =
                    intent.getParcelableExtra(StartIntentSenderForResult.EXTRA_INTENT_SENDER_REQUEST)!!
                try {
                    // startIntentSenderForResult path
                    ActivityCompat.startIntentSenderForResult(
                        activity,
                        request.intentSender,
                        requestCode,
                        request.fillInIntent,
                        request.flagsMask,
                        request.flagsValues,
                        0,
                        optionsBundle,
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Handler(Looper.getMainLooper()).post {
                        dispatchResult(
                            requestCode,
                            RESULT_CANCELED,
                            Intent()
                                .setAction(StartIntentSenderForResult.ACTION_INTENT_SENDER_REQUEST)
                                .putExtra(StartIntentSenderForResult.EXTRA_SEND_INTENT_EXCEPTION, e),
                        )
                    }
                }
            } else {
                // startActivityForResult path
                ActivityCompat.startActivityForResult(activity, intent, requestCode, optionsBundle)
            }
        }
    }
    override val onBackPressedDispatcher = OnBackPressedDispatcher {
        super.onBackPressed()
    }
    override val lifecycle: LifecycleRegistry = LifecycleRegistry(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        layoutInflater.factory2 = YitapLayoutFactory(this)
        savedStateRegistryController.performRestore(savedInstanceState)
        super.onCreate(savedInstanceState)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        prefs.launcherTheme.subscribeChanges(this, ::updateTheme)
        prefs.feedProvider.subscribeChanges(this, defaultOverlay::reconnect)
        preferenceManager2.enableFeed.get().distinctUntilChanged().onEach { enable ->
            defaultOverlay.setEnableFeed(enable)
        }.launchIn(scope = lifecycleScope)

        if (prefs.autoLaunchRoot.get()) {
            lifecycleScope.launch {
                try {
                    RootHelperManager.INSTANCE.get(this@YitapLauncher).getService()
                } catch (_: RootNotAvailableException) {
                }
            }
        }

        preferenceManager2.showStatusBar.get().distinctUntilChanged().onEach {
            with(insetsController) {
                if (it) {
                    show(WindowInsetsCompat.Type.statusBars())
                } else {
                    hide(WindowInsetsCompat.Type.statusBars())
                }
            }
            with(launcher.stateManager) {
                if (it) {
                    removeStateListener(noStatusBarStateListener)
                } else {
                    addStateListener(noStatusBarStateListener)
                }
            }
        }.launchIn(scope = lifecycleScope)

        preferenceManager2.rememberPosition.get().onEach {
            with(launcher.stateManager) {
                if (it) {
                    addStateListener(rememberPositionStateListener)
                } else {
                    removeStateListener(rememberPositionStateListener)
                }
            }
        }.launchIn(scope = lifecycleScope)

        prefs.overrideWindowCornerRadius.subscribeValues(this) {
            QuickStepContract.sHasCustomCornerRadius = it
        }
        prefs.windowCornerRadius.subscribeValues(this) {
            QuickStepContract.sCustomCornerRadius = it.toFloat()
        }
        preferenceManager2.roundedWidgets.onEach(launchIn = lifecycleScope) {
            RoundedCornerEnforcement.sRoundedCornerEnabled = it
        }
        val isWorkspaceDarkText = Themes.getAttrBoolean(this, R.attr.isWorkspaceDarkText)
        preferenceManager2.darkStatusBar.onEach(launchIn = lifecycleScope) { darkStatusBar ->
            systemUiController.updateUiState(UI_STATE_BASE_WINDOW, isWorkspaceDarkText || darkStatusBar)
        }
        preferenceManager2.backPressGestureHandler.onEach(launchIn = lifecycleScope) { handler ->
            hasBackGesture = handler !is GestureHandlerConfig.NoOp
        }

        // Handle update from version 12 Alpha 4 to version 12 Alpha 5.
        if (
            prefs.themedIcons.get() &&
            packageManager.getThemedIconPacksInstalled(this).isEmpty()
        ) {
            prefs.themedIcons.set(newValue = false)
        }

        colorScheme = themeProvider.colorScheme

        showQuickstepWarningIfNecessary()

        reloadIconsIfNeeded()
    }

    override fun setupViews() {
        super.setupViews()
        ComposeInitializer.initCompose(this)
    }

    override fun collectStateHandlers(out: MutableList<StateManager.StateHandler<*>>) {
        super.collectStateHandlers(out)
        out.add(SearchBarStateHandler(this))
    }

    override fun getSupportedShortcuts(): Stream<SystemShortcut.Factory<*>> =
        Stream.concat(super.getSupportedShortcuts(), Stream.of(YitapShortcut.UNINSTALL, YitapShortcut.CUSTOMIZE))

    override fun updateTheme() {
        if (themeProvider.colorScheme != colorScheme) {
            recreate()
        } else {
            super.updateTheme()
        }
    }

    override fun createTouchControllers(): Array<TouchController> {
        val verticalSwipeController = VerticalSwipeTouchController(this, gestureController)
        return arrayOf<TouchController>(verticalSwipeController) + super.createTouchControllers()
    }

    override fun handleHomeTap() {
        gestureController.onHomePressed()
    }

    override fun registerBackDispatcher() {
        if (YitapApp.isAtleastT) {
            super.registerBackDispatcher()
        }
    }

    override fun handleGestureContract(intent: Intent?) {
        if (!YitapApp.isRecentsEnabled) {
            val gnc = GestureNavContract.fromIntent(intent)
            if (gnc != null) {
                AbstractFloatingView.closeOpenViews(
                    this,
                    false,
                    AbstractFloatingView.TYPE_ICON_SURFACE,
                )
                FloatingSurfaceView.show(this, gnc)
            }
        }
    }

    override fun onUiChangedWhileSleeping() {
        if (Utilities.ATLEAST_S) {
            super.onUiChangedWhileSleeping()
        }
    }

    override fun createAppWidgetHolder(): LauncherWidgetHolder {
        val factory = LauncherWidgetHolder.HolderFactory.newFactory(this) as YitapWidgetHolder.YitapHolderFactory
        return factory.newInstance(
            this,
        ) { appWidgetId: Int ->
            workspace.removeWidget(
                appWidgetId,
            )
        }
    }

    override fun makeDefaultActivityOptions(splashScreenStyle: Int): ActivityOptionsWrapper {
        val callbacks = RunnableList()
        val options = if (Utilities.ATLEAST_Q) {
            YitapQuickstepCompat.activityOptionsCompat.makeCustomAnimation(
                this,
                0,
                0,
                Executors.MAIN_EXECUTOR.handler,
                null,
            ) {
                callbacks.executeAllAndDestroy()
            }
        } else {
            ActivityOptions.makeBasic()
        }
        if (Utilities.ATLEAST_T) {
            options.setSplashScreenStyle(splashScreenStyle)
        }

        Utilities.allowBGLaunch(options)
        return ActivityOptionsWrapper(options, callbacks)
    }

    override fun getActivityLaunchOptions(v: View?, item: ItemInfo?): ActivityOptionsWrapper {
        return runCatching {
            super.getActivityLaunchOptions(v, item)
        }.getOrElse {
            getActivityLaunchOptionsDefault(v, item)
        }
    }

    private fun getActivityLaunchOptionsDefault(v: View?, item: ItemInfo?): ActivityOptionsWrapper {
        var left = 0
        var top = 0
        var width = v!!.measuredWidth
        var height = v.measuredHeight
        if (v is BubbleTextView) {
            // Launch from center of icon, not entire view
            val icon: Drawable? = v.icon
            if (icon != null) {
                val bounds = icon.bounds
                left = (width - bounds.width()) / 2
                top = v.getPaddingTop()
                width = bounds.width()
                height = bounds.height()
            }
        }
        val options = Utilities.allowBGLaunch(
            ActivityOptions.makeClipRevealAnimation(
                v,
                left,
                top,
                width,
                height,
            ),
        )
        options.setLaunchDisplayId(
            if (v != null && v.display != null) v.display.displayId else Display.DEFAULT_DISPLAY,
        )
        val callback = RunnableList()
        return ActivityOptionsWrapper(options, callback)
    }

    override fun onStart() {
        super.onStart()
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun onResume() {
        super.onResume()
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        restartIfPending()

        dragLayer.viewTreeObserver.addOnDrawListener(object : ViewTreeObserver.OnDrawListener {
            private var handled = false

            override fun onDraw() {
                if (handled) {
                    return
                }
                handled = true

                dragLayer.post {
                    dragLayer.viewTreeObserver.removeOnDrawListener(this)
                }
                depthController
            }
        })
    }

    override fun onPause() {
        super.onPause()
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override fun onStop() {
        super.onStop()
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        // Only actually closes if required, safe to call if not enabled
        SmartspacerClient.close()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        onBackPressedDispatcher.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        savedStateRegistryController.performSave(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (activityResultRegistry.dispatchResult(requestCode, resultCode, data)) {
            mPendingActivityRequestCode = -1
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun getDefaultOverlay(): LauncherOverlayManager = defaultOverlay

    fun recreateIfNotScheduled() {
        if (sRestartFlags == 0) {
            recreate()
        }
    }

    private fun restartIfPending() {
        when {
            sRestartFlags and FLAG_RESTART != 0 -> yitapApp.restart(false)
            sRestartFlags and FLAG_RECREATE != 0 -> {
                sRestartFlags = 0
                recreate()
            }
        }
    }

    /**
     * Reloads app icons if there is an active icon pack & [PreferenceManager2.alwaysReloadIcons] is enabled.
     */
    private fun reloadIconsIfNeeded() {
        if (
            preferenceManager2.alwaysReloadIcons.firstBlocking() &&
            (prefs.iconPackPackage.get().isNotEmpty() || prefs.themedIconPackPackage.get().isNotEmpty())
        ) {
            LauncherAppState.getInstance(this).reloadIcons()
        }
    }

    companion object {
        private const val FLAG_RECREATE = 1 shl 0
        private const val FLAG_RESTART = 1 shl 1

        var sRestartFlags = 0

        val instance get() = LauncherAppState.getInstanceNoCreate()?.launcher as? YitapLauncher
    }
}

val Context.launcher: YitapLauncher
    get() = BaseActivity.fromContext(this)

val Context.launcherNullable: YitapLauncher? get() = try {
    launcher
} catch (_: IllegalArgumentException) {
    null
}
