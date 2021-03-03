package com.application.informationsupport

import android.app.DatePickerDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.application.informationsupport.adapters.ObjectListAdapter
import com.application.informationsupport.database.DatabaseConnector
import com.application.informationsupport.models.ModelObjectList
import java.sql.SQLException
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
        val nameET = findViewById<EditText>(R.id.nameET)
        val dateET = findViewById<EditText>(R.id.dateET)
        val innerSearchButton = findViewById<Button>(R.id.innerSearchButton)
        val outSearchButton = findViewById<Button>(R.id.outSearchButton)
        recyclerView = findViewById(R.id.dataRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)
        dateET.setOnClickListener {
            val calendar = Calendar.getInstance()
            val cYear = calendar.get(Calendar.YEAR)
            val cMonth = calendar.get(Calendar.MONTH)
            val cDay = calendar.get(Calendar.DAY_OF_MONTH)
            val datePickerDialog = DatePickerDialog(
                this,
                DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                    val correctMonth = (month + 1)
                    var formatMonth = correctMonth.toString()
                    if (correctMonth < 10) formatMonth = "0$correctMonth"
                    var formatDay = dayOfMonth.toString()
                    if (dayOfMonth < 10) formatDay = "0$dayOfMonth"
                    dateET.text = SpannableStringBuilder("${year}-${formatMonth}-${formatDay}")
                }, cYear, cMonth, cDay
            )
            datePickerDialog.show()
        }
        innerSearchButton.setOnClickListener {
            val dataSet = mutableListOf<ModelObjectList>()
            try {
                val connection = DatabaseConnector().createConnection()
                val currentUserInfoRS = connection.createStatement()
                    .executeQuery("select * from users where login = '${intent.getStringExtra("name")!!}'")
                currentUserInfoRS.next()
                val currentUserService = currentUserInfoRS.getString("service")
                val currentUserDistrict = currentUserInfoRS.getString("district")
                val rs = connection.createStatement()
                    .executeQuery("select * from dataobjects where deleted = '0' and iddataobject in (select dataobject from recordvalues where deleted = '0' and (value = '${nameET.text}' or value = '${dateET.text}')) and branch in (select branch from branches_services where deleted = '0' and service = '$currentUserService') and branch in (select branch from branches_districts where deleted = '0' and district = '$currentUserDistrict')")
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
            } catch (e: SQLException) {
                Log.e("MyApp", e.toString())
                e.printStackTrace()
            }
            recyclerView.adapter =
                ObjectListAdapter(this, dataSet, intent.getStringExtra("name")!!, true)
        }
        outSearchButton.setOnClickListener {

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
