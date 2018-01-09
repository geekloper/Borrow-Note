package com.geekloper.borrownote

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class DetailActivity : AppCompatActivity(), DetailFragment.Listener {
    companion object {
        const val EXTRA_MESSAGE_ID = "DetailActivity.EXTRA_MESSAGE_ID"
        const val EXTRA_LIST_CHANGED = "DetailActivity.EXTRA_LIST_CHANGED"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)


        val fragDet = fragmentManager.findFragmentById(R.id.frag_detail) as DetailFragment
        fragDet.afficherDetail(intent.getLongExtra(EXTRA_MESSAGE_ID, 0))
    }

    override fun onMessageDelete() {
        val ret = Intent()
        ret.putExtra(EXTRA_LIST_CHANGED, true)
        setResult(Activity.RESULT_OK, ret)
        finish()
    }
}
