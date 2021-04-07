package com.application.informationsupport

import android.app.DatePickerDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.application.informationsupport.adapters.ObjectListAdapter
import com.application.informationsupport.database.DatabaseConnector
import com.application.informationsupport.models.ModelObjectList
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.lang.Exception
import java.sql.SQLException
import java.text.SimpleDateFormat
import java.util.*

class HumanSearchActivity : AppCompatActivity() {

    lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_human_search)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        val configuration = this.resources.configuration
        val locale = Locale("ru", "RU")
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        this.createConfigurationContext(configuration)
        val searchET = findViewById<EditText>(R.id.searchET)
        val innerSearchButton = findViewById<Button>(R.id.innerSearchButton)
        recyclerView = findViewById(R.id.dataRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)
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
        innerSearchButton.setOnClickListener {
            val dataSet = mutableListOf<ModelObjectList>()
            try {
                val connection = DatabaseConnector(url, username, pass).createConnection()
                val currentUserInfoRS = connection.createStatement()
                    .executeQuery("select * from users where login = '${intent.getStringExtra("name")!!}'")
                currentUserInfoRS.next()
                val currentUserService = currentUserInfoRS.getString("service")
                val currentUserDistrict = currentUserInfoRS.getString("district")
                val rs = connection.createStatement()
                    .executeQuery("select * from dataobjects where deleted = '0' and ((iddataobject in (select dataobject from recordvalues where deleted = '0' and lower(value) like '%${searchET.text.toString().toLowerCase()}%')) or lower(name) like '%${searchET.text.toString().toLowerCase()}%') and branch in (select branch from branches_services where deleted = '0' and service = '$currentUserService') and branch in (select branch from branches_districts where deleted = '0' and district = '$currentUserDistrict')")
                while (rs.next()) {
                    val newStmt = connection.createStatement()
                    val nameSet = newStmt.executeQuery(
                        "select login from users where iduser = '" +
                                rs.getString("createdby") + "'"
                    )
                    nameSet.next()
                    val name = nameSet.getString("login")
                    dataSet.add(
                        ModelObjectList(
                            rs.getString("name"),
                            name,
                            rs.getString("creationdate").split(".")[0]
                        )
                    )
                }
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
            recyclerView.adapter =
                ObjectListAdapter(this, dataSet, intent.getStringExtra("name")!!, true, url, username, pass)
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
