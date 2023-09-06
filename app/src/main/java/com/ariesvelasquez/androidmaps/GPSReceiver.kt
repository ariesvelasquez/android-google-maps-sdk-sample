package com.ariesvelasquez.androidmaps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager

class LocationToggleReceiver constructor(private val callback: CallbackListener) :
    BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ("android.location.PROVIDERS_CHANGED")) {
            callback.onLocationSettingsChanged()
        }
    }

    interface CallbackListener {
        fun onLocationSettingsChanged()
    }

    companion object {
        val intentFilter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
    }
}