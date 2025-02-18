package app.yitap.util

import android.view.View

fun onAttachStateChangeListener(callback: (isAttached: Boolean) -> Unit) = object : View.OnAttachStateChangeListener {
    override fun onViewAttachedToWindow(v: View) = callback(true)
    override fun onViewDetachedFromWindow(v: View) = callback(false)
}

fun View.observeAttachedState(callback: (isAttached: Boolean) -> Unit): () -> Unit {
    var wasAttached = false
    val listener = onAttachStateChangeListener { isAttached ->
        if (wasAttached != isAttached) {
            wasAttached = isAttached
            callback(isAttached)
        }
    }
    addOnAttachStateChangeListener(listener)
    if (isAttachedToWindow) {
        listener.onViewAttachedToWindow(this)
    }
    return { removeOnAttachStateChangeListener(listener) }
}
