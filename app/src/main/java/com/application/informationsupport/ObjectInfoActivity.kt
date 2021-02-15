package com.application.informationsupport

import android.database.SQLException
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.application.informationsupport.adapters.ServiceItemAdapter
import com.application.informationsupport.database.DatabaseConnector
import com.application.informationsupport.models.ModelServiceInfo

class ObjectInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_object_info)
        title = intent.getStringExtra("name")

        val informationTV = findViewById<TextView>(R.id.informationTV)
        val objectIV = findViewById<ImageView>(R.id.imageView)

        if (intent.getStringExtra("type") == "service") {
            informationTV.text = "Информация о службе:"
            objectIV.visibility = ImageView.INVISIBLE

        }
    }
}
