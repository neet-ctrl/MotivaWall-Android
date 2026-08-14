package com.motivawall.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            context.sendBroadcast(Intent("com.motivawall.app.RESTORE_SCHEDULES").setPackage(context.packageName))
        }
    }
}