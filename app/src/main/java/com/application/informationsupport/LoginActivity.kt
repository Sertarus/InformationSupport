package com.application.informationsupport

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class LoginActivity : AppCompatActivity() {
    protected lateinit var logButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        logButton = findViewById(R.id.logButton)
        logButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
