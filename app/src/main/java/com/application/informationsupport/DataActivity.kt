package com.application.informationsupport

import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.application.informationsupport.adapters.DataItemAdapter
import com.application.informationsupport.models.ModelDataItem

class DataActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        val name = intent.getStringExtra("name")
        setTitle(name)
        val date = intent.getStringExtra("date")
        val dateTV = findViewById<TextView>(R.id.dateTV)
        dateTV.text = date
        val creator = intent.getStringExtra("creator")
        val creatorTV = findViewById<TextView>(R.id.creatorTV)
        creatorTV.text = creator
        val testData = mutableListOf(
            ModelDataItem("Мероприятие 1", "23.01.2021"),
            ModelDataItem("Мероприятие 2", "30.02.2021"),
            ModelDataItem("Мероприятие 3", "15.03.2021")
        )

        val adapter = DataItemAdapter(this, testData)
        val recyclerView = findViewById<RecyclerView>(R.id.dataItemRecyclerView)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
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
