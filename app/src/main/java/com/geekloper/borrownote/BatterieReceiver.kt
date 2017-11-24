package com.geekloper.borrownote

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import org.jetbrains.anko.toast



class BatterieReceiver : BroadcastReceiver() {
    var desactive = false

    override fun onReceive(context: Context, intent: Intent) {
        if(intent.action == Intent.ACTION_BATTERY_LOW) {
            context.toast(R.string.batterie_faible)
            desactive = true
        } else if(intent.action == Intent.ACTION_BATTERY_OKAY){
            context.toast(R.string.batterie_ok)
            desactive = false
        }
    }
}
