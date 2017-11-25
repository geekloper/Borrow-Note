package com.geekloper.borrownote

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_ajouter.*
import org.jetbrains.anko.db.insert
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast
import java.util.*

// Le code de cette activity provient pour beaucoup de l'ancien code de MainActivity
class AjouterActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_LIST_CHANGED = "AjouterActivity.EXTRA_LIST_CHANGED"
        const val EXTRA_NOM_LIVRE = "AjouterActivity.EXTRA_NOM_LIVRE"
        const val EXTRA_DATE = "AjouterActivity.EXTRA_DATE"
        const val EXTRA_NOTE = "AjouterActivity.EXTRA_NOTE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajouter)

        btn_send.setOnClickListener {
            startActivityForResult<ConfirmationActivity>(42, ConfirmationActivity.EXTRA_MESSAGE to txt_note.text.toString())
        }

        btn_schedule.setOnClickListener {

            val mgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager

            //Envoyer les information dans le PendingIntent
            val intent = Intent(this, AlarmReceiver::class.java)
            intent.putExtra(EXTRA_NOM_LIVRE, txt_nom_livre.text.toString())
            intent.putExtra(EXTRA_NOTE, txt_note.text.toString())
            intent.putExtra(EXTRA_DATE, txt_date.text.toString())

            val alarmIntent = PendingIntent.getBroadcast(this, 42, intent, 0)
            val calendar = Calendar.getInstance()

            val date:Date = Date(txt_date.text.toString())
            calendar.time = date

            // La date de retour à 7:00
            calendar.set(Calendar.HOUR_OF_DAY, 7)
            calendar.set(Calendar.MINUTE, 0)
            calendar.add(Calendar.SECOND, 0)

            mgr.set(AlarmManager.RTC_WAKEUP,calendar.timeInMillis,alarmIntent)

            toast(getString(R.string.livre_ajoute))

            finish()

        }
    }

    override fun onActivityResult(requestCode: Int,
                                  resultCode: Int,
                                  data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            42 -> {
                if (resultCode == RESULT_OK) {
                    val r = data?.getIntExtra(ConfirmationActivity.EXTRA_ISCONFIRMED, ConfirmationActivity.VAL_CANCEL) ?: ConfirmationActivity.VAL_CANCEL
                    when(r){
                        ConfirmationActivity.VAL_CONFIRMED -> {
                            dbLivres.use {
                                insert(DBLivres.TABLE_LIVRES,
                                        DBLivres.COLUMN_LIVRES_NOTE to txt_note.text.toString(),
                                        DBLivres.COLUMN_LIVRES_TITRE to txt_nom_livre.text.toString(),
                                        DBLivres.COLUMN_LIVRES_DATE to Date(txt_date.text.toString()).time)
                            }
                            toast(R.string.livre_ajoute)
                            // On prévient MainActivity que la liste a changé
                            val retIntent = Intent()
                            retIntent.putExtra(EXTRA_LIST_CHANGED, true)
                            setResult(Activity.RESULT_OK, retIntent)
                            // Et on ferme l'écran de saisie et on revient à MainActivity
                            // Ca va aussi effacer tous les champs
                            finish()
                        }
                        ConfirmationActivity.VAL_EDIT -> { } // rien de spécial à faire
                        ConfirmationActivity.VAL_CANCEL -> {
                            // On prévient MainActivity que la liste n'a PAS changé
                            val retIntent = Intent()
                            retIntent.putExtra(EXTRA_LIST_CHANGED, false)
                            setResult(Activity.RESULT_OK, retIntent)
                            finish()
                        }
                    }
                } else if(resultCode == RESULT_CANCELED) {
                    // on va considérer que le bouton "retour" du téléphone est équivalent à notre bouton "modifier"
                    // comme ci-dessus, rien de spécial à faire du coup
                }
            }
        }
    }
}
