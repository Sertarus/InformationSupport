package com.application.informationsupport.adapters

import android.app.Activity
import android.content.Intent
import android.database.SQLException
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.application.informationsupport.ObjectInfoActivity
import com.application.informationsupport.R
import com.application.informationsupport.database.DatabaseConnector
import com.application.informationsupport.models.ModelSimpleInfo
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class SimpleItemAdapter(
    val context: Activity,
    private var objectList: List<ModelSimpleInfo>,
    private val currentUser: String,
    private val currentType: String,
    private val url: String?,
    private val username: String?,
    private val pass: String?
) :
    RecyclerView.Adapter<SimpleItemAdapter.SimpleItemHolder>() {

    class SimpleItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var nameTV: TextView = itemView.findViewById(R.id.nameTV)
        var creatorTV: TextView = itemView.findViewById(R.id.creatorTV)
        var dateTV: TextView = itemView.findViewById(R.id.dateTV)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleItemHolder {
        var view = View(context)
        when (currentType) {
            "service" -> {
                view = LayoutInflater.from(context).inflate(R.layout.row_service, parent, false)
            }
            "district" -> {
                view = LayoutInflater.from(context).inflate(R.layout.row_district, parent, false)
            }
            "device" -> {
                view = LayoutInflater.from(context).inflate(R.layout.row_device, parent, false)
            }
        }
        return SimpleItemHolder(view)
    }

    override fun getItemCount(): Int {
        return objectList.size
    }

    override fun onBindViewHolder(holder: SimpleItemHolder, position: Int) {
        val name = objectList[position].name
        val creator = objectList[position].creator
        val creationDate = objectList[position].creationDate

        holder.nameTV.text = name
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
                        intent.putExtra("type", currentType)
                        context.startActivity(intent)
                    }

                    1 -> {
                        val view = LayoutInflater.from(context)
                            .inflate(R.layout.dialog_create_edit_simple_info, null)
                        val changeTV = view.findViewById<TextView>(R.id.addTV)
                        var typeString = ""
                        when (currentType) {
                            "service" -> typeString = "службу"
                            "district" -> typeString = "район"
                            "device" -> typeString = "устройство"
                        }
                        typeString = "Изменить $typeString"
                        changeTV.text = typeString
                        val nameET = view.findViewById<EditText>(R.id.nameET)
                        nameET.text = SpannableStringBuilder(holder.nameTV.text)
                        val button = view.findViewById<Button>(R.id.createSimpleInfoButton)
                        button.text = "Изменить"
                        val changeBuilder = AlertDialog.Builder(context)
                        changeBuilder.setView(view)
                        val ad = changeBuilder.create()
                        ad.show()
                        button.setOnClickListener {
                            if (nameET.text.toString().length in 1..40) {
                                try {
                                    val connection = DatabaseConnector(url, username, pass).createConnection()
                                    val ifNameExist = connection.createStatement()
                                        .executeQuery("select * from ${currentType}s where name = '${nameET.text}' and deleted = '0'")
                                    if (ifNameExist.next()) {
                                        Toast.makeText(
                                            context,
                                            "Объект с таким именем уже существует",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        val idStmt = connection.createStatement()
                                        val rs = idStmt.executeQuery(
                                            "select iduser from users where" +
                                                    " login = '$currentUser'"
                                        )
                                        rs.next()
                                        val creatorID = rs.getString("iduser")
                                        val stmt = connection.createStatement()
                                        stmt.executeQuery(
                                            "update ${currentType}s set name = '${nameET.text}'," +
                                                    " changedby = '$creatorID'," +
                                                    " changeddate = SYSTIMESTAMP where name = '${holder.nameTV.text}'"
                                        )
                                        var changeTypeString = ""
                                        when (currentType) {
                                            "service" -> {
                                                changeTypeString = "Служба изменена"
                                            }
                                            "district" -> {
                                                changeTypeString = "Район изменён"
                                            }
                                            "device" -> {
                                                changeTypeString = "Устройство изменено"
                                            }
                                        }
                                        Toast.makeText(
                                            context,
                                            changeTypeString,
                                            Toast.LENGTH_SHORT
                                        )
                                            .show()
                                        refreshSimpleInfo()
                                        ad.dismiss()
                                    }
                                    connection.close()

                                } catch (e: Exception) {
                                    val file = File(context.filesDir, "log_error")
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
                                        }
                                        else {
                                            val writer = FileWriter(logfile)
                                            writer.append(date.toString())
                                            writer.append("\n")
                                            writer.append(e.toString())
                                            writer.flush()
                                            writer.close()
                                        }
                                    }
                                    catch (e: Exception) {

                                    }
                                }
                            } else {
                                var genitiveTypeString = ""
                                when (currentType) {
                                    "service" -> {
                                        genitiveTypeString = "службы"
                                    }
                                    "district" -> {
                                        genitiveTypeString = "района"
                                    }
                                    "device" -> {
                                        genitiveTypeString = "устройства"
                                    }
                                }
                                Toast.makeText(
                                    context, "Недопустимое имя $genitiveTypeString",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    2 -> {
                        try {
                            val connection = DatabaseConnector(url, username, pass).createConnection()
                            val idStmt = connection.createStatement()
                            val rs = idStmt.executeQuery(
                                "select iduser from users where" +
                                        " login = '$currentUser'"
                            )
                            rs.next()
                            val creatorID = rs.getString("iduser")
                            val stmt = connection.createStatement()
                            stmt.executeQuery(
                                "update ${currentType}s set changedby = '$creatorID'," +
                                        " changeddate = SYSTIMESTAMP, deleted = '1' where name =" +
                                        " '${holder.nameTV.text}'"
                            )
                            var typeString = ""
                            when (currentType) {
                                "service" -> {
                                    typeString = "Служба удалена"
                                }
                                "district" -> {
                                    typeString = "Район удалён"
                                }
                                "device" -> {
                                    typeString = "Устройство удалено"
                                }
                            }
                            Toast.makeText(context, typeString, Toast.LENGTH_SHORT)
                                .show()
                            connection.close()
                            refreshSimpleInfo()
                        } catch (e: Exception) {
                            val file = File(context.filesDir, "log_error")
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
                                }
                                else {
                                    val writer = FileWriter(logfile)
                                    writer.append(date.toString())
                                    writer.append("\n")
                                    writer.append(e.toString())
                                    writer.flush()
                                    writer.close()
                                }
                            }
                            catch (e: Exception) {

                            }
                        }
                    }
                }
            }
            builder.create().show()
        }
    }

    fun refreshSimpleInfo() {
        val dataSet = mutableListOf<ModelSimpleInfo>()
        try {
            val connection = DatabaseConnector(url, username, pass).createConnection()
            val rs = connection.createStatement()
                .executeQuery("select * from ${currentType}s where deleted = '0'")
            while (rs.next()) {
                val nameSet = connection.createStatement().executeQuery(
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
        } catch (e: Exception) {
            val file = File(context.filesDir, "log_error")
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
                }
                else {
                    val writer = FileWriter(logfile)
                    writer.append(date.toString())
                    writer.append("\n")
                    writer.append(e.toString())
                    writer.flush()
                    writer.close()
                }
            }
            catch (e: Exception) {

            }
        }
        this.objectList = dataSet
        this.notifyDataSetChanged()
    }
}