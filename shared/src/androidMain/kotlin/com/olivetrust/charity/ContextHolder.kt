package com.olivetrust.charity

import android.content.Context

object ContextHolder {
    private var context: Context? = null

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    fun get(): Context? = context
}
