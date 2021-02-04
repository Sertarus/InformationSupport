package com.application.informationsupport

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.application.informationsupport.models.ModelUserInfo

class ProfileActivity : AppCompatActivity() {

    val testUserData = mapOf("Иванов И.И." to ModelUserInfo("1", "89044467864",
        "test1@mail.ru"), "Сергеев И.И." to ModelUserInfo("2",
        "89041237890", "test2@mail.ru"), "Петров И.И." to ModelUserInfo("3",
        "89045678764", "test3@mail.ru"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        val serviceTV = findViewById<TextView>(R.id.serviceTV)
        val phoneNumberTV = findViewById<TextView>(R.id.phoneNumberTV)
        val emailTV = findViewById<TextView>(R.id.emailTV)
        val changeInfoTV = findViewById<TextView>(R.id.changeInfoTV)
        val newPhoneNumberTV = findViewById<TextView>(R.id.newPhoneNumberTV)
        val newPhoneNumberET = findViewById<EditText>(R.id.phoneNumberET)
        val newPhoneNumberButton = findViewById<Button>(R.id.phoneNumberSaveButton)
        val newEmailTV = findViewById<TextView>(R.id.newEmailTV)
        val newEmailET = findViewById<EditText>(R.id.emailET)
        val newEmailButton = findViewById<Button>(R.id.emailSaveButton)

        if (intent.getStringExtra("isOwnProfile") == "n") {
            changeInfoTV.visibility = View.INVISIBLE
            newPhoneNumberTV.visibility = View.INVISIBLE
            newPhoneNumberET.visibility = View.INVISIBLE
            newPhoneNumberButton.visibility = View.INVISIBLE
            newEmailTV.visibility = View.INVISIBLE
            newEmailET.visibility = View.INVISIBLE
            newEmailButton.visibility = View.INVISIBLE
        }
        val userName = intent.getStringExtra("name")!!
        title = userName
        val userData = testUserData[userName]!!
        serviceTV.text = userData.service
        phoneNumberTV.text = userData.phoneNumber
        emailTV.text = userData.email
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
