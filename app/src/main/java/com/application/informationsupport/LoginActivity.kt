package com.application.informationsupport

import android.content.Intent
import android.database.SQLException
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.application.informationsupport.database.DatabaseConnector
import com.google.android.material.textfield.TextInputEditText


class LoginActivity : AppCompatActivity() {
    private lateinit var logButton: Button
    private lateinit var login: TextInputEditText
    private lateinit var password: TextInputEditText
    private lateinit var loginLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        logButton = findViewById(R.id.logButton)
        login = findViewById(R.id.loginET)
        password = findViewById(R.id.passwordET)
        loginLabel = findViewById(R.id.LoginLabel)
        logButton.setOnClickListener {
            var rightPass = ""
            var blocked = true
            var role = ""
            try {
                val connection = DatabaseConnector().createConnection()
                val stmt = connection.createStatement()
                val rs =
                    stmt.executeQuery("select * from users where login = '" + login.text.toString() + "'")
                val isNotEmpty = rs.next()
                if (isNotEmpty) {
                    rightPass = rs.getString("password")
                    if (rs.getString("blocked") == "0") blocked = false
                    role = rs.getString("role")
                }
                connection.close()
            } catch (e: SQLException) {
                Log.e("MyApp", e.toString())
                e.printStackTrace()
            }
            when {
                blocked || rightPass == "" || rightPass != password.text.toString() -> {
                    Toast.makeText(
                        this, "Неверное имя пользователя или пароль",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.putExtra("name", login.text.toString())
                    intent.putExtra("role", role)
                    startActivity(intent)
                }
            }
        }
    }
}
