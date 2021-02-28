package com.application.informationsupport

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.application.informationsupport.adapters.ObjectListAdapter
import com.application.informationsupport.models.ModelObjectList

class MainActivity : AppCompatActivity() {

    lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val adapter = ObjectListAdapter(this, mutableListOf(), intent.getStringExtra("name")!!)
        recyclerView = findViewById<RecyclerView>(R.id.dataRecyclerView)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter.refreshObjectList("")
        recyclerView.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        val item = menu!!.findItem(R.id.action_search)
        val searchView = item.actionView as androidx.appcompat.widget.SearchView
        searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                    if (!TextUtils.isEmpty(query!!.trim())) {
                        (recyclerView.adapter as ObjectListAdapter).refreshObjectList(query)
                    } else {
                        (recyclerView.adapter as ObjectListAdapter).refreshObjectList("")
                    }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                    if (!TextUtils.isEmpty(newText!!.trim())) {
                        (recyclerView.adapter as ObjectListAdapter).refreshObjectList(newText)
                    } else {
                        (recyclerView.adapter as ObjectListAdapter).refreshObjectList("")
                    }
                return false
            }

        })
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val admin = menu!!.findItem(R.id.action_admin)
        if (intent.getStringExtra("role") == "0") admin.isVisible = false
        return super.onPrepareOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_admin) {
            val newIntent = Intent(this, AdminActivity::class.java)
            newIntent.putExtra("name", intent.getStringExtra("name"))
            newIntent.putExtra("role", intent.getStringExtra("role"))
            startActivity(newIntent)
        }
        if (id == R.id.action_logout) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        if (id == R.id.action_program) {
            val newIntent = Intent(this, ProfileActivity::class.java)
            newIntent.putExtra("name", intent.getStringExtra("name"))
            newIntent.putExtra("isOwnProfile", "y")
            startActivity(newIntent)
        }
        return super.onOptionsItemSelected(item)
    }
}
