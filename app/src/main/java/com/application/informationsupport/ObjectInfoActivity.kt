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
                val rs=stmt.executeQuery("select * from ${type}s where name = '${intent.getStringExtra("name")}'")
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
                connection.close()
            }
            catch (e: SQLException) {
                Log.e("MyApp", e.toString())
                e.printStackTrace()
            }
            recyclerView.adapter = DataItemAdapter(this, dataSet)
        }
        if (type == "user") {
            informationTV.text = "Информация о пользователе:"
            objectIV.visibility = View.GONE
            try {
                val connection = DatabaseConnector().createConnection()
                val rs=connection.createStatement().executeQuery("select login, password," +
                        " fullname, role, email, phonenumber, s.name as service," +
                        " dis.name as district, dev.name as device, u.createdby, u.changedby," +
                        " u.creationdate, u.changeddate, blocked from users u " +
                        "join services s on s.idservice = u.service " +
                        "join districts dis on dis.iddistrict = u.district " +
                        "join devices dev on dev.iddevice = u.device " +
                        "where login = '${intent.getStringExtra("name")}'")
                rs.next()
                dataSet.add(ModelDataItem("Логин", rs.getString("login")))
                dataSet.add(ModelDataItem("Пароль", rs.getString("password")))
                dataSet.add(ModelDataItem("ФИО", rs.getString("fullname")))
                var roleName = ""
                when (rs.getString("role")) {
                    "0" -> {
                        roleName = "Пользователь"
                    }
                    "1" -> {
                        roleName = "Локальный администратор"
                    }
                    "2" -> {
                        roleName = "Администратор"
                    }
                }
                dataSet.add(ModelDataItem("Роль", roleName))
                dataSet.add(ModelDataItem("Электронная почта", rs.getString("email")))
                dataSet.add(ModelDataItem("Номер телефона", rs.getString("phonenumber")))
                dataSet.add(ModelDataItem("Служба", rs.getString("service")))
                dataSet.add(ModelDataItem("Район", rs.getString("district")))
                dataSet.add(ModelDataItem("Привязанное устройство", rs.getString("device")))
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
                var blockedName = ""
                when (rs.getString("blocked")) {
                    "0" -> {
                        blockedName = "Не заблокирован"
                    }
                    "1" -> {
                        blockedName = "Заблокирован"
                    }
                }
                dataSet.add(ModelDataItem("Статус блокировки", blockedName))
                connection.close()
            }
            catch (e: SQLException) {
                Log.e("MyApp", e.toString())
                e.printStackTrace()
            }
            recyclerView.adapter = DataItemAdapter(this, dataSet)
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
