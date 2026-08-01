package com.olivetrust.charity

import android.app.Activity
import java.lang.ref.WeakReference

object ActivityHolder {
    private var activityRef: WeakReference<Activity>? = null
    private var filePickerProvider: FilePickerProvider? = null

    interface FilePickerProvider {
        fun pickImage(callback: (ByteArray?) -> Unit)
        fun pickImageOrPdf(callback: (ByteArray?) -> Unit)
    }

    fun init(activity: Activity) {
        this.activityRef = WeakReference(activity)
    }

    fun setFilePickerProvider(provider: FilePickerProvider?) {
        this.filePickerProvider = provider
    }

    fun get(): Activity? = activityRef?.get()
    fun getPicker(): FilePickerProvider? = filePickerProvider
}
