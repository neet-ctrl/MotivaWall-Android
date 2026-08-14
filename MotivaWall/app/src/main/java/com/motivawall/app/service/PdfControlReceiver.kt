package com.motivawall.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Manifest-declared bridge for notification/system actions.
 * The service still owns its private in-process receiver for UI updates.
 */
class PdfControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        ContextCompat.startForegroundService(
            context,
            Intent(context, PdfLockScreenDialogService::class.java)
                .setAction(action)
                .putExtras(intent)
        )
    }
}