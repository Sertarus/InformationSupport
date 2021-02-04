package com.application.informationsupport

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {
    protected lateinit var logButton: Button
    protected lateinit var login: TextInputEditText
    protected lateinit var password: TextInputEditText

    val testUserData = mapOf("Иванов И.И." to "qwerty", "Сергеев И.И." to "12345", "Петров И.И." to "zxc")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        logButton = findViewById(R.id.logButton)
        login = findViewById(R.id.loginET)
        password = findViewById(R.id.passwordET)
        logButton.setOnClickListener {
            if ( testUserData.keys.contains(login.text.toString()) &&
                testUserData[login.text.toString()] == password.text.toString()) {
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra("name", login.text.toString())
                startActivity(intent)
            }
            else {
                Toast.makeText(this, "Неправильное имя пользователя или пароль",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
}
