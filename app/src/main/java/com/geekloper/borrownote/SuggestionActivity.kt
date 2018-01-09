package com.geekloper.borrownote

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_suggestion.*
import java.net.URLEncoder

class SuggestionActivity : AppCompatActivity() {

    companion object {

        const val EXTRA_QUERY = "SuggestionActivity.EXTRA_QUERY"

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_suggestion)

        val query = intent.getStringExtra(EXTRA_QUERY)

        webview.settings.setJavaScriptEnabled(true)
        webview.loadUrl("http://mansour-ismail.com/android?q=" + URLEncoder.encode(query, "UTF-8"))
    }
}
