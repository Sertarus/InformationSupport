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

class SimpleItemAdapter(
    val context: Activity,
    var objectList: List<ModelSimpleInfo>,
    val currentUser: String,
    val currentType: String
) :
    RecyclerView.Adapter<SimpleItemAdapter.ServiceItemHolder>() {

    class ServiceItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var nameTV: TextView = itemView.findViewById(R.id.nameTV)
        var creatorTV: TextView = itemView.findViewById(R.id.creatorTV)
        var dateTV: TextView = itemView.findViewById(R.id.dateTV)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceItemHolder {
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
        return ServiceItemHolder(view)
    }

    override fun getItemCount(): Int {
        return objectList.size
    }

    override fun onBindViewHolder(holder: ServiceItemHolder, position: Int) {
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
                            .inflate(R.layout.dialog_create_simple_info, null)
                        val nameET = view.findViewById<EditText>(R.id.nameET)
                        nameET.text = SpannableStringBuilder(holder.nameTV.text)
                        val button = view.findViewById<Button>(R.id.createServiceButton)
                        val changeBuilder = AlertDialog.Builder(context)
                        changeBuilder.setView(view)
                        val ad = changeBuilder.create()
                        ad.show()
                        button.setOnClickListener {
                            if (nameET.text.toString().length in 1..40) {
                                try {
                                    val connection = DatabaseConnector().createConnection()
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
                                    var typeString = ""
                                    when (currentType) {
                                        "service" -> {
                                            typeString = "Служба изменена"
                                        }
                                        "district" -> {
                                            typeString = "Район изменён"
                                        }
                                        "device" -> {
                                            typeString = "Устройство изменено"
                                        }
                                    }
                                    Toast.makeText(context, typeString, Toast.LENGTH_SHORT)
                                        .show()
                                    connection.close()
                                    refreshSimpleInfo()
                                    ad.dismiss()
                                } catch (e: SQLException) {
                                    Log.e("MyApp", e.toString())
                                    e.printStackTrace()
                                }
                            }
                            else {
                                var typeString = ""
                                when (currentType) {
                                    "service" -> {
                                        typeString = "службы"
                                    }
                                    "district" -> {
                                        typeString = "района"
                                    }
                                    "device" -> {
                                        typeString = "устройства"
                                    }
                                }
                                Toast.makeText(context, "Недопустимое имя $typeString",
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    2 -> {
                        try {
                            val connection = DatabaseConnector().createConnection()
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
                        } catch (e: SQLException) {
                            Log.e("MyApp", e.toString())
                            e.printStackTrace()
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
            val connection = DatabaseConnector().createConnection()
            val stmt = connection.createStatement()
            val rs = stmt.executeQuery("select * from ${currentType}s where deleted = '0'");
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
            connection.close();
        } catch (e: SQLException) {
            Log.e("MyApp", e.toString())
            e.printStackTrace()
        }
        this.objectList = dataSet
        this.notifyDataSetChanged()
    }
}