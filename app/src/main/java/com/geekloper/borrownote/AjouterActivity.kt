package com.geekloper.borrownote

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.activity_ajouter.*
import org.jetbrains.anko.db.insert
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast
import java.util.*
import android.Manifest
import java.io.File


class AjouterActivity : AppCompatActivity() , GoogleApiClient.ConnectionCallbacks , GoogleApiClient.OnConnectionFailedListener {
    companion object {
        const val EXTRA_LIST_CHANGED = "AjouterActivity.EXTRA_LIST_CHANGED"
        const val EXTRA_NOM_LIVRE = "AjouterActivity.EXTRA_NOM_LIVRE"
        const val EXTRA_DATE = "AjouterActivity.EXTRA_DATE"
        const val EXTRA_NOTE = "AjouterActivity.EXTRA_NOTE"
        const val EXTRA_ADRESS_LATITUDE = "AjouterActivity.EXTRA_ADRESS_LATITUDE"
        const val EXTRA_ADRESS_LONGTITUDE = "AjouterActivity.EXTRA_ADRESS_LONGTITUDE"
    }

    lateinit var apiClient: GoogleApiClient
    var gapiOkay = false // initialement pas okay
    var filePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajouter)

        apiClient = GoogleApiClient.Builder(this).addApi(LocationServices.API)
                .addConnectionCallbacks(this).addOnConnectionFailedListener(this).build()

        btn_send.setOnClickListener {
            Ajouter_Livre()
        }

        btn_schedule.setOnClickListener {

            val mgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager

            //Envoyer les information dans le PendingIntent
            val intent = Intent(this, AlarmReceiver::class.java)
            intent.putExtra(EXTRA_NOM_LIVRE, txt_nom_livre.text.toString())
            intent.putExtra(EXTRA_NOTE, txt_note.text.toString())
            intent.putExtra(EXTRA_DATE, txt_date.text.toString())
            intent.putExtra(EXTRA_ADRESS_LATITUDE,  get_location()?.get(0))
            intent.putExtra(EXTRA_ADRESS_LONGTITUDE,  get_location()?.get(1))


            val alarmIntent = PendingIntent.getBroadcast(this, 42, intent, 0)
            val calendar = Calendar.getInstance()

            val date:Date = Date(txt_date.text.toString())
            calendar.time = date

            // La date d'emprun à 7:00
            calendar.set(Calendar.HOUR_OF_DAY, 7)
            calendar.set(Calendar.MINUTE, 0)
            calendar.add(Calendar.SECOND, 0)

            mgr.set(AlarmManager.RTC_WAKEUP,calendar.timeInMillis,alarmIntent)

            toast(getString(R.string.livre_ajoute))

            finish()

        }

        btn_take_picture.setOnClickListener {

            // Checker la permission WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)

            // Checker la permission CAMERA
            else if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)

            else {
                val intentPhoto = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues())
                intentPhoto.putExtra(MediaStore.EXTRA_OUTPUT, uri)

                with(contentResolver.query(uri, arrayOf(android.provider.MediaStore.Images.ImageColumns.DATA), null, null, null)) {
                    moveToFirst()
                    filePath = getString(0)
                    close()
                }

                if (intentPhoto.resolveActivity(packageManager) != null) {
                    startActivityForResult(intentPhoto, 10)
                }
            }
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
            10 -> {
                if(resultCode == RESULT_OK){
                    iv_book_preview.setImageBitmap(BitmapFactory.decodeFile(filePath))
                }
            }
        }
    }

    private fun Ajouter_Livre(){
        dbLivres.use {
            insert(DBLivres.TABLE_LIVRES,
                    DBLivres.COLUMN_LIVRES_NOTE to txt_note.text.toString(),
                    DBLivres.COLUMN_LIVRES_TITRE to txt_nom_livre.text.toString(),
                    DBLivres.COLUMN_LIVRES_DATE to Date(txt_date.text.toString()).time,
                    DBLivres.COLUMN_LIVRES_ADRESS_LATITUDE to get_location()?.get(0),
                    DBLivres.COLUMN_LIVRES_ADRESS_LONGTITUDE to get_location()?.get(1),
                    DBLivres.COLUMN_LIVRES_IMAGE to filePath)
        }
        toast(R.string.livre_ajoute)
        val retIntent = Intent()
        retIntent.putExtra(EXTRA_LIST_CHANGED, true)
        setResult(Activity.RESULT_OK, retIntent)
        finish()
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
