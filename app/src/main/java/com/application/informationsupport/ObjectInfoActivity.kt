package com.application.informationsupport

import android.database.SQLException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.application.informationsupport.adapters.DataItemAdapter
import com.application.informationsupport.database.DatabaseConnector
import com.application.informationsupport.models.ModelDataItem
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class ObjectInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_object_info)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        title = intent.getStringExtra("name")
        val informationTV = findViewById<TextView>(R.id.informationTV)
        val objectIV = findViewById<ImageView>(R.id.imageView)
        val changeButton = findViewById<Button>(R.id.changeButton)
        objectIV.visibility = View.GONE
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
                    objectIV.setImageBitmap(Bitmap.createScaledBitmap(bitmap, 250, 250, false))
                    objectIV.visibility = View.VISIBLE
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
            changeButton.setOnClickListener {

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