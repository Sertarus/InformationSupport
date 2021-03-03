package com.application.informationsupport

import android.content.Intent
import android.database.SQLException
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.application.informationsupport.adapters.ObjectListAdapter
import com.application.informationsupport.database.DatabaseConnector
import java.util.*
import kotlin.concurrent.schedule


class MainActivity : AppCompatActivity() {

    lateinit var recyclerView: RecyclerView
    var hot_number = 0
    var ui_hot: TextView? = null

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
        val menu_hotlist = menu.findItem(R.id.menu_hotlist).actionView
        ui_hot = menu_hotlist.findViewById(R.id.hotlist_hot)
        try {
            val connection = DatabaseConnector().createConnection()
            val serviceIDRS = connection.createStatement().executeQuery("select service from users where login = '${intent.getStringExtra("name")!!}' and deleted = '0'")
            serviceIDRS.next()
            val serviceID = serviceIDRS.getString("service")
            val districtIDRS = connection.createStatement().executeQuery("select district from users where login = '${intent.getStringExtra("name")!!}' and deleted = '0'")
            districtIDRS.next()
            val districtID = districtIDRS.getString("district")
            val eventNumber = connection.createStatement().executeQuery("select count(*) as total from events where CAST(systimestamp AS TIMESTAMP) between timestart and timeend and idevent in (select event from events_services where service = '$serviceID' and deleted = '0') and  idevent in (select event from events_districts where district = '$districtID' and deleted = '0') and deleted = '0'")
            eventNumber.next()
            updateHotCount(eventNumber.getInt("total"))
            connection.close()
        }
        catch (e: SQLException) {
            Log.e("MyApp", e.toString())
            e.printStackTrace()
        }
        Timer("UpdateNotification", false).schedule(10000) {
            try {
                val connection = DatabaseConnector().createConnection()
                val serviceIDRS = connection.createStatement().executeQuery("select service from users where login = '${intent.getStringExtra("name")!!}' and deleted = '0'")
                serviceIDRS.next()
                val serviceID = serviceIDRS.getString("service")
                val districtIDRS = connection.createStatement().executeQuery("select district from users where login = '${intent.getStringExtra("name")!!}' and deleted = '0'")
                districtIDRS.next()
                val districtID = districtIDRS.getString("district")
                val eventNumber = connection.createStatement().executeQuery("select count(*) as total from events where CAST(systimestamp AS TIMESTAMP) between timestart and timeend and idevent in (select event from events_services where service = '$serviceID' and deleted = '0') and  idevent in (select event from events_districts where district = '$districtID' and deleted = '0') and deleted = '0'")
                eventNumber.next()
                updateHotCount(eventNumber.getInt("total"))
                connection.close()
                invalidateOptionsMenu()
            }
            catch (e: SQLException) {
                Log.e("MyApp", e.toString())
                e.printStackTrace()
            }
        }
        menu_hotlist.setOnClickListener {
            val newIntent = Intent(this, EventActivity::class.java)
            newIntent.putExtra("name", intent.getStringExtra("name")!!)
            this.startActivity(newIntent)
        }
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

    fun updateHotCount(new_hot_number: Int) {
        hot_number = new_hot_number
        if (ui_hot == null) return
        runOnUiThread {
            if (new_hot_number == 0) ui_hot!!.visibility = View.INVISIBLE else {
                ui_hot!!.visibility = View.VISIBLE
                ui_hot!!.text = hot_number.toString()
            }
        }
    }

}
