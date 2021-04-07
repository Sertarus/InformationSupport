package com.application.informationsupport

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        val loginTV = findViewById<TextView>(R.id.loginTV)
        val deviceIDTV = findViewById<TextView>(R.id.deviceIDTV)
        val appVersionTV = findViewById<TextView>(R.id.versionTV)
        val sendErrorInfoButton = findViewById<Button>(R.id.errorInfoButton)

        val userName = intent.getStringExtra("name")!!
        title = "О программе"
        loginTV.text = userName
        deviceIDTV.text = android.os.Build.SERIAL
        appVersionTV.text = BuildConfig.VERSION_NAME
        sendErrorInfoButton.setOnClickListener {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPreferences = EncryptedSharedPreferences.create(
                "informationSupport",
                masterKeyAlias,
                this,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            var con: FTPClient? = null
            try {
                con = FTPClient()
                con.connect(sharedPreferences.getString("ftp_IP", ""))
                if (con.login(sharedPreferences.getString("ftp_username", ""), sharedPreferences.getString("ftp_pass", ""))) {
                    con.enterLocalPassiveMode()
                    con.setFileType(FTP.BINARY_FILE_TYPE)
                    val fileDir = File(this.filesDir, "log_error")
                    if (fileDir.exists()) {
                        val logFile = File(fileDir, "log")
                        if (logFile.exists()) {
                            val input = FileInputStream(logFile)
                            val timestamp = System.currentTimeMillis()
                            val sdf = SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.ROOT);
                            val localTime = sdf.format(Date(timestamp))
                            con.storeFile("/log_" + userName + "_" + localTime, input)
                            input.close()
                        }
                    }
                    con.logout()
                    con.disconnect()
                }
            }
            catch (e: Exception) {
                Toast.makeText(this, "Произошла ошибка", Toast.LENGTH_SHORT).show()
            }
        }
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
