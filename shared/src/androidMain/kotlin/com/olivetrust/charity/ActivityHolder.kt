package com.olivetrust.charity

import android.app.Activity
import java.lang.ref.WeakReference

object ActivityHolder {
    private var activityRef: WeakReference<Activity>? = null

    fun init(activity: Activity) {
        this.activityRef = WeakReference(activity)
    }

    fun get(): Activity? = activityRef?.get()
}
