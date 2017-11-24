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

    // Le batterie receiver dynamique. Comme on l'utilise pour désactiver une fonction de notre appli,
    // on ne s'intéresse au messages que quand notre appli est active (et pas quand elle n'est pas active)
    // donc on fait un receiver dynamique qu'on enregistre dans onCreate et supprime dans onDestroy
    // plutôt qu'un receiver statique de serait toujours actif
    val batRcv = BatterieReceiver()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        activity.registerReceiver(batRcv, IntentFilter(Intent.ACTION_BATTERY_LOW))
        activity.registerReceiver(batRcv, IntentFilter(Intent.ACTION_BATTERY_OKAY))

        // On charge la liste des message depuis la base de données
        chargerListe()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_liste, container, false)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Ce qui est initialisé dans onCreate doit être déinitiliasé dans onDestroy
        activity.unregisterReceiver(batRcv)
    }
    var query : String = ""
    fun chargerListe() {
        // Les messages qui sont affichés dans le RecyclerView
        val listeLivres = arrayListOf<LivreRVD>()

        // On select sur la base de données
        activity.dbLivres.use {
            select(DBLivres.TABLE_LIVRES, // Table
                    DBLivres.TABLE_LIVRES_ID, // Les colones de MessageRVD uniquement
                    DBLivres.COLUMN_LIVRES_TITRE,
                    DBLivres.COLUMN_LIVRES_DATE,
                    DBLivres.COLUMN_LIVRES_IMAGE).whereArgs(query).exec {
                // Pas de Where, on veut tous les messages
                for (row in asMapSequence()) {
                    val date = Date()
                    date.time = row[DBLivres.COLUMN_LIVRES_DATE] as Long
                    listeLivres.add(LivreRVD(
                            row[DBLivres.TABLE_LIVRES_ID] as Long,
                            row[DBLivres.COLUMN_LIVRES_TITRE] as String,
                            date,
                            row[DBLivres.COLUMN_LIVRES_IMAGE] as String?
                    ))
                    // Si on a une image vide, on télécharge une nouvelle image
                    // - soit on vient d'ajouter cet item
                    // - soit la fonction était désactivée (batterie faible)
                    // - soit pas d'internet avant (ni wifi ni 3G/4G)
                    // - soit anciens messages du TP4, avant qu'on ajoute cette fonction
                    // - autres cas ?
                    if(row[DBLivres.COLUMN_LIVRES_IMAGE] == null)
                        telechargerImage(row[DBLivres.TABLE_LIVRES_ID] as Long)
                }
            }
        }

        rv_liste.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        rv_liste.adapter = LivreRecyclerAdapter(listeLivres){
            // On prévient MainActivity que l'utilisateur a cliqué sur un message
            mListener!!.onMessageSelection(it.id)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                // 1 = les permissions réseaux. Normalement c'est automatique par dérogation, mais on ne sait jamais
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission accordée, il faut maintenant relancer le traitement qui necéssitait cette permission
                    // On recharge la liste pour trouver (dans la boucle de chargerListe) tous les messages sans images
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

    fun telechargerImage(id: Long){
        if(batRcv.desactive){
            // Pas de téléchargement d'image pour l'instant
        }else {
            if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) {
                //toast("Permission Internet Ok")
                val connMgr = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                when (connMgr.activeNetworkInfo?.type) {
                    ConnectivityManager.TYPE_WIFI, ConnectivityManager.TYPE_MOBILE ->
                        DownloadTask(id).execute(URL("https://source.unsplash.com/random/200x200")) // 200x200 : petites images carrées
                    null -> {
                        toast("Pas de réseau")
                    }
                }
            } else {
                // On demande la permission
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE), 1);
            }
        }
    }

    inner class DownloadTask(val id: Long) : AsyncTask<URL, Void, String?>() {
        override fun doInBackground(vararg params: URL): String? {

            try {
                val conn = params[0].openConnection() as HttpURLConnection
                conn.connect()
                if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                    // on ne fait rien, on laisse la valeur à null
                    return null
                }else {
                    // J'ai donc un InputStream (conn.inputStream) et je veux l'écrire dans un fichier
                    // Je cherche sur Google "android write inputstream in file"
                    // Le code qui suit provient du premier résultat :
                    // https://stackoverflow.com/questions/10854211/android-store-inputstream-in-file

                    val file = File(activity.filesDir, "$id")
                    // on stocke le fichier dans le stockage internet du téléphone
                    // Nom du fichier = primary key du message (comme ça on est sur que c'est unique)
                    val output = FileOutputStream(file)
                    try {
                        val buffer = ByteArray(4 * 1024) // or other buffer size
                        var read = conn.inputStream.read(buffer)

                        while (read != -1) {
                            output.write(buffer, 0, read)
                            read = conn.inputStream.read(buffer)
                        }

                        output.flush()
                    } finally {
                        output.close()
                    }
                    // Fin du code de stackoverflow pour écrire un InputStream dans un fichier

                    // On a correctement télécharger une nouvelle image
                    return file.path

                    // TODO : on aurait pu avoir d'autres alternatives à la sauvegarde de l'image dans un fichier :
                    // - sauvegarder son URL réelle fixe (après redirection par unsplash.com/random) avec "conn.url" et le re-télécharger à chaque fois
                    // - sauvegarder l'inputstream binaire directement dans une colonne BLOB (données binaires) de la base de données
                    // - créer un Bitmap avec BitmapFactory ici et appeller bitmap.compress plus tard pour l'écrire dans un fichier sans gérer les OutputStream nous même
                }
            } catch(e: FileNotFoundException){
                // on ne fait rien, on laisse la valeur à null
                return null
            } catch(e: UnknownHostException){
                return null
            } catch(e: ConnectException){
                return null
            } catch(e: IOException){
                return null
            }
        }

        override fun onPostExecute(result: String?) {
            // On a une nouvelle image, on met à jour la base de données et on recharge la liste pour l'afficher
            if(result != null) {
                activity.dbLivres.use {
                    update(DBLivres.TABLE_LIVRES, DBLivres.COLUMN_LIVRES_IMAGE to result)
                            .whereArgs("${DBLivres.TABLE_LIVRES_ID} = {id}", "id" to id).exec()
                }

                chargerListe()
            }
        }
    } // Fin de DownloadTask

}
