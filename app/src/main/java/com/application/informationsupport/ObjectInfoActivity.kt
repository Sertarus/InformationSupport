package com.application.informationsupport

import android.database.SQLException
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.application.informationsupport.adapters.DataItemAdapter
import com.application.informationsupport.database.DatabaseConnector
import com.application.informationsupport.models.ModelDataItem

class ObjectInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_object_info)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        title = intent.getStringExtra("name")

        val informationTV = findViewById<TextView>(R.id.informationTV)
        val objectIV = findViewById<ImageView>(R.id.imageView)
        val recyclerView = findViewById<RecyclerView>(R.id.dataItemRecyclerView)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val dataSet = mutableListOf<ModelDataItem>()
        val type = intent.getStringExtra("type")
        if (type == "service" || type == "district" || type == "device") {
            var typeName = ""
            when (type) {
                "service" -> {
                    informationTV.text = "Информация о службе:"
                    typeName = "службы"
                }

                "district" -> {
                    informationTV.text = "Информация о районе:"
                    typeName = "района"
                }

                "device" -> {
                    informationTV.text = "Информация об устройстве:"
                    typeName = "устройства"
                }
            }
            objectIV.visibility = View.GONE
            try {
                val connection = DatabaseConnector().createConnection()
                val stmt = connection.createStatement()
                val rs=stmt.executeQuery("select * from ${type}s where name = '${intent.getStringExtra("name")}'");
                rs.next()
                dataSet.add(ModelDataItem("Название $typeName", rs.getString("name")))
                val userNameStmt = connection.createStatement()
                val userNameRS = userNameStmt.executeQuery("select login from users where iduser =" +
                        " '${rs.getString("createdBy")}'")
                userNameRS.next()
                val createdUserName = userNameRS.getString("login")
                dataSet.add(ModelDataItem("Создавший пользователь", createdUserName))
                dataSet.add(ModelDataItem("Дата создания", rs.getString("creationdate").split(".")[0]))
                if (rs.getString("changedby") != null && rs.getString("changeddate") != null) {
                    val changedUserNameStmt = connection.createStatement()
                    val changedUserNameRS = changedUserNameStmt.executeQuery("select login from users where iduser =" +
                            " '${rs.getString("changedby")}'")
                    changedUserNameRS.next()
                    val changedUserName = changedUserNameRS.getString("login")
                    dataSet.add(ModelDataItem("Последний изменивший пользователь", changedUserName))
                    dataSet.add(ModelDataItem("Дата последнего изменения", rs.getString("changeddate").split(".")[0]))
                }
                connection.close();
            }
            catch (e: SQLException) {
                Log.e("MyApp", e.toString())
                e.printStackTrace()
            }
            val adapter = DataItemAdapter(this, dataSet)
            recyclerView.adapter = adapter
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
