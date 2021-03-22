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
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.application.informationsupport.ObjectInfoActivity
import com.application.informationsupport.R
import com.application.informationsupport.database.DatabaseConnector
import com.application.informationsupport.models.ModelBranchInfo
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class BranchAdapter(
    val context: Activity,
    private var branchList: List<ModelBranchInfo>,
    private val currentUser: String,
    private val url: String?,
    private val username: String?,
    private val pass: String?
) : RecyclerView.Adapter<BranchAdapter.BranchHolder>() {

    class BranchHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var nameTV: TextView = itemView.findViewById(R.id.nameTV)
        var higherBranchTV: TextView = itemView.findViewById(R.id.higherBranchTV)
        var dataTypeTV: TextView = itemView.findViewById(R.id.dataTypeTV)
        var creatorTV: TextView = itemView.findViewById(R.id.creatorTV)
        var creationDateTV: TextView = itemView.findViewById(R.id.dateTV)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BranchHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.row_branch, parent, false)
        return BranchHolder(view)
    }

    override fun getItemCount(): Int {
        return branchList.size
    }

    override fun onBindViewHolder(holder: BranchHolder, position: Int) {
        val name = branchList[position].name
        val higherBranch = branchList[position].higherBranch
        val dataType = branchList[position].dataType
        val creationDate = branchList[position].creationDate
        val creator = branchList[position].creator

        holder.nameTV.text = name
        holder.higherBranchTV.text = higherBranch
        holder.dataTypeTV.text = dataType
        holder.creationDateTV.text = creationDate
        holder.creatorTV.text = creator

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
                        intent.putExtra("type", "branch")
                        context.startActivity(intent)
                    }

                    1 -> {
                        createOrEditBranch(holder.nameTV.text.toString(), true)
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
                            val branchIDRS = connection.createStatement()
                                .executeQuery("select idbranch from branches where name = '${holder.nameTV.text}'")
                            branchIDRS.next()
                            val branchID = branchIDRS.getString("idbranch")
                            val stmt = connection.createStatement()
                            stmt.executeQuery(
                                "update branches set changedby = '$creatorID'," +
                                        " changeddate = SYSTIMESTAMP, deleted = '1' where name =" +
                                        " '${holder.nameTV.text}'"
                            )
                            connection.createStatement()
                                .executeQuery("update branches_services set changedby = '$creatorID', changeddate = SYSTIMESTAMP, deleted = '1' where branch = '$branchID'")
                            connection.createStatement()
                                .executeQuery("update branches_districts set changedby = '$creatorID', changeddate = SYSTIMESTAMP, deleted = '1' where branch = '$branchID'")
                            Toast.makeText(context, "Ветка удалена", Toast.LENGTH_SHORT).show()
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
                        refreshBranches()
                    }
                }
            }
            builder.create().show()
            refreshBranches()
        }
    }

    fun refreshBranches() {
        val dataSet = mutableListOf<ModelBranchInfo>()
        try {
            val connection = DatabaseConnector(url, username, pass).createConnection()
            val rs = connection.createStatement()
                .executeQuery("select * from branches where deleted = '0'")
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
                    val higherBranchSet = connection.createStatement()
                        .executeQuery("select name from branches where idbranch = '${rs.getString("higherbranch")}'")
                    if (higherBranchSet.next()) {
                        higherBranchName = higherBranchSet.getString("name")
                    }
                }
                val datatypeSet = connection.createStatement()
                    .executeQuery("select name from datatypes where iddatatype = '${rs.getString("datatype")}'")
                var datatypeName = ""
                while (datatypeSet.next()) {
                    datatypeName = datatypeSet.getString("name")
                }
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
        this.branchList = dataSet
        this.notifyDataSetChanged()
    }

    fun createOrEditBranch(chosenBranchName: String, isEdit: Boolean) {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_create_edit_branch, null)
        val changeTV = view.findViewById<TextView>(R.id.addTV)
        val button = view.findViewById<Button>(R.id.createBranchButton)
        val nameET = view.findViewById<EditText>(R.id.nameET)
        val datatypeSpinner = view.findViewById<Spinner>(R.id.spinnerDataType)
        val branchSpinner = view.findViewById<Spinner>(R.id.spinnerHigherBranch)
        val serviceRecyclerView = view.findViewById<RecyclerView>(R.id.serviceRecyclerView)
        val servicesTV = view.findViewById<TextView>(R.id.servicesTV)
        val districtsTV = view.findViewById<TextView>(R.id.districtsTV)
        serviceRecyclerView.setHasFixedSize(true)
        serviceRecyclerView.layoutManager = LinearLayoutManager(context)
        val districtRecyclerView = view.findViewById<RecyclerView>(R.id.districtRecyclerView)
        districtRecyclerView.setHasFixedSize(true)
        districtRecyclerView.layoutManager = LinearLayoutManager(context)
        if (isEdit) {
            changeTV.text = "Изменить ветку"
            button.text = "Изменить"
            branchSpinner.visibility = View.GONE
            serviceRecyclerView.visibility = View.GONE
            districtRecyclerView.visibility = View.GONE
            servicesTV.visibility = View.GONE
            districtsTV.visibility = View.GONE
        }
        val datatypeData = mutableListOf<String>()
        val branchData = mutableListOf<String>()
        branchData.add("-")
        var currentDatatype = ""
        try {
            val connection = DatabaseConnector(url, username, pass).createConnection()
            val infoRS = connection.createStatement()
                .executeQuery("select * from branches where name = '$chosenBranchName'")
            infoRS.next()
            if (isEdit) {
                val currentDatatypeRS = connection.createStatement().executeQuery(
                    "select name from datatypes where iddatatype = '${infoRS.getString("datatype")}'"
                )
                currentDatatypeRS.next()
                currentDatatype = currentDatatypeRS.getString("name")
                nameET.text = SpannableStringBuilder(chosenBranchName)
            }
            val datatypeRS = connection.createStatement()
                .executeQuery("select name from datatypes where deleted = '0'")
            while (datatypeRS.next()) {
                datatypeData.add(datatypeRS.getString("name"))
            }
            val branchRS = connection.createStatement()
                .executeQuery("select name from branches where deleted = '0'")
            while (branchRS.next()) {
                branchData.add(branchRS.getString("name"))
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
        val datatypeAdapter =
            ArrayAdapter(context, android.R.layout.simple_spinner_item, datatypeData)
        datatypeSpinner.adapter = datatypeAdapter
        val branchAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, branchData)
        branchSpinner.adapter = branchAdapter
        if (isEdit) {
            datatypeSpinner.setSelection(datatypeAdapter.getPosition(currentDatatype))
        } else {
            branchSpinner.setSelection(branchAdapter.getPosition("-"))
        }
        var serviceAdapter = CheckAdapter(context, mutableListOf(), "service")
        var districtAdapter = CheckAdapter(context, mutableListOf(), "district")
        branchSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                try {
                    val newServiceData = mutableListOf<Pair<String, Boolean>>()
                    val newDistrictData = mutableListOf<Pair<String, Boolean>>()
                    val connection = DatabaseConnector(url, username, pass).createConnection()
                    var serviceStmt = "select name from services where deleted = '0'"
                    var districtStmt = "select name from districts where deleted = '0'"
                    if (branchAdapter.getItem(position) != "-") {
                        serviceStmt += " and idservice in (select service from branches_services where branch in (select idbranch from branches where name = '${branchAdapter.getItem(
                            position
                        )}'))"
                        districtStmt += " and iddistrict in (select district from branches_districts where branch in (select idbranch from branches where name = '${branchAdapter.getItem(
                            position
                        )}'))"
                    }
                    val serviceRS = connection.createStatement().executeQuery(serviceStmt)
                    while (serviceRS.next()) {
                        newServiceData.add(Pair(serviceRS.getString("name"), false))
                    }
                    serviceAdapter = CheckAdapter(context, newServiceData, "service")
                    serviceRecyclerView.adapter = serviceAdapter
                    val districtRS = connection.createStatement().executeQuery(districtStmt)
                    while (districtRS.next()) {
                        newDistrictData.add(Pair(districtRS.getString("name"), false))
                    }
                    districtAdapter = CheckAdapter(context, newDistrictData, "district")
                    districtRecyclerView.adapter = districtAdapter
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
        val changeBuilder = AlertDialog.Builder(context)
        changeBuilder.setView(view)
        val ad = changeBuilder.create()
        ad.show()
        button.setOnClickListener {
            var chosenService = false
            var chosenDistrict = false
            serviceAdapter.checkList.forEach {
                if (it.second) {
                    chosenService = true
                    return@forEach
                }
            }
            districtAdapter.checkList.forEach {
                if (it.second) {
                    chosenDistrict = true
                    return@forEach
                }
            }
            if (nameET.text.length !in 1..30 && nameET.text.toString() != "-") {
                Toast.makeText(
                    context,
                    "Длина названия ветки должна быть от 1 до 30 символов",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (!chosenService && !isEdit) {
                Toast.makeText(context, "Не выбрано ни одной службы", Toast.LENGTH_SHORT).show()
            } else if (!chosenDistrict && !isEdit) {
                Toast.makeText(context, "Не выбрано ни одного района", Toast.LENGTH_SHORT).show()
            } else {
                try {
                    val connection = DatabaseConnector(url, username, pass).createConnection()
                    val ifNameExist = connection.createStatement()
                        .executeQuery("select * from branches where name = '${nameET.text}' and deleted = '0'")
                    if (ifNameExist.next()) {
                        Toast.makeText(
                            context,
                            "Ветка с таким именем уже существует",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val selectedDatatypeIDRS = connection.createStatement()
                            .executeQuery("select iddatatype from datatypes where name = '${datatypeSpinner.selectedItem}'")
                        selectedDatatypeIDRS.next()
                        val datatypeID = selectedDatatypeIDRS.getString("iddatatype")
                        val selectedBranchIDRS = connection.createStatement()
                            .executeQuery("select idbranch from branches where name = '${branchSpinner.selectedItem}'")
                        var selectedBranchID = ""
                        if (selectedBranchIDRS.next()) {
                            selectedBranchID = selectedBranchIDRS.getString("idbranch")
                        }
                        val rs = connection.createStatement().executeQuery(
                            "select iduser from users where" +
                                    " login = '$currentUser'"
                        )
                        rs.next()
                        val creatorID = rs.getString("iduser")
                        if (isEdit) {
                            connection.createStatement()
                                .executeQuery("update branches set name = '${nameET.text}', datatype = '$datatypeID', changedby = '$creatorID', changeddate = SYSTIMESTAMP where name = '$chosenBranchName'")
                            Toast.makeText(context, "Ветка изменена", Toast.LENGTH_SHORT).show()
                            ad.dismiss()
                        } else {
                            var branchString = ""
                            var selectedBranchString = ""
                            if (selectedBranchID != "") {
                                branchString = "higherbranch, "
                                selectedBranchString = "'$selectedBranchID', "
                            }
                            connection.createStatement()
                                .executeQuery("insert into branches (name, datatype, ${branchString}createdby, creationdate) values ('${nameET.text}', '$datatypeID', $selectedBranchString'$creatorID', SYSTIMESTAMP)")
                            val branchIDRS = connection.createStatement()
                                .executeQuery("select idbranch from branches where name = '${nameET.text}'")
                            branchIDRS.next()
                            val branchID = branchIDRS.getString("idbranch")
                            serviceAdapter.checkList.forEach {
                                if (it.second) {
                                    val serviceIDRS = connection.createStatement()
                                        .executeQuery("select idservice from services where name = '${it.first}'")
                                    serviceIDRS.next()
                                    val serviceID = serviceIDRS.getString("idservice")
                                    connection.createStatement()
                                        .executeQuery("insert into branches_services (branch, service, createdby, creationdate) values ('$branchID', '$serviceID', '$creatorID', SYSTIMESTAMP)")
                                }
                            }
                            districtAdapter.checkList.forEach {
                                if (it.second) {
                                    val districtIDRS = connection.createStatement()
                                        .executeQuery("select iddistrict from districts where name = '${it.first}'")
                                    districtIDRS.next()
                                    val districtID = districtIDRS.getString("iddistrict")
                                    connection.createStatement()
                                        .executeQuery("insert into branches_districts (branch, district, createdby, creationdate) values ('$branchID', '$districtID', '$creatorID', SYSTIMESTAMP)")
                                }
                            }
                            Toast.makeText(context, "Ветка создана", Toast.LENGTH_SHORT).show()
                            ad.dismiss()
                        }
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
            }
            refreshBranches()
        }
    }
}