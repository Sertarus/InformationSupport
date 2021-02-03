package com.application.informationsupport

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.application.informationsupport.adapters.ObjectListAdapter
import com.application.informationsupport.models.ModelObjectList

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val testMap = mapOf(
            ModelObjectList("Ориентировки", "Иванов И.И.", "03.12.2020", true) to mapOf(
                ModelObjectList("Люди", "Иванов И.И.", "03.12.2020", true) to null,
                ModelObjectList("Машины", "Сергеев И.И.", "01.01.2021", true) to null
            ),
            ModelObjectList("Преступления", "Сергеев И.И.", "01.01.2021", true) to mapOf(
                ModelObjectList("Угон машины", "Иванов И.И.", "03.12.2020", false) to null,
                ModelObjectList("Ограбление магазина", "Сергеев И.И.", "01.01.2021", false) to null
            ),
            ModelObjectList("Мероприятия", "Иванов И.И.", "05.10.2020", false) to null
        )

        val adapter = ObjectListAdapter(this, testMap)
        val recyclerView = findViewById<RecyclerView>(R.id.dataRecyclerView)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }


}
