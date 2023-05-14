package com.feilongproject.baassetsdownloader.util


import java.util.*

class ThrottleProgress(private val updateInterval: Int) {

    private var nowTime = Date().time
    private var f = false

    @Synchronized
    fun update(force: Boolean, callback: () -> Unit) {
        if (f) return
        if (force) f = true
        if ((nowTime >= Date().time + updateInterval) && !force) return
        nowTime = Date().time
//        executor.execute {
        callback()
//        }
    }
}