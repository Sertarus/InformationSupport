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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.application.informationsupport.ObjectInfoActivity
import com.application.informationsupport.R
import com.application.informationsupport.database.DatabaseConnector
import com.application.informationsupport.models.ModelSimpleInfo

class DataTypeAdapter(
    val context: Activity,
    private var dataTypeList: List<ModelSimpleInfo>,
    private val currentUser: String
) : RecyclerView.Adapter<DataTypeAdapter.DataTypeHolder>() {

    class DataTypeHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var nameTV: TextView = itemView.findViewById(R.id.nameTV)
        var creatorTV: TextView = itemView.findViewById(R.id.creatorTV)
        var dateTV: TextView = itemView.findViewById(R.id.dateTV)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataTypeHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.row_datatype, parent, false)
        return DataTypeHolder(view)
    }

    override fun getItemCount(): Int {
        return dataTypeList.size
    }

    override fun onBindViewHolder(holder: DataTypeHolder, position: Int) {
        val name = dataTypeList[position].name
        val creator = dataTypeList[position].creator
        val creationDate = dataTypeList[position].creationDate

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
                        intent.putExtra("type", "datatype")
                        context.startActivity(intent)
                    }

                    1 -> {
                        createOrEditDatatype(holder.nameTV.text.toString(), true)
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
                            val dataTypeRS = connection.createStatement().executeQuery(
                                "select iddatatype from datatypes where" +
                                        " name = '${holder.nameTV.text}' and deleted = '0'"
                            )
                            dataTypeRS.next()
                            val datatypeID = dataTypeRS.getString("iddatatype")
                            val recordToDelete = connection.createStatement()
                                .executeQuery(
                                    "select recordtype from datatypes_recordtypes" +
                                            " where datatype in (select iddatatype from datatypes where" +
                                            " name = '${holder.nameTV.text}') minus (select recordtype" +
                                            " from datatypes_recordtypes where recordtype in (select" +
                                            " recordtype from datatypes_recordtypes where datatype in" +
                                            " (select iddatatype from datatypes where name =" +
                                            " '${holder.nameTV.text}')) and datatype not in (select" +
                                            " iddatatype from datatypes where name = '${holder.nameTV.text}'))"
                                )
                            while (recordToDelete.next()) {
                                connection.createStatement().executeQuery(
                                    "update recordtypes set changedby = '$creatorID'," +
                                            " changeddate = SYSTIMESTAMP, deleted = 1 where" +
                                            " idrecordtype = '${recordToDelete.getString(
                                                "recordtype"
                                            )}'"
                                )
                            }
                            connection.createStatement()
                                .executeQuery(
                                    "update datatypes_recordtypes " +
                                            "set changedby = '$creatorID', changeddate = SYSTIMESTAMP," +
                                            " deleted = 1 where datatype ='$datatypeID'"
                                )
                            connection.createStatement()
                                .executeQuery(
                                    "update datatypes set changedby = '$creatorID'," +
                                            " changeddate = SYSTIMESTAMP," +
                                            " deleted = 1 where iddatatype = '$datatypeID'"
                                )
                            Toast.makeText(context, "Форма удалена", Toast.LENGTH_SHORT).show()
                            connection.close()
                        } catch (e: SQLException) {
                            Log.e("MyApp", e.toString())
                            e.printStackTrace()
                        }
                        refreshDataTypes()
                    }
                }
            }
            builder.create().show()
            refreshDataTypes()
        }
    }

    fun refreshDataTypes() {
        val dataSet = mutableListOf<ModelSimpleInfo>()
        try {
            val connection = DatabaseConnector().createConnection()
            val rs = connection.createStatement()
                .executeQuery("select * from datatypes where deleted = '0'")
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
                        rs.getString("creationdate").split(".")[0]
                    )
                )
            }
            connection.close()
        } catch (e: SQLException) {
            Log.e("MyApp", e.toString())
            e.printStackTrace()
        }
        this.dataTypeList = dataSet
        this.notifyDataSetChanged()
    }

    fun createOrEditDatatype(chosenDataTypeName: String, isEdit: Boolean) {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_create_edit_datatype, null)
        val changeTV = view.findViewById<TextView>(R.id.addTV)
        val button = view.findViewById<Button>(R.id.createDatatypeButton)
        val nameET = view.findViewById<EditText>(R.id.nameET)
        val ifHumanTV = view.findViewById<TextView>(R.id.ifHumanWarning)
        ifHumanTV.visibility = View.GONE
        val ifHumanCheckbox = view.findViewById<CheckBox>(R.id.checkBoxIsHuman)
        ifHumanCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ifHumanTV.visibility = View.VISIBLE
            } else {
                ifHumanTV.visibility = View.GONE
            }
        }
        val recyclerView = view.findViewById<RecyclerView>(R.id.recordRecyclerView)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        val adapter = AddingItemListAdapter(context, mutableListOf())
        recyclerView.adapter = adapter
        val addRecordButton = view.findViewById<Button>(R.id.addRecordTypeButton)
        addRecordButton.setOnClickListener {
            adapter.addNewRecord()
        }
        if (isEdit) {
            changeTV.text = "Изменить форму"
            button.text = "Изменить"
            nameET.text = SpannableStringBuilder(chosenDataTypeName)
            val ishumanTV = view.findViewById<TextView>(R.id.isHumanTV)
            ishumanTV.visibility = View.GONE
            ifHumanCheckbox.visibility = View.GONE
            recyclerView.visibility = View.GONE
            addRecordButton.visibility = View.GONE
        }
        val changeBuilder = AlertDialog.Builder(context)
        changeBuilder.setView(view)
        val ad = changeBuilder.create()
        ad.show()
        val addButton = view.findViewById<Button>(R.id.createDatatypeButton)
        addButton.setOnClickListener {
            if (nameET.text.length !in 1..40) {
                Toast.makeText(
                    context,
                    "Длина названия формы должна быть от 1 до 40 символов",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (!isEdit && !ifHumanCheckbox.isChecked && adapter.addedItemList.isEmpty()) {
                Toast.makeText(context, "Список реквизитов пуст", Toast.LENGTH_SHORT).show()
            } else {
                try {
                    val connection = DatabaseConnector().createConnection()
                    val ifNameExistRS = connection.createStatement()
                        .executeQuery("select * from datatypes where name = '${nameET.text}' and deleted = '0'")
                    if (ifNameExistRS.next()) {
                        Toast.makeText(
                            context,
                            "Форма данных с таким именем уже существует",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val rs = connection.createStatement().executeQuery(
                            "select iduser from users where" +
                                    " login = '$currentUser'"
                        )
                        rs.next()
                        val creatorID = rs.getString("iduser")
                        if (isEdit) {
                            connection.createStatement()
                                .executeQuery(
                                    "update datatypes set name = '${nameET.text}'," +
                                            " changedby = '$creatorID', changeddate = SYSTIMESTAMP" +
                                            " where name = '${chosenDataTypeName}'"
                                )
                            Toast.makeText(
                                context,
                                "Название формы было изменено",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            refreshDataTypes()
                        } else {
                            val ifHuman: String = if (ifHumanCheckbox.isChecked) "1"
                            else "0"
                            connection.createStatement()
                                .executeQuery(
                                    "insert into datatypes (name, ishuman, createdby," +
                                            " creationdate) values ('${nameET.text}', '$ifHuman'," +
                                            " '$creatorID', SYSTIMESTAMP)"
                                )
                            if (ifHumanCheckbox.isChecked) {
                                adapter.addedItemList.add(0, Pair("ФИО", 1))
                                adapter.addedItemList.add(1, Pair("Дата рождения", 2))
                                for (i in 2 until adapter.addedItemList.size) {
                                    adapter.addedItemList[i] = Pair(
                                        adapter.addedItemList[i].first,
                                        adapter.addedItemList[i].second + 3
                                    )
                                }
                            }
                            adapter.addedItemList.forEach {
                                val recordExistCheckRS = connection.createStatement()
                                    .executeQuery(
                                        "select name from recordtypes where " +
                                                "name = '${it.first}' and deleted = '0'"
                                    )
                                if (!recordExistCheckRS.next()) {
                                    connection.createStatement()
                                        .executeQuery(
                                            "insert into recordtypes (name, createdby," +
                                                    " creationdate) values ('${it.first}', '$creatorID', SYSTIMESTAMP)"
                                        )
                                }
                                val datatypeIDRS = connection.createStatement()
                                    .executeQuery("select iddatatype from datatypes where name = '${nameET.text}'")
                                datatypeIDRS.next()
                                val datatypeID = datatypeIDRS.getString("iddatatype")
                                val recordtypeIDRS = connection.createStatement()
                                    .executeQuery("select idrecordtype from recordtypes where name = '${it.first}' and deleted = '0'")
                                recordtypeIDRS.next()
                                val recordTypeID = recordtypeIDRS.getString("idrecordtype")
                                connection.createStatement()
                                    .executeQuery(
                                        "insert into datatypes_recordtypes (datatype," +
                                                " recordtype, dataorder, createdby, creationdate) values ('$datatypeID'," +
                                                " '$recordTypeID', '${it.second}', '$creatorID', SYSTIMESTAMP)"
                                    )
                            }
                            Toast.makeText(context, "Форма добавлена", Toast.LENGTH_SHORT).show()
                            refreshDataTypes()
                            ad.dismiss()
                        }
                    }
                    connection.close()
                } catch (e: SQLException) {
                    Log.e("MyApp", e.toString())
                    e.printStackTrace()
                }
            }
        }
    }
}