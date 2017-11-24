package com.geekloper.borrownote

import android.app.Fragment
import android.content.Context
import android.os.Bundle
import android.view.*
import kotlinx.android.synthetic.main.fragment_detail.*
import org.jetbrains.anko.db.asMapSequence
import org.jetbrains.anko.db.select
import org.jetbrains.anko.db.delete
import org.jetbrains.anko.toast
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
    var idMessage : Long = -1;

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        btn_delete.setOnClickListener {
            // On supprime le message actuellement affiché de la base de données
            activity.dbLivres.use {
                // TODO: penser à aussi supprimer son image si il en a une
                delete(DBLivres.TABLE_LIVRES,
                        "${DBLivres.TABLE_LIVRES_ID} = {id}",
                        "id" to idMessage)
            }
            // On prévient notre activité :
            // si on est sur tablette, ce fragment est directement dans MainActivity qui va mettre à la jour le RecyclerView
            // si on est sur téléphone, ce fragment est dans DetailActivity, c'est lui qui va prévenir MainActivity pour nous
            mListener!!.onMessageDelete()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_detail, container, false)
    }

    fun afficherDetail(id: Long) {
        // On sauvegarde l'id affiché
        idMessage = id

        // On charge le message de la base de données
        activity.dbLivres.use {
            select(DBLivres.TABLE_LIVRES, // Table
                    DBLivres.TABLE_LIVRES_ID, // Toutes les colones
                    DBLivres.COLUMN_LIVRES_TITRE,
                    DBLivres.COLUMN_LIVRES_DATE,
                    DBLivres.COLUMN_LIVRES_NOTE)
                    .whereArgs("${DBLivres.TABLE_LIVRES_ID} = {id}", // On va charger un message à la fois avec comme identifiant id
                            "id" to idMessage)
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
                                tv_note.text = row[DBLivres.COLUMN_LIVRES_NOTE] as String
                            }
                        }
                    }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menu_delete -> {
                // On supprime le message actuellement affiché de la base de données
                activity.dbLivres.use {
                    delete(DBLivres.TABLE_LIVRES,
                            "${DBLivres.TABLE_LIVRES_ID} = {id}",
                            "id" to idMessage)
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
