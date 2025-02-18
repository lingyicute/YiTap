package app.yitap.overview

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Space
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import app.yitap.preferences.PreferenceManager
import app.yitap.util.RecentHelper
import app.yitap.util.isDefaultLauncher
import app.yitap.util.isOnePlusStock
import com.android.launcher3.Launcher
import com.android.launcher3.LauncherState
import com.android.launcher3.R
import com.android.launcher3.statemanager.StateManager
import com.android.quickstep.views.OverviewActionsView
import com.android.quickstep.views.RecentsView
import com.android.systemui.shared.recents.model.Task

class YitapOverviewActionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : OverviewActionsView<TaskOverlayFactoryImpl.OverlayUICallbacks>(context, attrs, defStyleAttr) {

    private val prefs = PreferenceManager.getInstance(context)
    private val launcher: Launcher? = if (context.isDefaultLauncher()) Launcher.getLauncher(context) else null
    private lateinit var container: LinearLayout
    private lateinit var screenshotAction: Button
    private lateinit var shareAction: Button
    private lateinit var lensAction: Button
    private lateinit var clearAllAction: Button
    private lateinit var lockedAction: Button
    private var rv: RecentsView<Launcher, *>? = null

    private val lockedTaskStateLister = object : StateManager.StateListener<LauncherState> {
        override fun onStateTransitionStart(toState: LauncherState) {
            rv = launcher?.getOverviewPanel()
        }

        override fun onStateTransitionComplete(finalState: LauncherState) {
            if (finalState.overviewUi && context.isDefaultLauncher()) {
                rv?.addOnScrollChangedListener {
                    val task = rv?.getCurrentPageTaskView()?.task
                    task?.let { updateLockedActionState(it) }
                }
            }
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        container = ViewCompat.requireViewById(this, R.id.action_buttons)
        clearAllAction = ViewCompat.requireViewById(this, R.id.action_clear_all)
        shareAction = ViewCompat.requireViewById(this, R.id.action_share)
        lensAction = ViewCompat.requireViewById(this, R.id.action_lens)
        screenshotAction = ViewCompat.requireViewById(this, R.id.action_screenshot)
        lockedAction = ViewCompat.requireViewById(this, R.id.action_locked)

        shareAction.setOnClickListener { mCallbacks?.onShare() }
        lensAction.setOnClickListener { mCallbacks?.onLens() }
        if (context.isDefaultLauncher()) {
            lockedAction.setOnClickListener {
                rv = launcher?.getOverviewPanel()
                val task = rv?.getCurrentPageTaskView()?.task
                if (task != null) {
                    mCallbacks?.onLocked(context, task)
                    updateLockedActionState(task)
                }
            }
        }

        prefs.recentsActionClearAll.subscribeChanges(this, ::updateVisibilities)
        prefs.recentsActionLens.subscribeChanges(this, ::updateVisibilities)
        prefs.recentsActionScreenshot.subscribeChanges(this, ::updateVisibilities)
        prefs.recentsActionShare.subscribeChanges(this, ::updateVisibilities)
        prefs.recentsActionLocked.subscribeChanges(this, ::updateVisibilities)

        updateVisibilities()
    }

    private fun updateVisibilities() {
        val buttons = mutableListOf<View>()
        if (prefs.recentsActionScreenshot.get() && !isOnePlusStock) {
            buttons.add(screenshotAction)
        }
        if (prefs.recentsActionShare.get()) {
            buttons.add(shareAction)
        }
        if (prefs.recentsActionLens.get() && isLensAvailable()) {
            buttons.add(lensAction)
        }
        if (prefs.recentsActionLocked.get()) {
            buttons.add(lockedAction)
        }
        if (prefs.recentsActionClearAll.get()) {
            buttons.add(clearAllAction)
        }
        container.removeAllViews()
        container.addView(createSpace())
        buttons.forEach { view ->
            view.isVisible = true
            container.addView(view)
            container.addView(createSpace())
        }
    }

    private fun isLensAvailable(): Boolean {
        val lensIntent = context.packageManager.getLaunchIntentForPackage("com.google.ar.lens")
        return lensIntent != null
    }

    private fun updateLockedActionState(task: Task) {
        lockedAction.post {
            val isLocked = RecentHelper.isAppLocked(task.key.packageName, context)
            val drawableResId = if (isLocked) R.drawable.ic_unlocked_recents else R.drawable.ic_locked_recents
            val textResId = if (isLocked) R.string.action_unlock else R.string.action_lock

            val drawable = ContextCompat.getDrawable(context, drawableResId)
            lockedAction.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
            lockedAction.setText(textResId)
        }
    }

    private fun createSpace(): View {
        return Space(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1).apply { weight = 1f }
        }
    }

    override fun setClearAllClickListener(clearAllClickListener: OnClickListener?) {
        clearAllAction.setOnClickListener(clearAllClickListener)
    }

    override fun removeOnAttachStateChangeListener(listener: OnAttachStateChangeListener?) {
        super.removeOnAttachStateChangeListener(listener)
        if (context.isDefaultLauncher() && prefs.recentsActionLocked.get()) {
            launcher?.stateManager?.removeStateListener(lockedTaskStateLister)
        }
    }

    @Synchronized
    override fun addOnAttachStateChangeListener(listener: OnAttachStateChangeListener?) {
        super.addOnAttachStateChangeListener(listener)
        if (context.isDefaultLauncher() && prefs.recentsActionLocked.get()) {
            launcher?.stateManager?.addStateListener(lockedTaskStateLister)
        }
    }
}
