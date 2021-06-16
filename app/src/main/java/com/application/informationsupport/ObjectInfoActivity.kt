package com.application.informationsupport

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.SQLException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.application.informationsupport.adapters.DataItemAdapter
import com.application.informationsupport.adapters.ImageAdapter
import com.application.informationsupport.database.DatabaseConnector
import com.application.informationsupport.models.ModelDataItem
import com.google.android.material.textfield.TextInputLayout
import com.squareup.picasso.Picasso
import java.io.*
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class ObjectInfoActivity : AppCompatActivity() {

    var image_uri: Uri? = null
    val CAMERA_REQUEST_CODE = 100
    val STORAGE_REQUEST_CODE = 200
    val IMAGE_PICK_GALLERY_CODE = 300
    val IMAGE_PICK_CAMERA_CODE = 400
    protected lateinit var storagePermissions: Array<String>
    protected lateinit var cameraPermissions: Array<String>
    val formRecordList = mutableListOf<Pair<String, String>>()
    val oldRecordList = mutableListOf<Pair<String, String>>()
    protected lateinit var imageView: AppCompatImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_object_info)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        title = intent.getStringExtra("name")
        val informationTV = findViewById<TextView>(R.id.informationTV)
        val imageRecyclerView = findViewById<RecyclerView>(R.id.ImageRecyclerView)
        imageRecyclerView.layoutManager = LinearLayoutManager(this)
        val imageSet = mutableListOf<Bitmap>()
        val changeButton = findViewById<Button>(R.id.changeButton)
        cameraPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val recyclerView = findViewById<RecyclerView>(R.id.dataItemRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val dataSet = mutableListOf<ModelDataItem>()
        val type = intent.getStringExtra("type")
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
        if (type == "dataobject") {
            informationTV.visibility = View.GONE
            changeButton.visibility = View.VISIBLE
            try {
                val connection = DatabaseConnector(url, username, pass).createConnection()
                val dataObjectRS = connection.createStatement()
                    .executeQuery("select * from dataobjects where name = '${intent.getStringExtra("name")}' and deleted = '0'")

                dataObjectRS.next()
                val image = dataObjectRS.getBlob("image")
                if (image != null) {
                    val bytes = image.getBytes(1L, image.length().toInt())
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    imageSet.add(bitmap)
                    val oldImagesRS = connection.createStatement().executeQuery("select image, creationdate from old_images where dataobject in (select iddataobject from dataobjects where name = '${intent.getStringExtra("name")}' and deleted = 0) and deleted = 0 order by creationdate desc")
                    while (oldImagesRS.next()) {
                        val oldImage = oldImagesRS.getBlob("image")
                        val oldBytes = oldImage.getBytes(1L, oldImage.length().toInt())
                        val oldBitmap = BitmapFactory.decodeByteArray(oldBytes, 0, oldBytes.size)
                        imageSet.add(oldBitmap)
                    }
                    imageRecyclerView.adapter = ImageAdapter(this, imageSet)
                }
                val idDataTypeRS = connection.createStatement()
                    .executeQuery("select datatype from branches where deleted = '0' and idBranch in (select branch from dataobjects where deleted = '0' and name = '${title}')")
                idDataTypeRS.next()
                val idDataType = idDataTypeRS.getString("datatype")
                val idDataObjectRS = connection.createStatement()
                    .executeQuery("select iddataobject from dataobjects where name = '$title' and deleted = '0'")
                idDataObjectRS.next()
                val idDataObject = idDataObjectRS.getString("iddataobject")
                val recordTypesRS = connection.createStatement()
                    .executeQuery("select recordtype, r.name, dataorder, dr.deleted from datatypes_recordtypes dr join recordtypes r on r.idrecordtype = dr.recordtype where datatype = '$idDataType' and dr.deleted = '0' order by dataorder")
                while (recordTypesRS.next()) {
                    val valueRS = connection.createStatement().executeQuery(
                        "select value from recordvalues where recordtype = '${
                            recordTypesRS.getString(
                                "recordtype"
                            )
                        }' and dataobject = '$idDataObject'"
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
            } catch (e: Exception) {
                try {
                    val logfile =
                        File(Environment.getExternalStorageDirectory().absolutePath, "log.txt")
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
                    } else {
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
                } catch (e: Exception) {

                }
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
            changeButton.setOnClickListener {
                formRecordList.clear()
                oldRecordList.clear()
                val view = LayoutInflater.from(this)
                    .inflate(R.layout.dialog_change_data, null)
                imageView = view.findViewById(R.id.imageView)
                val imageButton = view.findViewById<Button>(R.id.imageButton)
                val fillFormButton = view.findViewById<Button>(R.id.fillFormButton)
                val createButton = view.findViewById<Button>(R.id.createDataObjectButton)
                try {
                    val connection = DatabaseConnector(url, username, pass).createConnection()
                    val imageRS = connection.createStatement()
                        .executeQuery("select image from dataobjects where name = '$title'")
                    imageRS.next()
                    if (imageRS.getBlob("image") != null) {
                        val image = imageRS.getBlob("image")
                        val bytes = image.getBytes(1L, image.length().toInt())
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, 200, 200, false))
                    }
                    connection.close()
                } catch (e: Exception) {
                    val file = File(filesDir, "log_error")
                    if (!file.exists()) {
                        file.mkdir()
                    }
                    try {
                        val logfile = File(file, "log")
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
                            myOutWriter.close()
                            fout.close()
                        } else {
                            val writer = FileWriter(logfile)
                            writer.append(date.toString())
                            writer.append("\n")
                            writer.append(e.toString())
                            writer.flush()
                            writer.close()
                        }
                    } catch (e: Exception) {
                    }
                }
                imageButton.setOnClickListener {
                    showImagePicDialog()
                }
                fillFormButton.setOnClickListener {
                    val fillFormView = LayoutInflater.from(this)
                        .inflate(R.layout.dialog_fill_form, null)
                    val dataLL = fillFormView.findViewById<LinearLayout>(R.id.dataLL)
                    try {
                        val connection = DatabaseConnector(url, username, pass).createConnection()
                        when {
                            formRecordList.isNotEmpty() -> {
                                formRecordList.forEach {
                                    val textInputLayout = TextInputLayout(this)
                                    textInputLayout.layoutParams =
                                        LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                                    textInputLayout.setPadding(10, 10, 10, 10)
                                    dataLL.addView(textInputLayout)
                                    val editText = EditText(this)
                                    editText.layoutParams =
                                        LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                                    editText.hint = it.first
                                    editText.text = SpannableStringBuilder(it.second)
                                    textInputLayout.addView(editText)
                                }
                            }
                            else -> {
                                val idDataTypeRS = connection.createStatement()
                                    .executeQuery("select datatype from branches where idBranch in (select branch from dataobjects where name = '$title')")
                                idDataTypeRS.next()
                                val idDataType = idDataTypeRS.getString("datatype")
                                val idDataObjectRS = connection.createStatement()
                                    .executeQuery("select iddataobject from dataobjects where name = '$title' and deleted = 0")
                                idDataObjectRS.next()
                                val idDataObject = idDataObjectRS.getString("iddataobject")
                                val recordTypesRS = connection.createStatement()
                                    .executeQuery("select recordtype, r.name, dataorder, dr.deleted from datatypes_recordtypes dr join recordtypes r on r.idrecordtype = dr.recordtype where datatype = '$idDataType' and dr.deleted = '0' order by dataorder")
                                while (recordTypesRS.next()) {
                                    val textInputLayout = TextInputLayout(this)
                                    textInputLayout.layoutParams =
                                        LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                                    textInputLayout.setPadding(10, 10, 10, 10)
                                    dataLL.addView(textInputLayout)
                                    val editText = EditText(this)
                                    editText.layoutParams =
                                        LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                                    editText.hint = recordTypesRS.getString("name")
                                    val recordValueRS = connection.createStatement().executeQuery(
                                        "select value from recordvalues where recordtype = '${
                                            recordTypesRS.getString(
                                                "recordtype"
                                            )
                                        }' and dataobject = '$idDataObject' and deleted = 0"
                                    )
                                    recordValueRS.next()
                                    formRecordList.add(
                                        Pair(
                                            recordTypesRS.getString("name"),
                                            recordValueRS.getString("value")
                                        )
                                    )
                                    oldRecordList.add(
                                        Pair(
                                            recordTypesRS.getString("name"),
                                            recordValueRS.getString("value")
                                        )
                                    )
                                    editText.text =
                                        SpannableStringBuilder(recordValueRS.getString("value"))
                                    textInputLayout.addView(editText)
                                }
                            }
                        }
                        connection.close()
                    } catch (e: Exception) {
                        val file = File(filesDir, "log_error")
                        if (!file.exists()) {
                            file.mkdir()
                        }
                        try {
                            val logfile = File(file, "log")
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
                                myOutWriter.close()
                                fout.close()
                            } else {
                                val writer = FileWriter(logfile)
                                writer.append(date.toString())
                                writer.append("\n")
                                writer.append(e.toString())
                                writer.flush()
                                writer.close()
                            }
                        } catch (e: Exception) {
                        }
                    }
                    val button = Button(this, null, 0, R.style.Widget_AppCompat_Button_Colored)
                    button.text = "Заполнить"
                    button.layoutParams =
                        LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                            weight = 1.0f
                            gravity = Gravity.CENTER
                        }
                    button.setPadding(10, 10, 10, 10)
                    dataLL.addView(button)
                    val changeBuilder = AlertDialog.Builder(this)
                    changeBuilder.setView(fillFormView)
                    val ad = changeBuilder.create()
                    ad.show()
                    button.setOnClickListener {
                        var isRecordsCorrect = true
                        for (i in 1 until dataLL.childCount - 1) {
                            val TIL = dataLL.getChildAt(i) as TextInputLayout
                            if (TIL.editText!!.text.length !in 1..150) {
                                isRecordsCorrect = false
                                break
                            }
                        }
                        if (!isRecordsCorrect) {
                            Toast.makeText(
                                this,
                                "Длина значения каждого реквизита должна быть от 1 до 150 символов",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            formRecordList.clear()
                            for (i in 1 until dataLL.childCount - 1) {
                                val TIL = dataLL.getChildAt(i) as TextInputLayout
                                formRecordList.add(
                                    Pair(
                                        TIL.hint.toString(),
                                        TIL.editText!!.text.toString()
                                    )
                                )
                            }
                            ad.dismiss()
                        }
                    }
                }
                val changeBuilder = AlertDialog.Builder(this)
                changeBuilder.setView(view)
                val ad = changeBuilder.create()
                ad.show()
                createButton.setOnClickListener {
                    var encodedImage: ByteArray? = null
                    if (image_uri != null) {
                        val bitmap =
                            MediaStore.Images.Media.getBitmap(this.contentResolver, image_uri)
                        val baos = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos)
                        encodedImage = baos.toByteArray()
                    }
                    val connection = DatabaseConnector(url, username, pass).createConnection()
                    val rs = connection.createStatement().executeQuery(
                        "select iduser from users where" +
                                " login = '${intent.getStringExtra("user")}'"
                    )
                    rs.next()
                    val creatorID = rs.getString("iduser")
                    val iddata = connection.createStatement().executeQuery("select iddataobject from dataobjects where name = '$title' and deleted = 0")
                    iddata.next()
                    val dataID = iddata.getString("iddataobject")
                    if (encodedImage != null) {
                        val preparedStatement =
                            connection.prepareStatement("insert into dataobjects_suggested (dataobject, image, createdby, creationdate) values ('$dataID', ?, '$creatorID', SYSTIMESTAMP)")
                        preparedStatement.setBinaryStream(1, ByteArrayInputStream(encodedImage))
                        preparedStatement.executeUpdate()
                    } else {
                        connection.createStatement()
                            .executeQuery("insert into dataobjects_suggested (dataobject, createdby, creationdate) values ('$dataID', '$creatorID', SYSTIMESTAMP)")
                    }
                    val dataObjectRS = connection.createStatement()
                        .executeQuery("select idsuggested from dataobjects_suggested where idsuggested = (select max(idsuggested) from dataobjects_suggested where createdby = $creatorID)")
                    dataObjectRS.next()
                    val dataObjectID = dataObjectRS.getString("idsuggested")
                    for (i in formRecordList.size - 1 downTo 0) {
                        if (formRecordList[i].second == oldRecordList[i].second) {
                            formRecordList.removeAt(i)
                        }
                    }
                    formRecordList.forEach {
                        val recordTypeIDRS = connection.createStatement()
                            .executeQuery("select idrecordtype from recordtypes where name = '${it.first}'")
                        recordTypeIDRS.next()
                        val recordTypeID = recordTypeIDRS.getString("idrecordtype")
                        connection.createStatement()
                            .executeQuery("insert into recordvalues_suggested (value, recordtype, suggesteddataobject, createdby, creationdate) values ('${it.second}', '$recordTypeID', '$dataObjectID', '$creatorID', SYSTIMESTAMP)")
                    }
                    Toast.makeText(this, "Запрос на изменение отправлен", Toast.LENGTH_SHORT).show()
                    ad.dismiss()
                    connection.close()
                }
            }
            recyclerView.adapter = DataItemAdapter(this, dataSet)
        }
        if (type == "currentEvent") {
            informationTV.visibility = View.GONE
            try {
                val connection = DatabaseConnector(url, username, pass).createConnection()
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
            } catch (e: Exception) {
                try {
                    val logfile =
                        File(Environment.getExternalStorageDirectory().absolutePath, "log.txt")
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
                    } else {
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
                } catch (e: Exception) {

                }
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
            recyclerView.adapter = DataItemAdapter(this, dataSet)
        }
    }

    private fun showImagePicDialog() {
        val options = arrayOf("Камера", "Галерея")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Откуда получить нужно получить изображение?")
        builder.setItems(options) { _, which ->
            if (which == 0) {
                if (!checkCameraPermission()) {
                    requestCameraPermission()
                } else {
                    pickFromCamera()
                }
            } else if (which == 1) {
                if (!checkStoragePermission()) {
                    requestStoragePermission()
                } else {
                    pickFromGallery()
                }
            }
        }
        builder.create().show()
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            storagePermissions,
            STORAGE_REQUEST_CODE
        )
    }

    private fun checkCameraPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val result2 = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        return result && result2
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            cameraPermissions,
            CAMERA_REQUEST_CODE
        )
    }

    private fun pickFromGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK)
        galleryIntent.type = "image/*"
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_CODE)
    }

    private fun pickFromCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Temp pic")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp description")
        image_uri =
            this.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )!!
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                image_uri = data!!.data!!
                Picasso.get().load(image_uri).resize(200, 200).centerCrop().into(imageView)
            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                Picasso.get().load(image_uri).resize(200, 200).centerCrop().into(imageView)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
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