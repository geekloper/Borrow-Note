package com.geekloper.borrownote

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.provider.SyncStateContract.Helpers.insert
import android.support.v4.app.NotificationCompat
import org.jetbrains.anko.db.insert
import java.util.*

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val nom_livre = intent.getStringExtra(AjouterActivity.EXTRA_NOM_LIVRE)
        val date = intent.getStringExtra(AjouterActivity.EXTRA_DATE)
        val note = intent.getStringExtra(AjouterActivity.EXTRA_NOTE)
        val latitude = intent.getStringArrayExtra(AjouterActivity.EXTRA_ADRESS_LATITUDE)
        val longtitude= intent.getStringExtra(AjouterActivity.EXTRA_ADRESS_LONGTITUDE)

         context.dbLivres.use {
            insert(DBLivres.TABLE_LIVRES,
                    DBLivres.COLUMN_LIVRES_NOTE to note,
                    DBLivres.COLUMN_LIVRES_TITRE to nom_livre,
                    DBLivres.COLUMN_LIVRES_DATE to Date(date).time,
                    DBLivres.COLUMN_LIVRES_ADRESS_LATITUDE to latitude,
                    DBLivres.COLUMN_LIVRES_ADRESS_LONGTITUDE to longtitude)
        }

        val notif = NotificationCompat.Builder(context , "msg")
                .setSmallIcon(R.drawable.pas_d_image)
                .setContentTitle("Livre ajouté")
                .setContentText("Livre ajouté")
        val notificationId = 1
        val mgr = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(notificationId, notif.build())
    }
}
