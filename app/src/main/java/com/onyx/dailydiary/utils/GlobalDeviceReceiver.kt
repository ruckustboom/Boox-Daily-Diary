package com.onyx.dailydiary.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.onyx.android.sdk.utils.BroadcastHelper
import com.onyx.android.sdk.utils.DeviceReceiver

class GlobalDeviceReceiver : BroadcastReceiver() {
    var callbacks: Callbacks? = null

    fun enable(context: Context, enable: Boolean) {
        try {
            if (enable) {
                BroadcastHelper.ensureRegisterReceiver(context, this, intentFilter())
            } else {
                BroadcastHelper.ensureUnregisterReceiver(context, this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun intentFilter(): IntentFilter = IntentFilter().apply {
        addAction(DeviceReceiver.SYSTEM_UI_DIALOG_OPEN_ACTION)
        addAction(DeviceReceiver.SYSTEM_UI_DIALOG_CLOSE_ACTION)
        addAction(Intent.ACTION_SCREEN_ON)
    }

    private fun handleSystemUIDialogAction(intent: Intent) {
        val action = intent.action
        val dialogType = intent.getStringExtra(DeviceReceiver.DIALOG_TYPE)
        val open: Boolean
        if (dialogType == DeviceReceiver.DIALOG_TYPE_NOTIFICATION_PANEL) {
            open = when (action) {
                DeviceReceiver.SYSTEM_UI_DIALOG_OPEN_ACTION -> true
                DeviceReceiver.SYSTEM_UI_DIALOG_CLOSE_ACTION -> false
                else -> false
            }
            callbacks?.onNotificationPanel(open)
        }
    }

    private fun handSystemScreenOnAction() {
        callbacks?.onScreenOn()
    }

    private fun handSystemScreenOffAction() {
        callbacks?.onScreenOff()
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        when (action) {
            DeviceReceiver.SYSTEM_UI_DIALOG_OPEN_ACTION, DeviceReceiver.SYSTEM_UI_DIALOG_CLOSE_ACTION ->
                handleSystemUIDialogAction(intent)

            Intent.ACTION_SCREEN_ON -> handSystemScreenOnAction()
            Intent.ACTION_SCREEN_OFF -> handSystemScreenOffAction()
        }
    }

    interface Callbacks {
        fun onNotificationPanel(open: Boolean)
        fun onScreenOn()
        fun onScreenOff()
    }
}