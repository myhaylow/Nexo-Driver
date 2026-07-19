package com.myhaylow.nexodriver

import android.app.Application
import android.content.Context
import com.myhaylow.nexodriver.profile.ProfileRepository
import com.myhaylow.nexodriver.profile.RuntimeConfiguration

class NexoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeRuntime(this)
    }

    companion object {
        /** Safe to call from process components, including a service restored before any Activity. */
        fun initializeRuntime(context: Context) {
            RuntimeConfiguration.initializeIfNeeded { ProfileRepository(context.applicationContext) }
        }
    }
}
