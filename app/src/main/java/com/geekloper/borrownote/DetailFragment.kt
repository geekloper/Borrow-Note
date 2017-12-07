package com.geekloper.borrownote

import android.app.Fragment
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import kotlinx.android.synthetic.main.fragment_detail.*
import org.jetbrains.anko.db.asMapSequence
import org.jetbrains.anko.db.select
import org.jetbrains.anko.db.delete
import org.jetbrains.anko.toast
import java.io.File
import java.util.*


class DetailFragment : Fragment(){
    interface Listener {
        // Quand on supprime un message, il faut prévenir MainActivity de mettre à jour la liste
        fun onMessageDelete()
    }

    var mListener: Listener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mListener = context as Listener
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString() + " must implement DetailFragment.Listener")
        }
    }

    // L'id du message actuellement affiché
    var idLivre : Long = -1;

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_detail, container, false)
    }

    fun afficherDetail(id: Long) {
        // On sauvegarde l'id affiché
        idLivre = id

        // On charge le message de la base de données
        activity.dbLivres.use {
            select(DBLivres.TABLE_LIVRES, // Table
                    DBLivres.TABLE_LIVRES_ID, // Toutes les colones
                    DBLivres.COLUMN_LIVRES_TITRE,
                    DBLivres.COLUMN_LIVRES_DATE,
                    DBLivres.COLUMN_LIVRES_NOTE,
                    DBLivres.COLUMN_LIVRES_ADRESS_LATITUDE,
                    DBLivres.COLUMN_LIVRES_ADRESS_LONGTITUDE)
                    .whereArgs("${DBLivres.TABLE_LIVRES_ID} = {id}", // On va charger un message à la fois avec comme identifiant id
                            "id" to idLivre)
                    .exec {
                        if(count == 0){
                            // Au cas où il n'y est aucun message
                            val am = activity.getString(R.string.aucun_livre)
                            tv_pk.text = am
                            tv_nom_livre.text = am
                            tv_date.text = am
                            tv_note.text = am
                        }else {
                            // 1 message maximum retourné, parce que les id sont uniques (PK)
                            for (row in asMapSequence()) {
                                // On ne passe qu'une seule fois dans le for du coup
                                tv_pk.text = (row[DBLivres.TABLE_LIVRES_ID] as Long).toString()
                                tv_nom_livre.text = row[DBLivres.COLUMN_LIVRES_TITRE] as String
                                val date = Date()
                                date.time = row[DBLivres.COLUMN_LIVRES_DATE] as Long
                                tv_date.text = date.toLocaleString()
                                tv_location.text = row[DBLivres.COLUMN_LIVRES_ADRESS_LATITUDE] as String? +  row[DBLivres.COLUMN_LIVRES_ADRESS_LONGTITUDE] as String?
                                tv_note.text = row[DBLivres.COLUMN_LIVRES_NOTE] as String
                            }
                        }
                    }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.menu, menu)
        val item = menu?.findItem(R.id.action_search)
        item?.setVisible(false)
        super.onCreateOptionsMenu(menu, inflater)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menu_delete -> {

                //Supprimer la photo
                activity.dbLivres.use{
                    select(DBLivres.TABLE_LIVRES , DBLivres.COLUMN_LIVRES_IMAGE)
                            .whereArgs("${DBLivres.TABLE_LIVRES_ID} = {id}","id" to idLivre)
                            .exec {

                                for (row in asMapSequence()) {
                                    if(row[DBLivres.COLUMN_LIVRES_IMAGE ] != null){
                                        Log.e("LOG : ",row[DBLivres.COLUMN_LIVRES_IMAGE] as String)
                                        var image = File(row[DBLivres.COLUMN_LIVRES_IMAGE] as String)
                                        toast( image.delete().toString())
                                    }
                                }

                            }
                }

                activity.dbLivres.use {
                    delete(DBLivres.TABLE_LIVRES,
                            "${DBLivres.TABLE_LIVRES_ID} = {id}",
                            "id" to idLivre)
                }
                // On prévient notre activité :
                // si on est sur tablette, ce fragment est directement dans MainActivity qui va mettre à la jour le RecyclerView
                // si on est sur téléphone, ce fragment est dans DetailActivity, c'est lui qui va prévenir MainActivity pour nous
                mListener!!.onMessageDelete()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
}
