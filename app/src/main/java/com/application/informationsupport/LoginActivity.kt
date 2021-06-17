package com.application.informationsupport

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.application.informationsupport.database.DatabaseConnector
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*


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
        val fab = findViewById<FloatingActionButton>(R.id.floatingChooseButton)
        fab.setOnClickListener {
            val settingsView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_set_settings, null)
            val ipET = settingsView.findViewById<EditText>(R.id.serverET)
            val portET = settingsView.findViewById<EditText>(R.id.portET)
            val instanceET = settingsView.findViewById<EditText>(R.id.instanceET)
            val userET = settingsView.findViewById<EditText>(R.id.userNameET)
            val passET = settingsView.findViewById<EditText>(R.id.DBPassET)
            val ftpIPET = settingsView.findViewById<EditText>(R.id.ftpServerET)
            val ftpUsernameET = settingsView.findViewById<EditText>(R.id.ftpUserNameET)
            val ftpPassET = settingsView.findViewById<EditText>(R.id.ftpPassET)
            val settingsButton = settingsView.findViewById<Button>(R.id.saveSettingsButton)
            val changeBuilder = AlertDialog.Builder(this)
            changeBuilder.setView(settingsView)
            val ad = changeBuilder.create()
            ad.show()
            settingsButton.setOnClickListener {
                when {
                    ipET.text.toString().isEmpty() -> {
                        Toast.makeText(
                            this,
                            "Не заполнен адрес сервера",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    portET.text.isEmpty() -> {
                        Toast.makeText(
                            this,
                            "Не заполнен порт сервера",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    instanceET.text.isEmpty() -> {
                        Toast.makeText(
                            this,
                            "Не заполнено имя экземпляра БД",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    userET.text.isEmpty() -> {
                        Toast.makeText(
                            this,
                            "Не заполнено имя пользователя БД",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    passET.text.isEmpty() -> {
                        Toast.makeText(
                            this,
                            "Не заполнен пароль пользователя БД",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    ftpIPET.text.isEmpty() -> {
                        Toast.makeText(
                            this,
                            "Не заполнен адрес FTP-сервера",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    ftpUsernameET.text.isEmpty() -> {
                        Toast.makeText(
                            this,
                            "Не заполнено имя пользователя FTP-сервера",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    ftpPassET.text.isEmpty() -> {
                        Toast.makeText(
                            this,
                            "Не заполнен пароль пользователя FTP-сервера",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {
                        val url =
                            ("jdbc:oracle:thin:@" + ipET.text + ":" + portET.text + ":"
                                    + instanceET.text)
                        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                        val sharedPreferences = EncryptedSharedPreferences.create(
                            "informationSupport",
                            masterKeyAlias,
                            this,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                        )

                        val editor = sharedPreferences.edit()
                        editor.putString("URL", url)
                        editor.apply()
                        editor.putString("username", userET.text.toString())
                        editor.apply()
                        editor.putString("pass", passET.text.toString())
                        editor.apply()
                        editor.putString("ftp_IP", ftpIPET.text.toString())
                        editor.apply()
                        editor.putString("ftp_username", ftpUsernameET.text.toString())
                        editor.apply()
                        editor.putString("ftp_pass", ftpPassET.text.toString())
                        editor.apply()
                        ad.dismiss()
                    }
                }
            }
        }
        logButton.setOnClickListener {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPreferences = EncryptedSharedPreferences.create(
                "informationSupport",
                masterKeyAlias,
                this,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            val url = sharedPreferences.getString("URL", "")
            val username = sharedPreferences.getString("username", "")
            val pass = sharedPreferences.getString("pass", "")
            var rightPass = ""
            var blocked = true
            var role = ""
            try {
                val connection = DatabaseConnector(url, username, pass).createConnection()
                val stmt = connection.createStatement()
                val rs =
                    stmt.executeQuery("select * from users where login = '" + login.text.toString() + "'")
                val isNotEmpty = rs.next()
                if (isNotEmpty) {
                    rightPass = rs.getString("password")
                    if (rs.getString("blocked") == "0") blocked = false
                    role = rs.getString("role")
                }

                when {
                    blocked || rightPass == "" || rightPass != password.text.toString() -> {
                        Toast.makeText(
                            this, "Неверное имя пользователя или пароль",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {
                        val rs = connection.createStatement().executeQuery(
                            "select iduser from users where" +
                                    " login = '${login.text.toString()}'"
                        )
                        rs.next()
                        val creatorID = rs.getString("iduser")
                        connection.createStatement().executeQuery("update log_user_info set deleted = 1, changeddate = SYSTIMESTAMP where userid = $creatorID")
                        connection.createStatement().executeQuery("insert into log_user_info (userid, creationdate) values ('$creatorID', SYSTIMESTAMP)")
                        val intent = Intent(this, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.putExtra("name", login.text.toString())
                        intent.putExtra("role", role)
                        startActivity(intent)
                    }
                }
                connection.close()
            } catch (e: Exception) {
                try {
                    val logfile = File(this.filesDir, "log.txt")
                    val timestamp = System.currentTimeMillis()
                    val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm:ss", Locale.ROOT);
                    val localTime = sdf.format(Date(timestamp))
                    val date = sdf.parse(localTime)!!
                    if (logfile.exists()) {
                        val fout = FileOutputStream(logfile, true)
                        val myOutWriter = OutputStreamWriter(fout)
                        myOutWriter.append("\n")
                        myOutWriter.append(date.toString())
                        myOutWriter.append("\n")
                        myOutWriter.append(e.toString())
                        e.stackTrace.forEach {
                            myOutWriter.append("\n")
                            myOutWriter.append(it.toString())
                        }
                        myOutWriter.append("\n")
                        myOutWriter.close()
                        fout.close()
                    }
                    else {
                        val writer = FileWriter(logfile)
                        writer.append(date.toString())
                        writer.append("\n")
                        writer.append(e.toString())
                        e.stackTrace.forEach {
                            writer.append("\n")
                            writer.append(it.toString())
                        }
                        writer.append("\n")
                        writer.flush()
                        writer.close()
                    }
                }
                catch (e: Exception) {

                }
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
