package com.geekloper.borrownote

import android.Manifest
import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_liste.*
import org.jetbrains.anko.db.asMapSequence
import org.jetbrains.anko.db.select
import org.jetbrains.anko.db.update
import org.jetbrains.anko.toast
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException
import java.nio.file.Files
import java.util.*


class ListeFragment : Fragment(){
    interface Listener {
        // Quand l'utilisateur clique sur un message, on prévient l'activity
        fun onMessageSelection(id: Long)
    }

    var mListener: Listener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mListener = context as Listener
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString() + " must implement ListeFragment.Listener")
        }
    }

    // Pour résoudre un bug avec les anciennes versions d'android
    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        try {
            mListener = activity as Listener
        } catch (e: ClassCastException) {
            throw ClassCastException(activity.toString() + " must implement ListeFragment.Listener")
        }
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // On charge la liste des livres depuis la base de données
        chargerListe()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_liste, container, false)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
    var query : String = ""
    fun chargerListe() {

        val listeLivres = arrayListOf<LivreRVD>()

        activity.dbLivres.use {
            select(DBLivres.TABLE_LIVRES,
                    DBLivres.TABLE_LIVRES_ID,
                    DBLivres.COLUMN_LIVRES_TITRE,
                    DBLivres.COLUMN_LIVRES_DATE,
                    DBLivres.COLUMN_LIVRES_IMAGE).whereArgs(query).exec {

                for (row in asMapSequence()) {
                    val date = Date()
                    date.time = row[DBLivres.COLUMN_LIVRES_DATE] as Long
                    listeLivres.add(LivreRVD(
                            row[DBLivres.TABLE_LIVRES_ID] as Long,
                            row[DBLivres.COLUMN_LIVRES_TITRE] as String,
                            date,
                            row[DBLivres.COLUMN_LIVRES_IMAGE] as String?
                    ))
                }
            }
        }

        rv_liste.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        rv_liste.adapter = LivreRecyclerAdapter(listeLivres){
            // On prévient MainActivity que l'utilisateur a cliqué sur un livre
            mListener!!.onMessageSelection(it.id)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                // 1 = les permissions réseaux. Normalement c'est automatique par dérogation, mais on ne sait jamais
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission accordée
                    chargerListe()
                } else {
                    // Permission refusées par l'utilisateur
                    // Ici on affiche une boite de dialogue avec un bouton Ok. A la place on pourrait afficher juste un toast
                    val msg = AlertDialog.Builder(activity)
                    msg.setMessage(R.string.pas_permission_reseau)
                    msg.setTitle(R.string.app_name)
                    msg.setPositiveButton(R.string.ok, DialogInterface.OnClickListener { _, _ ->  })
                    msg.setCancelable(true)
                    msg.create().show()
                }
            }
        }
    }
}
