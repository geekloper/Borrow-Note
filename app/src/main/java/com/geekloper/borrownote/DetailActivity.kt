package com.geekloper.borrownote

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle



// DetailActivity contient juste un fragment DetailFragment en plein écran
// La classe DetailActivity se contente de passer des messages entre MainActivity et le fragment, elle ne fait rien d'autre
class DetailActivity : AppCompatActivity(), DetailFragment.Listener {
    companion object {
        const val EXTRA_MESSAGE_ID = "DetailActivity.EXTRA_MESSAGE_ID"
        const val EXTRA_LIST_CHANGED = "DetailActivity.EXTRA_LIST_CHANGED"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // On passe au fragment l'id qu'on a reçu dans notre intent (de la part de MainActivity)
        val fragDet = fragmentManager.findFragmentById(R.id.frag_detail) as DetailFragment
        fragDet.afficherDetail(intent.getLongExtra(EXTRA_MESSAGE_ID, 0))
    }

    override fun onMessageDelete() {
        // On prévient MainActivity que la fragment nous a dit que la message a été supprimé
        // Et on retourne à MainActivity automatiquement avec finish(), puisque le message a été supprimé il n'y a plus rien à afficher
        val ret = Intent()
        ret.putExtra(EXTRA_LIST_CHANGED, true)
        setResult(Activity.RESULT_OK, ret)
        finish()
    }
}
