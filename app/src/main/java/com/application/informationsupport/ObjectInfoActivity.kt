package com.application.informationsupport

import android.database.SQLException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
        objectIV.visibility = View.GONE
        val recyclerView = findViewById<RecyclerView>(R.id.dataItemRecyclerView)
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
            try {
                val connection = DatabaseConnector().createConnection()
                val stmt = connection.createStatement()
                val rs = stmt.executeQuery(
                    "select * from ${type}s where name = '${intent.getStringExtra("name")}' and deleted = '0'"
                )
                rs.next()
                dataSet.add(ModelDataItem("Название $typeName", rs.getString("name")))
                val userNameStmt = connection.createStatement()
                val userNameRS = userNameStmt.executeQuery(
                    "select login from users where iduser =" +
                            " '${rs.getString("createdBy")}'"
                )
                userNameRS.next()
                val createdUserName = userNameRS.getString("login")
                dataSet.add(ModelDataItem("Создавший пользователь", createdUserName))
                dataSet.add(
                    ModelDataItem(
                        "Дата создания",
                        rs.getString("creationdate").split(".")[0]
                    )
                )
                if (rs.getString("changedby") != null && rs.getString("changeddate") != null) {
                    val changedUserNameStmt = connection.createStatement()
                    val changedUserNameRS = changedUserNameStmt.executeQuery(
                        "select login from users where iduser =" +
                                " '${rs.getString("changedby")}'"
                    )
                    changedUserNameRS.next()
                    val changedUserName = changedUserNameRS.getString("login")
                    dataSet.add(ModelDataItem("Последний изменивший пользователь", changedUserName))
                    dataSet.add(
                        ModelDataItem(
                            "Дата последнего изменения",
                            rs.getString("changeddate").split(".")[0]
                        )
                    )
                }
                connection.close()
            } catch (e: SQLException) {
                Log.e("MyApp", e.toString())
                e.printStackTrace()
            }
            recyclerView.adapter = DataItemAdapter(this, dataSet)
        }
        if (type == "user") {
            informationTV.text = "Информация о пользователе:"
            try {
                val connection = DatabaseConnector().createConnection()
                val rs = connection.createStatement().executeQuery(
                    "select login, password," +
                            " fullname, role, email, phonenumber, s.name as service," +
                            " dis.name as district, dev.name as device, u.createdby, u.changedby," +
                            " u.creationdate, u.changeddate, blocked from users u " +
                            "join services s on s.idservice = u.service " +
                            "join districts dis on dis.iddistrict = u.district " +
                            "join devices dev on dev.iddevice = u.device " +
                            "where login = '${intent.getStringExtra("name")}' and deleted = '0'"
                )
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
                val userNameRS = userNameStmt.executeQuery(
                    "select login from users where iduser =" +
                            " '${rs.getString("createdBy")}'"
                )
                userNameRS.next()
                val createdUserName = userNameRS.getString("login")
                dataSet.add(ModelDataItem("Создавший пользователь", createdUserName))
                dataSet.add(
                    ModelDataItem(
                        "Дата создания",
                        rs.getString("creationdate").split(".")[0]
                    )
                )
                if (rs.getString("changedby") != null && rs.getString("changeddate") != null) {
                    val changedUserNameStmt = connection.createStatement()
                    val changedUserNameRS = changedUserNameStmt.executeQuery(
                        "select login from users where iduser =" +
                                " '${rs.getString("changedby")}'"
                    )
                    changedUserNameRS.next()
                    val changedUserName = changedUserNameRS.getString("login")
                    dataSet.add(ModelDataItem("Последний изменивший пользователь", changedUserName))
                    dataSet.add(
                        ModelDataItem(
                            "Дата последнего изменения",
                            rs.getString("changeddate").split(".")[0]
                        )
                    )
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
            } catch (e: SQLException) {
                Log.e("MyApp", e.toString())
                e.printStackTrace()
            }
            recyclerView.adapter = DataItemAdapter(this, dataSet)
        }
        if (type == "datatype") {
            informationTV.text = "Информация о форме:"
            try {
                val connection = DatabaseConnector().createConnection()
                val datatypeRS = connection.createStatement().executeQuery(
                    "select * from" +
                            " datatypes where name = '${intent.getStringExtra("name")}' and deleted = '0'"
                )
                datatypeRS.next()
                dataSet.add(ModelDataItem("Название", datatypeRS.getString("name")))
                val ishuman: String = if (datatypeRS.getString("isHuman") == "0") "Нет" else "Да"
                dataSet.add(ModelDataItem("Информация о человеке", ishuman))
                val userNameStmt = connection.createStatement()
                val userNameRS = userNameStmt.executeQuery(
                    "select login from users where iduser =" +
                            " '${datatypeRS.getString("createdBy")}'"
                )
                userNameRS.next()
                val createdUserName = userNameRS.getString("login")
                dataSet.add(ModelDataItem("Создавший пользователь", createdUserName))
                dataSet.add(
                    ModelDataItem(
                        "Дата создания",
                        datatypeRS.getString("creationdate").split(".")[0]
                    )
                )
                if (datatypeRS.getString("changedby") != null && datatypeRS.getString("changeddate") != null) {
                    val changedUserNameStmt = connection.createStatement()
                    val changedUserNameRS = changedUserNameStmt.executeQuery(
                        "select login from users where iduser =" +
                                " '${datatypeRS.getString("changedby")}'"
                    )
                    changedUserNameRS.next()
                    val changedUserName = changedUserNameRS.getString("login")
                    dataSet.add(ModelDataItem("Последний изменивший пользователь", changedUserName))
                    dataSet.add(
                        ModelDataItem(
                            "Дата последнего изменения",
                            datatypeRS.getString("changeddate").split(".")[0]
                        )
                    )
                }
                dataSet.add(ModelDataItem("Информация о реквизитах", ""))
                val recordsRS = connection.createStatement().executeQuery(
                    "select d.name as datatype, r.name as record, dataorder from" +
                            " datatypes_recordtypes dr join datatypes d on d.iddatatype = dr.datatype" +
                            " join recordtypes r on r.idrecordtype = dr.recordtype where d.name = '${intent.getStringExtra(
                                "name"
                            )}' order by dataorder asc"
                )
                var counter = 1
                while (recordsRS.next()) {
                    dataSet.add(ModelDataItem("Реквизит $counter", recordsRS.getString("record")))
                    counter++
                }
                connection.close()
            } catch (e: SQLException) {
                Log.e("MyApp", e.toString())
                e.printStackTrace()
            }
            recyclerView.adapter = DataItemAdapter(this, dataSet)
        }
        if (type == "branch") {
            informationTV.text = "Информация о ветке:"
            try {
                val connection = DatabaseConnector().createConnection()
                val branchRS = connection.createStatement().executeQuery(
                    "select * from" +
                            " branches where name = '${intent.getStringExtra("name")}' and deleted = '0'"
                )
                branchRS.next()
                dataSet.add(ModelDataItem("Название", branchRS.getString("name")))
                var higherBranchName = "-"
                if (branchRS.getString("higherbranch") != null) {
                    val higherBranchSet = connection.createStatement().executeQuery(
                        "select name from branches where idbranch = '${branchRS.getString("higherbranch")}'"
                    )
                    if (higherBranchSet.next()) {
                        higherBranchName = higherBranchSet.getString("name")
                    }
                }
                val datatypeSet = connection.createStatement().executeQuery(
                    "select name from datatypes where iddatatype = '${branchRS.getString("datatype")}'"
                )
                datatypeSet.next()
                val datatypeName = datatypeSet.getString("name")
                dataSet.add(ModelDataItem("Находится в ветке", higherBranchName))
                dataSet.add(ModelDataItem("Форма", datatypeName))
                val userNameStmt = connection.createStatement()
                val userNameRS = userNameStmt.executeQuery(
                    "select login from users where iduser =" +
                            " '${branchRS.getString("createdBy")}'"
                )
                userNameRS.next()
                val createdUserName = userNameRS.getString("login")
                dataSet.add(ModelDataItem("Создавший пользователь", createdUserName))
                dataSet.add(
                    ModelDataItem(
                        "Дата создания",
                        branchRS.getString("creationdate").split(".")[0]
                    )
                )
                if (branchRS.getString("changedby") != null && branchRS.getString("changeddate") != null) {
                    val changedUserNameStmt = connection.createStatement()
                    val changedUserNameRS = changedUserNameStmt.executeQuery(
                        "select login from users where iduser =" +
                                " '${branchRS.getString("changedby")}'"
                    )
                    changedUserNameRS.next()
                    val changedUserName = changedUserNameRS.getString("login")
                    dataSet.add(ModelDataItem("Последний изменивший пользователь", changedUserName))
                    dataSet.add(
                        ModelDataItem(
                            "Дата последнего изменения",
                            branchRS.getString("changeddate").split(".")[0]
                        )
                    )
                }
                dataSet.add(ModelDataItem("Привязанные службы", ""))
                val serviceRS = connection.createStatement().executeQuery(
                    "select name from services where idservice in (select service from branches_services where branch = '${branchRS.getString(
                        "idbranch"
                    )}')"
                )
                var counter = 1
                while (serviceRS.next()) {
                    dataSet.add(ModelDataItem("Служба $counter", serviceRS.getString("name")))
                    counter++
                }
                dataSet.add(ModelDataItem("Привязанные районы", ""))
                val districtRS = connection.createStatement().executeQuery(
                    "select name from districts where iddistrict in (select district from branches_districts where branch = '${branchRS.getString(
                        "idbranch"
                    )}')"
                )
                counter = 1
                while (districtRS.next()) {
                    dataSet.add(ModelDataItem("Район $counter", districtRS.getString("name")))
                    counter++
                }
                connection.close()
            } catch (e: SQLException) {
                Log.e("MyApp", e.toString())
                e.printStackTrace()
            }
            recyclerView.adapter = DataItemAdapter(this, dataSet)
        }
        if (type == "dataobject") {
            informationTV.visibility = View.GONE
            try {
                val connection = DatabaseConnector().createConnection()
                val dataObjectRS = connection.createStatement()
                    .executeQuery("select * from dataobjects where name = '${intent.getStringExtra("name")}' and deleted = '0'")
                dataObjectRS.next()
                if (intent.getBooleanExtra("isAdmin", false)) {
                    informationTV.text = "Информация об объекте:"
                    informationTV.visibility = View.VISIBLE
                    dataSet.add(ModelDataItem("Название", dataObjectRS.getString("name")))
                    val brachRS = connection.createStatement().executeQuery(
                        "select name from branches where idbranch = '${dataObjectRS.getString("branch")}'"
                    )
                    brachRS.next()
                    val branchName = brachRS.getString("name")
                    dataSet.add(ModelDataItem("Располагается в ветке", branchName))
                    val userNameStmt = connection.createStatement()
                    val userNameRS = userNameStmt.executeQuery(
                        "select login from users where iduser =" +
                                " '${dataObjectRS.getString("createdBy")}'"
                    )
                    userNameRS.next()
                    val createdUserName = userNameRS.getString("login")
                    dataSet.add(ModelDataItem("Создавший пользователь", createdUserName))
                    dataSet.add(
                        ModelDataItem(
                            "Дата создания",
                            dataObjectRS.getString("creationdate").split(".")[0]
                        )
                    )
                    if (dataObjectRS.getString("changedby") != null && dataObjectRS.getString("changeddate") != null) {
                        val changedUserNameStmt = connection.createStatement()
                        val changedUserNameRS = changedUserNameStmt.executeQuery(
                            "select login from users where iduser =" +
                                    " '${dataObjectRS.getString("changedby")}'"
                        )
                        changedUserNameRS.next()
                        val changedUserName = changedUserNameRS.getString("login")
                        dataSet.add(
                            ModelDataItem(
                                "Последний изменивший пользователь",
                                changedUserName
                            )
                        )
                        dataSet.add(
                            ModelDataItem(
                                "Дата последнего изменения",
                                dataObjectRS.getString("changeddate").split(".")[0]
                            )
                        )
                    }
                    dataSet.add(ModelDataItem("Форма объекта", ""))
                }
                val image = dataObjectRS.getBlob("image")
                if (image != null) {
                    val bytes = image.getBytes(1L, image.length().toInt())
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    objectIV.setImageBitmap(Bitmap.createScaledBitmap(bitmap, 250, 250, false))
                    objectIV.visibility = View.VISIBLE
                }
                val idDataTypeRS = connection.createStatement()
                    .executeQuery("select datatype from branches where idBranch in (select branch from dataobjects where name = '${title}')")
                idDataTypeRS.next()
                val idDataType = idDataTypeRS.getString("datatype")
                val idDataObjectRS = connection.createStatement()
                    .executeQuery("select iddataobject from dataobjects where name = '$title'")
                idDataObjectRS.next()
                val idDataObject = idDataObjectRS.getString("iddataobject")
                val recordTypesRS = connection.createStatement()
                    .executeQuery("select recordtype, r.name, dataorder, dr.deleted from datatypes_recordtypes dr join recordtypes r on r.idrecordtype = dr.recordtype where datatype = '$idDataType' and dr.deleted = '0' order by dataorder")
                while (recordTypesRS.next()) {
                    val valueRS = connection.createStatement().executeQuery(
                        "select value from recordvalues where recordtype = '${recordTypesRS.getString(
                            "recordtype"
                        )}' and dataobject = '$idDataObject'"
                    )
                    valueRS.next()
                    dataSet.add(
                        ModelDataItem(
                            recordTypesRS.getString("name"),
                            valueRS.getString("value")
                        )
                    )
                }
                connection.close()
            } catch (e: SQLException) {
                Log.e("MyApp", e.toString())
                e.printStackTrace()
            }
            recyclerView.adapter = DataItemAdapter(this, dataSet)
        }
        if (type == "event") {
            informationTV.text = "Информация о мероприятии:"
            try {
                val connection = DatabaseConnector().createConnection()
                val eventRS = connection.createStatement()
                    .executeQuery("select * from events where name = '${intent.getStringExtra("name")}' and deleted = '0'")
                eventRS.next()
                dataSet.add(ModelDataItem("Название", eventRS.getString("name")))
                dataSet.add(ModelDataItem("Описание", eventRS.getString("description")))
                dataSet.add(
                    ModelDataItem(
                        "Дата начала",
                        eventRS.getString("timestart").split(".")[0]
                    )
                )
                dataSet.add(
                    ModelDataItem(
                        "Дата окончания",
                        eventRS.getString("timeend").split(".")[0]
                    )
                )
                val userNameRS = connection.createStatement().executeQuery(
                    "select login from users where iduser =" +
                            " '${eventRS.getString("createdBy")}'"
                )
                userNameRS.next()
                val createdUserName = userNameRS.getString("login")
                dataSet.add(ModelDataItem("Создавший пользователь", createdUserName))
                dataSet.add(
                    ModelDataItem(
                        "Дата создания",
                        eventRS.getString("creationdate").split(".")[0]
                    )
                )
                if (eventRS.getString("changedby") != null && eventRS.getString("changeddate") != null) {
                    val changedUserNameStmt = connection.createStatement()
                    val changedUserNameRS = changedUserNameStmt.executeQuery(
                        "select login from users where iduser =" +
                                " '${eventRS.getString("changedby")}'"
                    )
                    changedUserNameRS.next()
                    val changedUserName = changedUserNameRS.getString("login")
                    dataSet.add(ModelDataItem("Последний изменивший пользователь", changedUserName))
                    dataSet.add(
                        ModelDataItem(
                            "Дата последнего изменения",
                            eventRS.getString("changeddate").split(".")[0]
                        )
                    )
                }
                dataSet.add(ModelDataItem("Привязанные службы", ""))
                val serviceRS = connection.createStatement().executeQuery(
                    "select name from services where idservice in (select service from events_services where event = '${eventRS.getString(
                        "idevent"
                    )}' and deleted = '0')"
                )
                var counter = 1
                while (serviceRS.next()) {
                    dataSet.add(ModelDataItem("Служба $counter", serviceRS.getString("name")))
                    counter++
                }
                dataSet.add(ModelDataItem("Привязанные районы", ""))
                val districtRS = connection.createStatement().executeQuery(
                    "select name from districts where iddistrict in (select district from events_districts where event = '${eventRS.getString(
                        "idevent"
                    )}' and deleted = '0')"
                )
                counter = 1
                while (districtRS.next()) {
                    dataSet.add(ModelDataItem("Район $counter", districtRS.getString("name")))
                    counter++
                }
                connection.close()
            } catch (e: SQLException) {
                Log.e("MyApp", e.toString())
                e.printStackTrace()
            }
            recyclerView.adapter = DataItemAdapter(this, dataSet)
        }
        if (type == "currentEvent") {
            informationTV.visibility = View.GONE
            try {
                val connection = DatabaseConnector().createConnection()
                val eventRS = connection.createStatement()
                    .executeQuery("select * from events where name = '${intent.getStringExtra("name")}' and deleted = '0'")
                eventRS.next()
                dataSet.add(ModelDataItem("Описание", eventRS.getString("description")))
                dataSet.add(
                    ModelDataItem(
                        "Дата начала",
                        eventRS.getString("timestart").split(".")[0]
                    )
                )
                dataSet.add(
                    ModelDataItem(
                        "Дата окончания",
                        eventRS.getString("timeend").split(".")[0]
                    )
                )
                connection.close()
            } catch (e: SQLException) {
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