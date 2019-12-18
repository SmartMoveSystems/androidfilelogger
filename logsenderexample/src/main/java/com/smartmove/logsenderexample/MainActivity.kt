package com.smartmove.logsenderexample

import android.app.AlertDialog
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.smartmove.androidfilelogger.LogSender

import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { _ ->
            AlertDialog.Builder(this)
                .setTitle("Logger test")
                .setMessage("What would you like to test?")
                .setPositiveButton("Crash app") { _, _ ->
                    val uhOh : HashMap<String, String>? = null
                    val thisWillBreak = uhOh!!.containsKey("Nope")
                    Timber.i("Reaching this point is exceedingly unlikely $thisWillBreak")
                }
                .setNegativeButton("Send logs") { _, _ ->
                    LogSenderApp.instance?.sendLogs()
                }
                .create().show()

        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
