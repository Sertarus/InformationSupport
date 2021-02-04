package com.application.informationsupport

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.application.informationsupport.adapters.ObjectListAdapter
import com.application.informationsupport.models.ModelObjectList

class MainActivity : AppCompatActivity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        val adapter = ObjectListAdapter(this, testMap)
        val recyclerView = findViewById<RecyclerView>(R.id.dataRecyclerView)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_logout) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        if (id == R.id.action_profile) {
            val newIntent = Intent(this, ProfileActivity::class.java)
            newIntent.putExtra("name", intent.getStringExtra("name"))
            newIntent.putExtra("isOwnProfile", "y")
            startActivity(newIntent)
        }
        return super.onOptionsItemSelected(item)
    }
}
