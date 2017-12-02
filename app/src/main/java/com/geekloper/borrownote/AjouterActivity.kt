package com.geekloper.borrownote

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.activity_ajouter.*
import org.jetbrains.anko.db.insert
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast
import java.util.*

// Le code de cette activity provient pour beaucoup de l'ancien code de MainActivity
class AjouterActivity : AppCompatActivity() , GoogleApiClient.ConnectionCallbacks , GoogleApiClient.OnConnectionFailedListener {
    companion object {
        const val EXTRA_LIST_CHANGED = "AjouterActivity.EXTRA_LIST_CHANGED"
        const val EXTRA_NOM_LIVRE = "AjouterActivity.EXTRA_NOM_LIVRE"
        const val EXTRA_DATE = "AjouterActivity.EXTRA_DATE"
        const val EXTRA_NOTE = "AjouterActivity.EXTRA_NOTE"
    }

    lateinit var apiClient: GoogleApiClient
    var gapiOkay = false // initialement pas okay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajouter)

        apiClient = GoogleApiClient.Builder(this).addApi(LocationServices.API)
                .addConnectionCallbacks(this).addOnConnectionFailedListener(this).build()

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

        //Désactiver/activer txt_adresse_specifique en fontion de l'etat du radio button  rb_adresse_specifique
        rb_adresse_specifique.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked)
                txt_adresse_specifique.isEnabled = true
            else
                txt_adresse_specifique.isEnabled = false

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
                                        DBLivres.COLUMN_LIVRES_DATE to Date(txt_date.text.toString()).time,
                                        DBLivres.COLUMN_LIVRES_ADRESS_LATITUDE to get_location()?.get(0),
                                        DBLivres.COLUMN_LIVRES_ADRESS_LONGTITUDE to get_location()?.get(1))
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


    fun get_location(): Array<String>? {

        if ( gapiOkay && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            if(rb_postion_actuelle.isChecked){
                val pos = LocationServices.FusedLocationApi.getLastLocation(apiClient)
                return arrayOf(pos.latitude.toString(),pos.longitude.toString())
            }
            else if(rb_adresse_specifique.isChecked){
                val geocoder = Geocoder(this)
                val addresses = geocoder.getFromLocationName(txt_adresse_specifique.text.toString(), 1)

                if (addresses != null && addresses.size > 0){
                    return arrayOf(addresses[0].latitude.toString(),addresses[0].longitude.toString())
                }
            }
        }

        return null
    }

    //region Gapi
    override fun onStart() {
        super.onStart()
        apiClient.connect()

    }

    override fun onStop() {
        super.onStop()
        apiClient.disconnect()
    }

    override fun onConnected(p0: Bundle?) {
        gapiOkay = true
    }

    override fun onConnectionSuspended(p0: Int) {
        gapiOkay = false
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        toast("Connection to Google API Failed : ${p0.errorMessage} , ${p0.errorCode} ")
    }
    //endregion
}
