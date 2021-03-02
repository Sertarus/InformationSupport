package com.application.informationsupport

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.SQLException
import android.os.Bundle
import android.provider.MediaStore
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.application.informationsupport.adapters.*
import com.application.informationsupport.database.DatabaseConnector
import com.application.informationsupport.models.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*

class AdminActivity : AppCompatActivity() {

    val CAMERA_REQUEST_CODE = 100
    val STORAGE_REQUEST_CODE = 200
    val IMAGE_PICK_GALLERY_CODE = 300
    val IMAGE_PICK_CAMERA_CODE = 400

    private var currentData = ""
    var serviceAdapter = SimpleItemAdapter(
        this, mutableListOf(),
        "", currentData
    )
    var userAdapter = UserAdapter(this, mutableListOf(), "")
    var datatypeAdapter = DataTypeAdapter(this, mutableListOf(), "")
    var branchAdapter = BranchAdapter(this, mutableListOf(), "")
    var dataObjectAdapter = DataObjectAdapter(this, mutableListOf(), "")
    var eventAdapter = EventAdapter(this, mutableListOf(), "")
    lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        title = "Панель администратора"
        val configuration = this.getResources().getConfiguration()
        val locale = Locale("ru", "RU")
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        this.createConfigurationContext(configuration)
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
                "Объекты данных",
                "Мероприятия"
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
                        branchAdapter =
                            BranchAdapter(this, mutableListOf(), intent.getStringExtra("name")!!)
                        branchAdapter.refreshBranches()
                        recyclerView.adapter = branchAdapter
                    }
                    5 -> {
                        title = "Формы"
                        currentData = "datatype"
                        datatypeAdapter =
                            DataTypeAdapter(this, mutableListOf(), intent.getStringExtra("name")!!)
                        datatypeAdapter.refreshDataTypes()
                        recyclerView.adapter = datatypeAdapter
                    }
                    6 -> {
                        title = "Объекты данных"
                        currentData = "dataobject"
                        dataObjectAdapter = DataObjectAdapter(
                            this,
                            mutableListOf(),
                            intent.getStringExtra("name")!!
                        )
                        dataObjectAdapter.refreshDataObjects()
                        recyclerView.adapter = dataObjectAdapter
                    }
                    7 -> {
                        title = "Мероприятия"
                        currentData = "event"
                        eventAdapter =
                            EventAdapter(this, mutableListOf(), intent.getStringExtra("name")!!)
                        eventAdapter.refreshEvents()
                        recyclerView.adapter = eventAdapter
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
            if (currentData == "dataobject") {
                dataObjectAdapter.createOrEditDataObject("", false)
            }
            if (currentData == "event") {
                eventAdapter.createOrEditEvent("", false)
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
                        searchItems("")
                    }
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (currentData != "") {
                    if (!TextUtils.isEmpty(newText!!.trim())) {
                        searchItems(newText)
                    } else {
                        searchItems("")
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
                    "select * from ${currentData}s where (lower(name) like'%${query.toLowerCase(
                        Locale.ROOT
                    )}%' or" +
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
                            " lower(s.name) like '%${query.toLowerCase(Locale.ROOT)}%' or lower(dis.name) like '%${query.toLowerCase(
                                Locale.ROOT
                            )}%' or" +
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
                val rs = connection.createStatement().executeQuery(
                    "select * from datatypes where" +
                            " lower(name) like '%${query.toLowerCase(Locale.ROOT)}%' and deleted = '0'"
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
                            rs.getString("name"), name,
                            rs.getTimestamp("creationdate").toString().split(".")[0]
                        )
                    )
                }
                connection.close()
            } catch (e: SQLException) {
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
                val rs = connection.createStatement()
                    .executeQuery(
                        "select * from branches where lower(name) like '%${query.toLowerCase(
                            Locale.ROOT
                        )}%' and deleted = '0'"
                    )
                while (rs.next()) {
                    val newStmt = connection.createStatement()
                    val nameSet = newStmt.executeQuery(
                        "select login from users where iduser = '" +
                                rs.getString("createdby") + "'"
                    )
                    nameSet.next()
                    val name = nameSet.getString("login")
                    var higherBranchName = "-"
                    if (rs.getString("higherbranch") != null) {
                        val higherBranchSet = connection.createStatement().executeQuery(
                            "select name from branches where idbranch = '${rs.getString("higherbranch")}'"
                        )
                        if (higherBranchSet.next()) {
                            higherBranchName = higherBranchSet.getString("name")
                        }
                    }
                    val datatypeSet = connection.createStatement().executeQuery(
                        "select name from datatypes where iddatatype = '${rs.getString("datatype")}'"
                    )
                    datatypeSet.next()
                    val datatypeName = datatypeSet.getString("name")
                    dataSet.add(
                        ModelBranchInfo(
                            rs.getString("name"),
                            higherBranchName,
                            datatypeName,
                            name,
                            rs.getString("creationDate").split(".")[0]
                        )
                    )
                }
                connection.close()
            } catch (e: SQLException) {
                Log.e("MyApp", e.toString())
                e.printStackTrace()
            }
            branchAdapter = BranchAdapter(this, dataSet, intent.getStringExtra("name")!!)
            recyclerView.adapter = branchAdapter
        }
        if (currentData == "dataobject") {
            val dataSet = mutableListOf<ModelDataObject>()
            try {
                val connection = DatabaseConnector().createConnection()
                val rs = connection.createStatement().executeQuery(
                    "select * from dataobjects where (lower(name) like '%${query.toLowerCase(
                        Locale.ROOT
                    )}%' or branch in (select idbranch from branches where lower(name) like '%${query.toLowerCase(
                        Locale.ROOT
                    )}%')) and deleted = '0'"
                )
                while (rs.next()) {
                    val newStmt = connection.createStatement()
                    val nameSet = newStmt.executeQuery(
                        "select login from users where iduser = '" +
                                rs.getString("createdby") + "'"
                    )
                    nameSet.next()
                    val name = nameSet.getString("login")
                    val branchSet = connection.createStatement().executeQuery(
                        "select name from branches where idbranch = '${rs.getString("branch")}' and deleted = '0'"
                    )
                    branchSet.next()
                    val branch = branchSet.getString("name")
                    dataSet.add(
                        ModelDataObject(
                            rs.getString("name"),
                            branch,
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
            dataObjectAdapter = DataObjectAdapter(this, dataSet, intent.getStringExtra("name")!!)
            recyclerView.adapter = dataObjectAdapter
        }
        if (currentData == "event") {
            val dataSet = mutableListOf<ModelEvent>()
            try {
                val connection = DatabaseConnector().createConnection()
                val rs = connection.createStatement().executeQuery(
                    "select * from events where (lower(name) like '%${query.toLowerCase(
                        Locale.ROOT
                    )}%' and deleted = '0') or idevent in (select event from events_services where service in (select idservice from services where lower(name) like '%${query.toLowerCase(
                        Locale.ROOT
                    )}%') and deleted = '0') or idevent in (select event from events_districts where district in (select iddistrict from districts where lower(name) like '%${query.toLowerCase(
                        Locale.ROOT
                    )}%') and deleted = '0')"
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
                        ModelEvent(
                            rs.getString("name"),
                            name,
                            rs.getString("timestart").split(".")[0],
                            rs.getString("timeend").split(".")[0]
                        )
                    )
                }
                connection.close()
            } catch (e: SQLException) {
                Log.e("MyApp", e.toString())
                e.printStackTrace()
            }
            eventAdapter = EventAdapter(this, dataSet, intent.getStringExtra("name")!!)
            recyclerView.adapter = eventAdapter
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
                        val ifNameExist = connection.createStatement()
                            .executeQuery("select * from ${currentData}s where name = '$name'")
                        if (ifNameExist.next()) {
                            Toast.makeText(
                                this,
                                "Объект с таким именем уже существует",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
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
                            ad.dismiss()
                        }
                        connection.close()

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                val imageUri = data!!.data!!
                dataObjectAdapter.currentUri = imageUri
                dataObjectAdapter.updateIV()
            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                dataObjectAdapter.updateIV()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if (cameraAccepted && writeStorageAccepted) {
                        pickFromCamera()
                    } else {
                        Toast.makeText(
                            this,
                            "Please enable camera and storage permission",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            STORAGE_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    val writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if (writeStorageAccepted) {
                        pickFromGallery()
                    } else {
                        Toast.makeText(
                            this,
                            "Please enable storage permission",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun pickFromGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK)
        galleryIntent.type = "image/*"
        ActivityCompat.startActivityForResult(
            this,
            galleryIntent,
            IMAGE_PICK_GALLERY_CODE,
            Bundle()
        )
    }

    private fun pickFromCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Temp pic")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp description")
        val imageUri =
            this.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )!!
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        ActivityCompat.startActivityForResult(
            this,
            cameraIntent,
            IMAGE_PICK_CAMERA_CODE,
            Bundle()
        )
    }
}