package com.geekloper.borrownote

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.confirmation_activity.*


class ConfirmationActivity  : AppCompatActivity() {
    companion object {
        const val EXTRA_MESSAGE = "ConfirmationActivity.MESSAGE"
        const val EXTRA_ISCONFIRMED = "ConfirmationActivity.ISCONFIRMED"

        const val VAL_CANCEL = 0
        const val VAL_CONFIRMED = 1
        const val VAL_EDIT = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.confirmation_activity)

        txt_note.text = intent.getStringExtra(EXTRA_MESSAGE)
        btn_confirm.setOnClickListener {
            returnResult(VAL_CONFIRMED)
        }
        btn_edit.setOnClickListener {
            returnResult(VAL_EDIT)
        }
        btn_cancel.setOnClickListener {
            returnResult(VAL_CANCEL)
        }
    }

    private fun returnResult(res: Int) {
        val retIntent = Intent()
        retIntent.putExtra(EXTRA_ISCONFIRMED, res)
        setResult(Activity.RESULT_OK, retIntent)
        finish()
    }
}