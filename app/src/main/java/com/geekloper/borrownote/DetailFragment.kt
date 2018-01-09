package com.geekloper.borrownote

import android.app.Activity.RESULT_OK
import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.*
import kotlinx.android.synthetic.main.fragment_detail.*
import org.jetbrains.anko.db.asMapSequence
import org.jetbrains.anko.db.select
import org.jetbrains.anko.db.delete
import org.jetbrains.anko.sendSMS
import org.jetbrains.anko.toast
import java.io.File
import java.util.*


class DetailFragment : Fragment(){
    interface Listener {
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

    var idLivre : Long = -1;

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_detail, container, false)
    }

    fun afficherDetail(id: Long) {

        idLivre = id


        activity.dbLivres.use {
            select(DBLivres.TABLE_LIVRES,
                    DBLivres.TABLE_LIVRES_ID,
                    DBLivres.COLUMN_LIVRES_TITRE,
                    DBLivres.COLUMN_LIVRES_DATE,
                    DBLivres.COLUMN_LIVRES_NOTE,
                    DBLivres.COLUMN_LIVRES_IMAGE)
                    .whereArgs("${DBLivres.TABLE_LIVRES_ID} = {id}",
                            "id" to idLivre)
                    .exec {
                        if(count == 0){
                            // Au cas où il n'y est aucun Livre
                            val am = activity.getString(R.string.aucun_livre)
                            tv_nom_livre.text = am
                            tv_date.text = am
                            tv_note.text = am
                        }else {

                            for (row in asMapSequence()) {

                                tv_nom_livre.text = row[DBLivres.COLUMN_LIVRES_TITRE] as String
                                val date = Date()
                                date.time = row[DBLivres.COLUMN_LIVRES_DATE] as Long
                                tv_date.text = date.toLocaleString()
                                tv_note.text = row[DBLivres.COLUMN_LIVRES_NOTE] as String

                                if(row[DBLivres.COLUMN_LIVRES_IMAGE] != null)
                                    iv_detail.setImageBitmap((BitmapFactory.decodeFile(row[DBLivres.COLUMN_LIVRES_IMAGE] as String)))
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
            R.id.action_share -> {

                val intentContact = Intent(Intent.ACTION_PICK)
                intentContact.type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                if (intentContact.resolveActivity(activity.packageManager) != null) {
                    startActivityForResult(intentContact, 42)
                }

            }
        }
            return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){

            42 -> {

                if(resultCode == RESULT_OK) {
                    with(activity.contentResolver.query(data?.data,arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),null, null, null)) {
                        moveToFirst()
                        val telephone = getString(0)

                        sendSMS(telephone, "Livre : ${tv_nom_livre.text}" )
                    }
                }

            }

        }

    }
}
