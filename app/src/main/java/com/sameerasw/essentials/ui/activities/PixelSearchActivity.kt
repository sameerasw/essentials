/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Application Activities
 * File: PixelSearchActivity.kt
 * Description: Activity component for PixelSearchActivity.kt.
 */

package com.sameerasw.essentials
 
import android.app.Activity
import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import com.sameerasw.essentials.ui.activities.PixelSearchResultsActivity

class PixelSearchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val query = intent.getStringExtra(SearchManager.QUERY)
            ?: intent.getStringExtra("query")
            ?: ""

        val resultsIntent = Intent(this, PixelSearchResultsActivity::class.java).apply {
            putExtra(SearchManager.QUERY, query)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(resultsIntent)
        finish()
    }
}
