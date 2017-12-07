// CHANGER LE NOM DU PACKAGE POUR UTILISER LE VOTRE (.g. c'est mon login, pas le votre)
package com.geekloper.borrownote

import android.Manifest
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.support.v7.widget.GridLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_detail.*
import org.jetbrains.anko.db.asMapSequence
import org.jetbrains.anko.db.select
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast
import java.io.File
import java.util.*
import android.Manifest.permission
import android.Manifest.permission.WRITE_CALENDAR
import android.app.SearchManager
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.net.NetworkInfo
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.DialogInterface
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.HttpURLConnection.HTTP_OK
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import android.widget.TextView
import android.os.AsyncTask
import android.os.BatteryManager
import android.support.v7.app.AlertDialog
import android.view.Menu
import android.widget.ImageView
import android.support.v7.widget.SearchView
import java.io.FileNotFoundException
import java.net.ConnectException
import java.net.UnknownHostException


class MainActivity : AppCompatActivity(), DetailFragment.Listener, ListeFragment.Listener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn_send.setOnClickListener {
            // Il faut qu'on soit prévenu quand l'activité Ajouter est finie pour pouvoir mettre à jour la liste du RecyclerView
            startActivityForResult<AjouterActivity>(1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            1 -> {
                // AjouterActivity, sur téléphone et tablette
                if (resultCode == RESULT_OK) {
                    // Est-ce qu'un message a réellement été ajouter ou est-ce qu el'utilisateur a annulé ?
                    val r = data?.getBooleanExtra(AjouterActivity.EXTRA_LIST_CHANGED, false) ?: false
                    if (r) {
                        // On RE-charge la liste des message depuis la base de données
                        val fragListe = fragmentManager.findFragmentById(R.id.frag_liste) as ListeFragment
                        fragListe.chargerListe()
                    }
                }
            }
            2 -> {
                // DetailActivity: sur téléphone uniquement (sur tablette, c'est onMessageDelete qui fait ça)
                if (resultCode == RESULT_OK) {
                    // Est-ce que le message affiché a été supprimé ou est-ce que l'utilisateur est juste revenu à la liste sans le modifier ?
                    val r = data?.getBooleanExtra(DetailActivity.EXTRA_LIST_CHANGED, false) ?: false
                    if (r) {
                        // On RE-charge la liste des message depuis la base de données
                        val fragListe = fragmentManager.findFragmentById(R.id.frag_liste) as ListeFragment
                        fragListe.chargerListe()
                    }
                }
            }
        }
    }

    override fun onMessageSelection(id: Long) {
        // Notre fragment de liste nous indique que l'utilisateur a cliqué sur un message

        // On regarde si le fargment Detail existe
        val fragDet = fragmentManager.findFragmentById(R.id.frag_detail) as DetailFragment?
        if(fragDet != null){
            // On est en layour sw600dp, c'est à dire qu'on est sur un tablette
            // On dit au fragment d'afficer le message avec l'id cliqué dans la liste
            fragDet.afficherDetail(id)
        }else{
            // Pas de fragment, on est sur le layout sans modifieur, donc sur téléphone
            // On lance une nouvelle activité Detail avec en paramètre l'id cliqué dans la liste
            startActivityForResult<DetailActivity>(2, DetailActivity.EXTRA_MESSAGE_ID to id)
        }
    }

    override fun onMessageDelete() {
        // Le fragment (on est obligatoirement sur tablette si cette fonction est appellée) nous informe que le message actuellement affiché est supprimé
        // On RE-charge la liste des message depuis la base de données
        val fragListe = fragmentManager.findFragmentById(R.id.frag_liste) as ListeFragment
        fragListe.chargerListe()

        // En attendant, on affiche "message introvable" dans le fragment en passant un id inexistant
        val fragDet = fragmentManager.findFragmentById(R.id.frag_detail) as DetailFragment
        fragDet.afficherDetail(-1)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)

        val searchView = menu?.findItem(R.id.action_search)?.actionView as SearchView
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))

        val item = menu?.findItem(R.id.menu_delete)
        item?.setVisible(false)

        return super.onCreateOptionsMenu(menu)
    }
}
