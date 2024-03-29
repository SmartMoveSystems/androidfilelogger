package com.smartmove.logsenderexample

import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.smartmove.logsenderexample.databinding.ActivityMainBinding
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.fab.setOnClickListener { _ ->
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
