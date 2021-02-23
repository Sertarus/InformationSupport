package com.application.informationsupport

import android.database.SQLException
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.application.informationsupport.adapters.BranchAdapter
import com.application.informationsupport.adapters.DataTypeAdapter
import com.application.informationsupport.adapters.SimpleItemAdapter
import com.application.informationsupport.adapters.UserAdapter
import com.application.informationsupport.database.DatabaseConnector
import com.application.informationsupport.models.ModelBranchInfo
import com.application.informationsupport.models.ModelMainUserInfo
import com.application.informationsupport.models.ModelSimpleInfo
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*

class AdminActivity : AppCompatActivity() {

    private var currentData = ""
    var serviceAdapter = SimpleItemAdapter(
        this, mutableListOf(),
        "", currentData
    )
    var userAdapter = UserAdapter(this, mutableListOf(), "")
    var datatypeAdapter = DataTypeAdapter(this, mutableListOf(), "")
    var branchAdapter = BranchAdapter(this, mutableListOf(), "")
    lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        title = "Панель администратора"
        val chooseDatabaseTypeObjectButton =
            findViewById<FloatingActionButton>(R.id.floatingChooseButton)
        val addObjectButton = findViewById<FloatingActionButton>(R.id.floatingAddButton)
        recyclerView = findViewById(R.id.dataRecyclerView)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        serviceAdapter = SimpleItemAdapter(
            this, mutableListOf(),
            intent.getStringExtra("name")!!, currentData
        )
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

                    0 -> {
                        title = "Пользователи"
                        currentData = "user"
                        userAdapter =
                            UserAdapter(this, mutableListOf(), intent.getStringExtra("name")!!)
                        userAdapter.refreshUserInfo()
                        recyclerView.adapter = userAdapter
                    }

                    1 -> {
                        title = "Службы"
                        currentData = "service"
                        serviceAdapter = SimpleItemAdapter(
                            this, mutableListOf(),
                            intent.getStringExtra("name")!!, currentData
                        )
                        serviceAdapter.refreshSimpleInfo()
                        recyclerView.adapter = serviceAdapter
                    }

                    2 -> {
                        title = "Районы"
                        currentData = "district"
                        serviceAdapter = SimpleItemAdapter(
                            this, mutableListOf(),
                            intent.getStringExtra("name")!!, currentData
                        )
                        serviceAdapter.refreshSimpleInfo()
                        recyclerView.adapter = serviceAdapter
                    }

                    3 -> {
                        title = "Устройства"
                        currentData = "device"
                        serviceAdapter = SimpleItemAdapter(
                            this, mutableListOf(),
                            intent.getStringExtra("name")!!, currentData
                        )
                        serviceAdapter.refreshSimpleInfo()
                        recyclerView.adapter = serviceAdapter
                    }
                    4 -> {
                        title = "Ветки"
                        currentData = "branch"
                        branchAdapter = BranchAdapter(this, mutableListOf(), intent.getStringExtra("name")!!)
                        branchAdapter.refreshBranches()
                        recyclerView.adapter = branchAdapter
                    }
                    5 -> {
                        title = "Формы"
                        currentData = "datatype"
                        datatypeAdapter = DataTypeAdapter(this, mutableListOf(), intent.getStringExtra("name")!!)
                        datatypeAdapter.refreshDataTypes()
                        recyclerView.adapter = datatypeAdapter
                    }
                }
            }
            builder.create().show()
        }

        addObjectButton.setOnClickListener {
            if (currentData == "service" || currentData == "district" || currentData == "device") {
                createSimpleDialog()
            }
            if (currentData == "user") {
                userAdapter.createOrEditUser("", false)
            }
            if (currentData == "datatype") {
                datatypeAdapter.createOrEditDatatype("", false)
            }
            if (currentData == "branch") {
                branchAdapter.createOrEditBranch("", false)
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_search, menu)
        val item = menu!!.findItem(R.id.action_search)
        val searchView = item.actionView as androidx.appcompat.widget.SearchView
        searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (currentData != "") {
                    if (!TextUtils.isEmpty(query!!.trim())) {
                        searchItems(query)
                    } else {
                        if (currentData == "service" || currentData == "district" || currentData == "device") {
                            serviceAdapter.refreshSimpleInfo()
                        } else if (currentData == "user") {
                            userAdapter.refreshUserInfo()
                        }
                    }
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (currentData != "") {
                    if (!TextUtils.isEmpty(newText!!.trim())) {
                        searchItems(newText)
                    } else {
                        if (currentData == "service" || currentData == "district" || currentData == "device") {
                            serviceAdapter.refreshSimpleInfo()
                        } else if (currentData == "user") {
                            userAdapter.refreshUserInfo()
                        }
                    }
                }
                return false
            }

        })
        return super.onCreateOptionsMenu(menu)
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

    private fun searchItems(query: String) {
        if (currentData == "service" || currentData == "district" || currentData == "device") {
            val dataSet = mutableListOf<ModelSimpleInfo>()
            try {
                val connection = DatabaseConnector().createConnection()
                val stmt = connection.createStatement()
                val rs = stmt.executeQuery(
                    "select * from ${currentData}s where (lower(name) like'%${query.toLowerCase(Locale.ROOT)}%' or" +
                            " createdby like '%$query%' or changedby like '%$query%' or" +
                            " creationdate like '%$query%' or changeddate like '%$query%') and deleted = '0'"
                )
                while (rs.next()) {
                    val newStmt = connection.createStatement()
                    val nameSet = newStmt.executeQuery(
                        "select login from users where iduser = '" +
                                rs.getString("createdby") + "'"
                    )
                    nameSet.next()
                    val name = nameSet.getString("login")
                    dataSet.add(
                        ModelSimpleInfo(
                            rs.getString("name"),
                            name,
                            rs.getTimestamp("creationdate").toString().split(".")[0]
                        )
                    )
                }
                connection.close()
            } catch (e: SQLException) {
                Log.e("MyApp", e.toString())
                e.printStackTrace()
            }
            serviceAdapter =
                SimpleItemAdapter(this, dataSet, intent.getStringExtra("name")!!, currentData)
            recyclerView.adapter = serviceAdapter
        }
        if (currentData == "user") {
            var roleQuery = "3"
            if ("Пользователь".contains(query, true)) {
                roleQuery = "0"
            }
            if ("Локальный".contains(query, true)) {
                roleQuery = "1"
            }
            if ("Администратор".contains(query, true)) {
                roleQuery = "2"
            }
            val dataSet = mutableListOf<ModelMainUserInfo>()
            try {
                val connection = DatabaseConnector().createConnection()
                val stmt = connection.createStatement()
                val rs = stmt.executeQuery(
                    "select login, fullname, role, email, phonenumber, s.name as service," +
                            " dis.name as district, dev.name as device, u.createdby, u.creationdate," +
                            " u.deleted from users u" +
                            " join services s on s.idservice = u.service" +
                            " join districts dis on dis.iddistrict = u.district" +
                            " join devices dev on dev.iddevice = u.device" +
                            " where (lower(login) like '%${query.toLowerCase(Locale.ROOT)}%' or" +
                            " lower(fullname)  like '%${query.toLowerCase(Locale.ROOT)}%' or role like '%$roleQuery%' or" +
                            " lower(email) like '%${query.toLowerCase(Locale.ROOT)}%' or phonenumber like '%$query%' or" +
                            " lower(s.name) like '%${query.toLowerCase(Locale.ROOT)}%' or lower(dis.name) like '%${query.toLowerCase(Locale.ROOT)}%' or" +
                            " lower(dev.name) like '%${query.toLowerCase(Locale.ROOT)}%') and u.deleted = '0'"
                )
                while (rs.next()) {
                    val newStmt = connection.createStatement()
                    val nameSet = newStmt.executeQuery(
                        "select login from users where iduser = '" +
                                rs.getString("createdby") + "'"
                    )
                    nameSet.next()
                    val name = nameSet.getString("login")
                    dataSet.add(
                        ModelMainUserInfo(
                            rs.getString("login"),
                            rs.getString("fullname"),
                            rs.getString("district"),
                            rs.getString("service"),
                            name,
                            rs.getTimestamp("creationdate").toString().split(".")[0]
                        )
                    )
                }
                connection.close()
            } catch (e: SQLException) {
                Log.e("MyApp", e.toString())
                e.printStackTrace()
            }
            userAdapter = UserAdapter(this, dataSet, intent.getStringExtra("name")!!)
            recyclerView.adapter = userAdapter
        }
        if (currentData == "datatype") {
            val dataSet = mutableListOf<ModelSimpleInfo>()
            try {
                val connection = DatabaseConnector().createConnection()
                val rs = connection.createStatement().executeQuery("select * from datatypes where" +
                        " name like '%$query%' and deleted = '0'")
                while (rs.next()) {
                    val newStmt = connection.createStatement()
                    val nameSet = newStmt.executeQuery(
                        "select login from users where iduser = '" +
                                rs.getString("createdby") + "'"
                    )
                    nameSet.next()
                    val name = nameSet.getString("login")
                    dataSet.add(ModelSimpleInfo(rs.getString("name"), name,
                        rs.getTimestamp("creationdate").toString().split(".")[0]))
                }
                connection.close()
            }
            catch (e: SQLException) {
                Log.e("MyApp", e.toString())
                e.printStackTrace()
            }
            datatypeAdapter = DataTypeAdapter(this, dataSet, intent.getStringExtra("name")!!)
            recyclerView.adapter = datatypeAdapter
        }
        if (currentData == "branch") {
            val dataSet = mutableListOf<ModelBranchInfo>()
            try {
                val connection = DatabaseConnector().createConnection()
                val rs = connection.createStatement().executeQuery("select * from branches where name like '%$query%' and deleted = '0'")
                while (rs.next()) {
                    val newStmt = connection.createStatement()
                    val nameSet = newStmt.executeQuery(
                        "select login from users where iduser = '" +
                                rs.getString("createdby") + "'"
                    )
                    nameSet.next()
                    val name = nameSet.getString("login")
                    var higherBranchName = "-"
                    if (rs.getString("higherbranch") != null){
                        val higherBranchSet = connection.createStatement().executeQuery("select name from branches where idbranch = '${rs.getString("higherbranch")}'")
                        if (higherBranchSet.next()) {
                            higherBranchName = higherBranchSet.getString("name")
                        }
                    }
                    val datatypeSet = connection.createStatement().executeQuery("select name from datatypes where iddatatype = '${rs.getString("datatype")}'")
                    datatypeSet.next()
                    val datatypeName = datatypeSet.getString("name")
                    dataSet.add(ModelBranchInfo(rs.getString("name"), higherBranchName, datatypeName, name, rs.getString("creationDate").split(".")[0]))
                }
                connection.close()
            }
            catch (e: SQLException) {
                Log.e("MyApp", e.toString())
                e.printStackTrace()
            }
            branchAdapter = BranchAdapter(this, dataSet, intent.getStringExtra("name")!!)
            recyclerView.adapter = branchAdapter
        }
    }

    private fun createSimpleDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_edit_simple_info, null)
        val addTV = view.findViewById<TextView>(R.id.addTV)
        var typeString = ""
        when (currentData) {
            "service" -> {
                typeString = addTV.text.toString() + "службу"
            }
            "district" -> {
                typeString = addTV.text.toString() + "район"
            }
            "device" -> {
                typeString = addTV.text.toString() + "устройство"
            }
        }
        addTV.text = typeString
        val nameET = view.findViewById<EditText>(R.id.nameET)
        val button = view.findViewById<Button>(R.id.createSimpleInfoButton)
        val builder = AlertDialog.Builder(this)
        builder.setView(view)
        val ad = builder.create()
        ad.show()
        button.setOnClickListener {
            if (currentData == "service" || currentData == "district" || currentData == "device") {
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
                            "insert into ${currentData}s (name, createdby," +
                                    " creationdate) values ('$name', '$creatorID', TO_TIMESTAMP('$timestamp'," +
                                    " 'YYYY-MM-DD HH24:MI:SS.FF'))"
                        )
                        Toast.makeText(this, "Служба добавлена", Toast.LENGTH_SHORT).show()
                        serviceAdapter.refreshSimpleInfo()
                        connection.close()
                        ad.dismiss()
                    } catch (e: SQLException) {
                        Log.e("MyApp", e.toString())
                        e.printStackTrace()
                    }
                } else {
                    var secondTypeString = ""
                    when (currentData) {
                        "service" -> {
                            secondTypeString = "службы"
                        }
                        "district" -> {
                            secondTypeString = "района"
                        }
                        "device" -> {
                            secondTypeString = "устройства"
                        }
                    }
                    Toast.makeText(
                        this, "Недопустимое имя $secondTypeString",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}