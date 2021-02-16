package com.application.informationsupport

import android.database.SQLException
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.application.informationsupport.adapters.ServiceItemAdapter
import com.application.informationsupport.database.DatabaseConnector
import com.application.informationsupport.models.ModelServiceInfo
import com.google.android.material.floatingactionbutton.FloatingActionButton

class AdminActivity : AppCompatActivity() {

    private var currentData = ""
    var serviceAdapter = ServiceItemAdapter(this, mutableListOf(),
        "")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        title = "Панель администратора"
        val chooseDatabaseTypeObjectButton =
            findViewById<FloatingActionButton>(R.id.floatingChooseButton)
        val addObjectButton = findViewById<FloatingActionButton>(R.id.floatingAddButton)
        val recyclerView = findViewById<RecyclerView>(R.id.dataRecyclerView)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        serviceAdapter = ServiceItemAdapter(this, mutableListOf(),
            intent.getStringExtra("name")!!)
        chooseDatabaseTypeObjectButton.setOnClickListener {
            val options = arrayOf(
                "Пользователи",
                "Службы",
                "Районы",
                "Устройства",
                "Ветки",
                "Формы заполнения",
                "Объекты данных"
            )
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Выберите необходимый тип данных")
            builder.setItems(options) { _, which ->
                when (which) {
                    1 -> {
                        title = "Службы"
                        currentData = "services"
                        serviceAdapter.refreshServices()
                        recyclerView.adapter = serviceAdapter
                    }
                }
            }
            builder.create().show()
        }

        addObjectButton.setOnClickListener {
            if (currentData == "services") {
                createServiceDialog(recyclerView)
            }
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

    private fun createServiceDialog(recyclerView: RecyclerView) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_service, null)
        val nameET = view.findViewById<EditText>(R.id.nameET)
        val button = view.findViewById<Button>(R.id.createServiceButton)
        val builder = AlertDialog.Builder(this)
        builder.setView(view)
        val ad = builder.create()
        ad.show()
        button.setOnClickListener {
            if (currentData != "") {
                val name = nameET.text.toString()
                if (name.length in 1..40) {
                    try {
                        val connection = DatabaseConnector().createConnection()
                        val timeStmt = connection.createStatement()
                        val timeSet = timeStmt.executeQuery(
                            "select TO_CHAR(SYSTIMESTAMP," +
                                    " 'YYYY-MM-DD HH24:MI:SS.FF') as mydate from dual"
                        )
                        timeSet.next()
                        val timestamp = timeSet.getString("mydate")
                        val creatorIDStmt = connection.createStatement()
                        val creatorSet = creatorIDStmt.executeQuery(
                            "select iduser from users where" +
                                    " login = '" + intent.getStringExtra("name") + "'"
                        )
                        creatorSet.next()
                        val creatorID = creatorSet.getString("iduser")
                        val insertStmt = connection.createStatement()
                        insertStmt.executeQuery(
                            "insert into services (name, createdby," +
                                    " creationdate) values ('$name', '$creatorID', TO_TIMESTAMP('$timestamp'," +
                                    " 'YYYY-MM-DD HH24:MI:SS.FF'))"
                        )
                        Toast.makeText(this, "Служба добавлена", Toast.LENGTH_SHORT).show()
                        serviceAdapter.refreshServices()
                        ad.dismiss()
                    } catch (e: SQLException) {
                        Log.e("MyApp", e.toString())
                        e.printStackTrace()
                    }
                } else {
                    Toast.makeText(
                        this, "Недопустимое имя службы",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}