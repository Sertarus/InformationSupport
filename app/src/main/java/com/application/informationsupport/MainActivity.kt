package com.application.informationsupport

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.application.informationsupport.adapters.ObjectListAdapter
import com.application.informationsupport.models.ModelObjectList

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val testData = mutableListOf(
            ModelObjectList("123", "456", "789", true),
            ModelObjectList("123", "456", "789", true),
            ModelObjectList("123", "456", "789", false)
        )

        val adapter = ObjectListAdapter(this, testData)
        val recyclerView = findViewById<RecyclerView>(R.id.dataRecyclerView)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
}
