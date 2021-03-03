package com.application.informationsupport

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.application.informationsupport.models.ModelUserInfo
import kotlin.math.log

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        val loginTV = findViewById<TextView>(R.id.loginTV)
        val deviceIDTV = findViewById<TextView>(R.id.deviceIDTV)
        val sendErrorInfoButton = findViewById<Button>(R.id.errorInfoButton)

        val userName = intent.getStringExtra("name")!!
        title = "О программе"
        loginTV.text = userName
        deviceIDTV.text = android.os.Build.SERIAL
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
