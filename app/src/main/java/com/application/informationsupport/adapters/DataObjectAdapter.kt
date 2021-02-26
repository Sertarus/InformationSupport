package com.application.informationsupport.adapters

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.SQLException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import com.application.informationsupport.ObjectInfoActivity
import com.application.informationsupport.R
import com.application.informationsupport.database.DatabaseConnector
import com.application.informationsupport.models.ModelDataObject
import com.google.android.material.textfield.TextInputLayout
import com.squareup.picasso.Picasso
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class DataObjectAdapter(
    val context: Activity,
    var dataObjectList: List<ModelDataObject>,
    var currentUser: String
) : RecyclerView.Adapter<DataObjectAdapter.DataObjectHolder>() {

    val CAMERA_REQUEST_CODE = 100
    val STORAGE_REQUEST_CODE = 200
    val IMAGE_PICK_GALLERY_CODE = 300
    val IMAGE_PICK_CAMERA_CODE = 400
    val cameraPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    val storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    lateinit var currentUri: Uri
    lateinit var imageView: ImageView
    val formRecordList = mutableListOf<Pair<String, String>>()
    val oldRecordList = mutableListOf<Pair<String, String>>()

    class DataObjectHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var nameTV: TextView = itemView.findViewById(R.id.nameTV)
        var branchTV: TextView = itemView.findViewById(R.id.branchTV)
        var creatorTV: TextView = itemView.findViewById(R.id.creatorTV)
        var dateTV: TextView = itemView.findViewById(R.id.dateTV)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataObjectHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.row_data_object, parent, false)
        return DataObjectHolder(view)
    }

    override fun getItemCount(): Int {
        return dataObjectList.size
    }

    override fun onBindViewHolder(holder: DataObjectHolder, position: Int) {
        val name = dataObjectList[position].name
        val branch = dataObjectList[position].branch
        val creator = dataObjectList[position].creator
        val creationDate = dataObjectList[position].creationDate

        holder.nameTV.text = name
        holder.branchTV.text = branch
        holder.creatorTV.text = creator
        holder.dateTV.text = creationDate

        holder.itemView.setOnClickListener {
            val options = arrayOf(
                "Просмотр",
                "Изменение",
                "Удаление"
            )
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Выберите действие")
            builder.setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(context, ObjectInfoActivity::class.java)
                        intent.putExtra("name", name)
                        intent.putExtra("type", "dataobject")
                        context.startActivity(intent)
                    }

                    1 -> {
                        createOrEditDataObject(holder.nameTV.text.toString(), true)
                    }
                    2-> {
                        try {
                            val connection = DatabaseConnector().createConnection()
                            val idStmt = connection.createStatement()
                            val rs = idStmt.executeQuery(
                                "select iduser from users where" +
                                        " login = '$currentUser'"
                            )
                            rs.next()
                            val creatorID = rs.getString("iduser")
                            val dataObjectIDRS = connection.createStatement().executeQuery("select iddataobject from dataobjects where name = '${holder.nameTV.text}'")
                            dataObjectIDRS.next()
                            val dataObjectID = dataObjectIDRS.getString("iddataobject")
                            connection.createStatement().executeQuery("update dataobjects set changedby = '$creatorID', changeddate = SYSTIMESTAMP, deleted = '1' where name = '${holder.nameTV.text}'")
                            connection.createStatement().executeQuery("update recordvalues set changedby = '$creatorID', changeddate = SYSTIMESTAMP, deleted = '1' where dataobject = '$dataObjectID'")
                        }
                        catch (e: SQLException) {
                            Log.e("MyApp", e.toString())
                            e.printStackTrace()
                        }
                        Toast.makeText(context, "Объект удалён", Toast.LENGTH_SHORT).show()
                        refreshDataObjects()
                    }
                }
            }
            builder.create().show()
        }
    }

    fun refreshDataObjects() {
        val dataSet = mutableListOf<ModelDataObject>()
        try {
            val connection = DatabaseConnector().createConnection()
            val rs = connection.createStatement()
                .executeQuery("select * from dataobjects where deleted = '0'")
            while (rs.next()) {
                val newStmt = connection.createStatement()
                val nameSet = newStmt.executeQuery(
                    "select login from users where iduser = '" +
                            rs.getString("createdby") + "'"
                )
                nameSet.next()
                val name = nameSet.getString("login")
                val branchSet = connection.createStatement().executeQuery(
                    "select name from branches where idbranch = '${rs.getString("branch")}' and deleted = '0'")
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
        this.dataObjectList = dataSet
        this.notifyDataSetChanged()
    }

    fun createOrEditDataObject(chosenDataObjectName: String, isEdit: Boolean) {
        formRecordList.clear()
        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_create_edit_data_object, null)
        val changeTV = view.findViewById<TextView>(R.id.addTV)
        val nameET = view.findViewById<EditText>(R.id.nameET)
        imageView = view.findViewById<ImageView>(R.id.imageView)
        val imageButton = view.findViewById<Button>(R.id.imageButton)
        val branchSpinner = view.findViewById<Spinner>(R.id.spinnerBranch)
        val fillFormButton = view.findViewById<Button>(R.id.fillFormButton)
        val createButton = view.findViewById<Button>(R.id.createDataObjectButton)
        val branchData = mutableListOf<String>()
        try {
            val connection = DatabaseConnector().createConnection()
            if (isEdit) {
                changeTV.text = "Изменить объект данных"
                nameET.text = SpannableStringBuilder(chosenDataObjectName)
                fillFormButton.text = "Изменить форму"
                createButton.text = "Изменить"
                branchSpinner.visibility = View.GONE
                val imageRS = connection.createStatement()
                    .executeQuery("select image from dataobjects where name = '$chosenDataObjectName'")
                imageRS.next()
                if (imageRS.getBlob("image") != null) {
                    val image = imageRS.getBlob("image")
                    val bytes = image.getBytes(1L, image.length().toInt())
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, 200, 200, false))
                }
                else {
                    currentUri = Uri.EMPTY
                }
            } else {
                val branchRS = connection.createStatement()
                    .executeQuery("select name from branches where deleted = '0'")
                while (branchRS.next()) {
                    branchData.add(branchRS.getString("name"))
                }
                branchSpinner.adapter =
                    ArrayAdapter(context, android.R.layout.simple_spinner_item, branchData)
                branchSpinner.setSelection(0)
                currentUri = Uri.EMPTY
            }
            connection.close()
        } catch (e: SQLException) {
            Log.e("MyApp", e.toString())
            e.printStackTrace()
        }

        branchSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                formRecordList.clear()
            }

        }

        imageButton.setOnClickListener {
            val options = arrayOf("Камера", "Галерея")
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Выбрать изображение из:")
            builder.setItems(options) { _, which ->
                if (which == 0) {
                    if (!checkCameraPermission()) {
                        requestCameraPermission()
                    } else {
                        pickFromCamera()
                    }
                }
                if (which == 1) {
                    if (!checkStoragePermission()) {
                        requestStoragePermission()
                    } else {
                        pickFromGallery()
                    }
                }
            }
            builder.create().show()
        }

        fillFormButton.setOnClickListener {
            val fillFormView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_fill_form, null)
            val dataLL = fillFormView.findViewById<LinearLayout>(R.id.dataLL)
            try {
                val connection = DatabaseConnector().createConnection()
                when {
                    formRecordList.isNotEmpty() -> {
                        formRecordList.forEach {
                            val textInputLayout = TextInputLayout(context)
                            textInputLayout.layoutParams =
                                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                            textInputLayout.setPadding(10, 10, 10, 10)
                            dataLL.addView(textInputLayout)
                            val editText = EditText(context)
                            editText.layoutParams =
                                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                            editText.hint = it.first
                            editText.text = SpannableStringBuilder(it.second)
                            textInputLayout.addView(editText)
                        }
                    }
                    isEdit -> {
                        val idDataTypeRS = connection.createStatement()
                            .executeQuery("select datatype from branches where idBranch in (select branch from dataobjects where name = '$chosenDataObjectName')")
                        idDataTypeRS.next()
                        val idDataType = idDataTypeRS.getString("datatype")
                        val idDataObjectRS = connection.createStatement()
                            .executeQuery("select iddataobject from dataobjects where name = '$chosenDataObjectName'")
                        idDataObjectRS.next()
                        val idDataObject = idDataObjectRS.getString("iddataobject")
                        val recordTypesRS = connection.createStatement()
                            .executeQuery("select recordtype, r.name, dataorder, dr.deleted from datatypes_recordtypes dr join recordtypes r on r.idrecordtype = dr.recordtype where datatype = '$idDataType' and dr.deleted = '0' order by dataorder")
                        while (recordTypesRS.next()) {
                            val textInputLayout = TextInputLayout(context)
                            textInputLayout.layoutParams =
                                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                            textInputLayout.setPadding(10, 10, 10, 10)
                            dataLL.addView(textInputLayout)
                            val editText = EditText(context)
                            editText.layoutParams =
                                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                            editText.hint = recordTypesRS.getString("name")
                            val recordValueRS = connection.createStatement().executeQuery(
                                "select value from recordvalues where recordtype = '${recordTypesRS.getString(
                                    "recordtype"
                                )}' and dataobject = '$idDataObject'"
                            )
                            recordValueRS.next()
                            oldRecordList.add(Pair(recordTypesRS.getString("name"), recordValueRS.getString("value")))
                            formRecordList.add(Pair(recordTypesRS.getString("name"), recordValueRS.getString("value")))
                            editText.text = SpannableStringBuilder(recordValueRS.getString("value"))
                            textInputLayout.addView(editText)
                        }
                    }
                    else -> {
                        val idDataTypeRS = connection.createStatement()
                            .executeQuery("select datatype from branches where name = '${branchSpinner.selectedItem}' and deleted = '0'")
                        idDataTypeRS.next()
                        val idDataType = idDataTypeRS.getString("datatype")
                        val recordTypesRS = connection.createStatement()
                            .executeQuery("select recordtype, r.name, dataorder, dr.deleted from datatypes_recordtypes dr join recordtypes r on r.idrecordtype = dr.recordtype where datatype = '$idDataType' and dr.deleted = '0' order by dataorder")
                        while (recordTypesRS.next()) {
                            val textInputLayout = TextInputLayout(context)
                            textInputLayout.layoutParams =
                                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                            textInputLayout.setPadding(10, 10, 10, 10)
                            dataLL.addView(textInputLayout)
                            val editText = EditText(context)
                            editText.layoutParams =
                                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                            editText.hint = recordTypesRS.getString("name")
                            textInputLayout.addView(editText)
                        }
                    }
                }
                connection.close()
            } catch (e: SQLException) {
                Log.e("MyApp", e.toString())
                e.printStackTrace()
            }
            val button = Button(context, null, 0, R.style.Widget_AppCompat_Button_Colored)
            button.text = "Заполнить"
            button.layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                weight = 1.0f
                gravity = Gravity.CENTER
            }
            button.setPadding(10, 10, 10, 10)
            dataLL.addView(button)
            val changeBuilder = AlertDialog.Builder(context)
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
                    Toast.makeText(context, "Длина значения каждого реквизита должна быть от 1 до 150 символов", Toast.LENGTH_SHORT).show()
                }
                else {
                    formRecordList.clear()
                    for (i in 1 until dataLL.childCount - 1) {
                        val TIL = dataLL.getChildAt(i) as TextInputLayout
                        formRecordList.add(Pair(TIL.hint.toString(), TIL.editText!!.text.toString()))
                    }
                }
                ad.dismiss()
            }
        }
        val changeBuilder = AlertDialog.Builder(context)
        changeBuilder.setView(view)
        val ad = changeBuilder.create()
        ad.show()
        createButton.setOnClickListener {
            if (nameET.text.length !in 1..30) {
                Toast.makeText(context, "Длина названия объекта должна быть от 1 до 30 символов", Toast.LENGTH_SHORT).show()
            }
            else if (formRecordList.isEmpty()) {
                Toast.makeText(context, "Форма объекта должна быть заполнена", Toast.LENGTH_SHORT).show()
            }
            else {
                var encodedImage: ByteArray? = null
                if (this::currentUri.isInitialized && currentUri != Uri.EMPTY) {
                    val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, currentUri)
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos)
                    encodedImage = baos.toByteArray()
                }
                val connection = DatabaseConnector().createConnection()
                val rs = connection.createStatement().executeQuery(
                    "select iduser from users where" +
                            " login = '$currentUser'"
                )
                rs.next()
                val creatorID = rs.getString("iduser")
                if (isEdit) {
                    val dataObjectRS = connection.createStatement().executeQuery("select iddataobject from dataobjects where name = '$chosenDataObjectName'")
                    dataObjectRS.next()
                    val dataObjectID = dataObjectRS.getString("iddataobject")
                    if (encodedImage != null) {
                        val preparedStatement = connection.prepareStatement("update dataobjects set name = '${nameET.text}', image = ?, changedby = '$creatorID', changeddate = SYSTIMESTAMP where name = '$chosenDataObjectName'")
                        preparedStatement.setBinaryStream(1, ByteArrayInputStream(encodedImage))
                        preparedStatement.executeUpdate()
                    }
                    else {
                        connection.createStatement().executeQuery("update dataobjects set name = '${nameET.text}', changedby = '$creatorID', changeddate = SYSTIMESTAMP where name = '$chosenDataObjectName'")
                    }
                    var counter = 0
                    formRecordList.forEach {
                        if (it.second != oldRecordList[0].second) {
                            val recordTypeIDRS = connection.createStatement().executeQuery("select idrecordtype from recordtypes where name = '${it.first}'")
                            recordTypeIDRS.next()
                            val recordTypeID = recordTypeIDRS.getString("idrecordtype")
                            connection.createStatement().executeQuery("update recordvalues set value = '${it.second}', changedby = '$creatorID', changeddate = SYSTIMESTAMP where recordtype = '$recordTypeID' and dataobject = '$dataObjectID'")
                        }
                        counter++
                    }
                    Toast.makeText(context, "Объект изменён", Toast.LENGTH_SHORT).show()
                    ad.dismiss()
                    refreshDataObjects()
                }
                else {
                    val selectedBranchIDRS = connection.createStatement().executeQuery("select idbranch from branches where name = '${branchSpinner.selectedItem}' and deleted = '0'")
                    selectedBranchIDRS.next()
                    val selectedBranchID = selectedBranchIDRS.getString("idbranch")
                    if (encodedImage != null) {
                        val preparedStatement = connection.prepareStatement("insert into dataobjects (name, branch, image, createdby, creationdate) values ('${nameET.text}', '$selectedBranchID', ?, '$creatorID', SYSTIMESTAMP)")
                        preparedStatement.setBinaryStream(1, ByteArrayInputStream(encodedImage))
                        preparedStatement.executeUpdate()
                    }
                    else {
                        connection.createStatement().executeQuery("insert into dataobjects (name, branch, createdby, creationdate) values ('${nameET.text}', '$selectedBranchID', '$creatorID', SYSTIMESTAMP)")
                    }
                    val dataObjectRS = connection.createStatement().executeQuery("select iddataobject from dataobjects where name = '${nameET.text}'")
                    dataObjectRS.next()
                    val dataObjectID = dataObjectRS.getString("iddataobject")
                    formRecordList.forEach {
                        val recordTypeIDRS = connection.createStatement().executeQuery("select idrecordtype from recordtypes where name = '${it.first}'")
                        recordTypeIDRS.next()
                        val recordTypeID = recordTypeIDRS.getString("idrecordtype")
                        connection.createStatement().executeQuery("insert into recordvalues (value, recordtype, dataobject, createdby, creationdate) values ('${it.second}', '$recordTypeID', '$dataObjectID', '$creatorID', SYSTIMESTAMP)")
                    }
                    Toast.makeText(context, "Объект создан", Toast.LENGTH_SHORT).show()
                    ad.dismiss()
                    refreshDataObjects()
                }
                connection.close()
            }
        }
    }

    private fun pickFromGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK)
        galleryIntent.type = "image/*"
        startActivityForResult(context, galleryIntent, IMAGE_PICK_GALLERY_CODE, Bundle())
    }

    private fun pickFromCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Temp pic")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp description")
        currentUri =
            context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )!!
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentUri)
        startActivityForResult(context, cameraIntent, IMAGE_PICK_CAMERA_CODE, Bundle())
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            context,
            storagePermissions,
            STORAGE_REQUEST_CODE
        )
    }

    private fun checkCameraPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val result2 = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        return result && result2
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            context,
            cameraPermissions,
            CAMERA_REQUEST_CODE
        )
    }

    fun updateIV() {
        try {
            Picasso.get().load(currentUri).resize(200, 200).centerCrop().into(imageView)
        } catch (e: Exception) {
            Log.e("MyApp", e.toString())
            e.printStackTrace()
        }
    }

}