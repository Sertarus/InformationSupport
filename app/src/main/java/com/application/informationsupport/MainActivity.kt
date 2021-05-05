package com.application.informationsupport

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.application.informationsupport.adapters.ObjectListAdapter
import com.application.informationsupport.database.DatabaseConnector
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule


class MainActivity : AppCompatActivity() {

    lateinit var recyclerView: RecyclerView
    private var hot_number = 0
    private var ui_hot: TextView? = null
    private var update_number = 0
    private var ui_update: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
        val adapter =
            ObjectListAdapter(this, mutableListOf(), intent.getStringExtra("name")!!, false, url, username, pass)
        recyclerView = findViewById(R.id.dataRecyclerView)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter.refreshObjectList("")
        recyclerView.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        val menu_hotlist = menu!!.findItem(R.id.menu_hotlist).actionView
        val menu_update = menu.findItem(R.id.menu_notifications).actionView
        ui_hot = menu_hotlist.findViewById(R.id.hotlist_hot)
        ui_update = menu_update.findViewById(R.id.hotlist_hot)
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
        try {
            val connection = DatabaseConnector(url, username, pass).createConnection()
            val serviceIDRS = connection.createStatement()
                .executeQuery("select service from users where login = '${intent.getStringExtra("name")!!}' and deleted = '0'")
            serviceIDRS.next()
            val serviceID = serviceIDRS.getString("service")
            val districtIDRS = connection.createStatement()
                .executeQuery("select district from users where login = '${intent.getStringExtra("name")!!}' and deleted = '0'")
            districtIDRS.next()
            val districtID = districtIDRS.getString("district")
            val eventNumber = connection.createStatement()
                .executeQuery("select count(*) as total from events where CAST(systimestamp AS TIMESTAMP) between timestart and timeend and idevent in (select event from events_services where service = '$serviceID' and deleted = '0') and  idevent in (select event from events_districts where district = '$districtID' and deleted = '0') and deleted = '0'")
            eventNumber.next()
            updateHotCount(eventNumber.getInt("total"))
            val updateNumber = connection.createStatement().executeQuery("select count(*) as total from dataobjects where (extract (day from (SYSTIMESTAMP - creationdate)) < 1 or extract (day from (SYSTIMESTAMP - changeddate)) < 1) and deleted = 0 and branch in (select branch from branches_districts where district = '$districtID' and deleted = 0) and branch in (select branch from branches_services where service = '$serviceID' and deleted = 0)")
            updateNumber.next()
            updateNewCount(updateNumber.getInt("total"))
            connection.close()
        } catch (e: Exception) {
            try {
                val logfile = File(Environment.getExternalStorageDirectory().absolutePath, "log.txt")
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
                    writer.flush()
                    writer.close()
                }
            }
            catch (e: Exception) {

            }
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
        Timer("UpdateNotification", false).schedule(20000, 20000) {
            try {
                val connection = DatabaseConnector(url, username, pass).createConnection()
                val serviceIDRS = connection.createStatement().executeQuery(
                    "select service from users where login = '${intent.getStringExtra("name")!!}' and deleted = '0'"
                )
                serviceIDRS.next()
                val serviceID = serviceIDRS.getString("service")
                val districtIDRS = connection.createStatement().executeQuery(
                    "select district from users where login = '${intent.getStringExtra("name")!!}' and deleted = '0'"
                )
                districtIDRS.next()
                val districtID = districtIDRS.getString("district")
                val eventNumber = connection.createStatement()
                    .executeQuery("select count(*) as total from events where CAST(systimestamp AS TIMESTAMP) between timestart and timeend and idevent in (select event from events_services where service = '$serviceID' and deleted = '0') and  idevent in (select event from events_districts where district = '$districtID' and deleted = '0') and deleted = '0'")
                eventNumber.next()
                updateHotCount(eventNumber.getInt("total"))
                val updateNumber = connection.createStatement().executeQuery("select count(*) as total from dataobjects where (extract (day from (SYSTIMESTAMP - creationdate)) < 1 or extract (day from (SYSTIMESTAMP - changeddate)) < 1) and deleted = 0 and branch in (select branch from branches_districts where district = $districtID) and branch in (select branch from branches_services where service = $serviceID)")
                updateNumber.next()
                updateNewCount(updateNumber.getInt("total"))
                connection.close()
                invalidateOptionsMenu()
            } catch (e: Exception) {
                try {
                    val logfile = File(Environment.getExternalStorageDirectory().absolutePath, "log.txt")
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
                        writer.flush()
                        writer.close()
                    }
                }
                catch (e: Exception) {

                }
                runOnUiThread {
                    Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
        menu_hotlist.setOnClickListener {
            val newIntent = Intent(this, EventActivity::class.java)
            newIntent.putExtra("name", intent.getStringExtra("name")!!)
            this.startActivity(newIntent)
        }
        menu_update.setOnClickListener {
            val newIntent = Intent(this, NewObjectActivity::class.java)
            newIntent.putExtra("name", intent.getStringExtra("name")!!)
            this.startActivity(newIntent)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
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
        if (id == R.id.action_logout) {
            try {
                val connection = DatabaseConnector(url, username, pass).createConnection()
                val rs = connection.createStatement().executeQuery(
                    "select iduser from users where" +
                            " login = '${intent.getStringExtra("name")}'"
                )
                rs.next()
                val creatorID = rs.getString("iduser")
                connection.createStatement().executeQuery("update log_user_info set changeddate = SYSTIMESTAMP, deleted = '1' where userid = '$creatorID' and deleted = '0'")
                connection.close()
            }
            catch (e: Exception) {
                try {
                    val logfile = File(Environment.getExternalStorageDirectory().absolutePath, "log.txt")
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
                        writer.flush()
                        writer.close()
                    }
                }
                catch (e: Exception) {

                }
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        if (id == R.id.action_program) {
            val newIntent = Intent(this, ProfileActivity::class.java)
            newIntent.putExtra("name", intent.getStringExtra("name"))
            startActivity(newIntent)
        }
        if (id == R.id.action_full_search) {
            val newIntent = Intent(this, HumanSearchActivity::class.java)
            newIntent.putExtra("name", intent.getStringExtra("name"))
            startActivity(newIntent)
        }
        if (id == R.id.action_chat) {
            val newIntent = Intent(this, ChatActivity::class.java)
            newIntent.putExtra("name", intent.getStringExtra("name"))
            startActivity(newIntent)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateHotCount(new_hot_number: Int) {
        hot_number = new_hot_number
        if (ui_hot == null) return
        runOnUiThread {
            if (new_hot_number == 0) ui_hot!!.visibility = View.INVISIBLE else {
                ui_hot!!.visibility = View.VISIBLE
                ui_hot!!.text = hot_number.toString()
            }
        }
    }

    private fun updateNewCount(new_update_number: Int) {
        update_number = new_update_number
        if (ui_update == null) return
        runOnUiThread {
            if (new_update_number == 0) ui_update!!.visibility = View.INVISIBLE else {
                ui_update!!.visibility = View.VISIBLE
                ui_update!!.text = update_number.toString()
            }
        }
    }

}
