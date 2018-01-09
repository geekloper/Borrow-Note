package com.geekloper.borrownote

import android.Manifest
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.opengl.Visibility
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.SearchView
import android.util.Log
import android.view.Menu
import android.view.View
import kotlinx.android.synthetic.main.activity_recherche.*
import kotlinx.android.synthetic.main.fragment_liste.*
import org.jetbrains.anko.db.asMapSequence
import org.jetbrains.anko.db.select
import org.jetbrains.anko.startActivityForResult
import java.util.*

class RechercheActivity : AppCompatActivity() , DetailFragment.Listener, ListeFragment.Listener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recherche)

        if (intent.action == Intent.ACTION_SEARCH) {
            val query = intent.getStringExtra(SearchManager.QUERY)

            val fragListe = fragmentManager.findFragmentById(R.id.frag_liste) as ListeFragment
            fragListe.query = DBLivres.COLUMN_LIVRES_NOTE + " like '%" + query + "%' OR " + DBLivres.COLUMN_LIVRES_TITRE + " like '%" + query + "%'"
            fragListe.chargerListe()

            // Si on ne trouve aucun résultat on affiche le bouton rechercher sur internet
            if(fragListe.rv_liste.adapter.itemCount ==0 ){

                btn_suggest.visibility = View.VISIBLE
                tv_aucun.visibility = View.VISIBLE

                btn_suggest.setOnClickListener {

                    val ite = Intent(this , SuggestionActivity::class.java )

                    ite.putExtra(SuggestionActivity.EXTRA_QUERY , intent.getStringExtra(SearchManager.QUERY) )

                    startActivity(ite)

                }

            }
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
        // On n'affiche pas le menu
        return super.onCreateOptionsMenu(menu)
    }

}

