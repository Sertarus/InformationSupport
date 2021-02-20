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
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.recyclerview.widget.RecyclerView
import com.application.informationsupport.ObjectInfoActivity
import com.application.informationsupport.R
import com.application.informationsupport.database.DatabaseConnector
import com.application.informationsupport.models.ModelMainUserInfo
import com.application.informationsupport.models.ModelUserInfo

class UserAdapter(val context: Activity, var userList: List<ModelMainUserInfo>, val currentUser: String): RecyclerView.Adapter<UserAdapter.UserHolder>() {

    class UserHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var loginTV: TextView = itemView.findViewById(R.id.loginTV)
        var nameTV: TextView = itemView.findViewById(R.id.nameTV)
        var serviceTV: TextView = itemView.findViewById(R.id.serviceTV)
        var districtTV: TextView = itemView.findViewById(R.id.districtTV)
        var creatorTV: TextView = itemView.findViewById(R.id.creatorTV)
        var creationDateTV: TextView = itemView.findViewById(R.id.createDateTV)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.row_user, parent, false)
        return UserHolder(view)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    override fun onBindViewHolder(holder: UserHolder, position: Int) {
        val login = userList[position].login
        val fullName = userList[position].fullName
        val district = userList[position].district
        val service = userList[position].service
        val creator = userList[position].creator
        val creationDate = userList[position].creationDate

        holder.loginTV.text  = login
        holder.nameTV.text = fullName
        holder.districtTV.text = district
        holder.serviceTV.text = service
        holder.creatorTV.text = creator
        holder.creationDateTV.text = creationDate

        holder.itemView.setOnClickListener {
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
                            intent.putExtra("name", login)
                            intent.putExtra("type", "user")
                            context.startActivity(intent)
                        }

                        1 -> {
                            createOrEditUser(holder.loginTV.text.toString(), true)
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
                                    "update users set changedby = '$creatorID'," +
                                            " changeddate = SYSTIMESTAMP, deleted = '1' where login =" +
                                            " '${holder.loginTV.text}'"
                                )
                                Toast.makeText(context, "Пользователь удалён", Toast.LENGTH_SHORT).show()
                                connection.close()
                                refreshUserInfo()
                            }
                            catch (e: SQLException) {
                                Log.e("MyApp", e.toString())
                                e.printStackTrace()
                            }
                        }
                    }
                }
                builder.create().show()
            }
        }
    }

    fun refreshUserInfo() {
        val dataSet = mutableListOf<ModelMainUserInfo>()
        try {
            val connection = DatabaseConnector().createConnection()
            val rs = connection.createStatement().executeQuery("select * from users where deleted = '0'")
            while (rs.next()) {
                val newStmt = connection.createStatement()
                val nameSet = newStmt.executeQuery(
                    "select login from users where iduser = '" +
                            rs.getString("createdby") + "'"
                )
                nameSet.next()
                val name = nameSet.getString("login")
                val serviceSet = connection.createStatement().executeQuery(
                    "select name from services where idservice = '" +
                            rs.getString("service") + "'"
                )
                serviceSet.next()
                val service = serviceSet.getString("name")
                val districtSet = connection.createStatement().executeQuery(
                    "select name from districts where iddistrict = '" +
                            rs.getString("district") + "'"
                )
                districtSet.next()
                val district = districtSet.getString("name")
                dataSet.add(ModelMainUserInfo(rs.getString("login"),
                    rs.getString("fullname"), district, service,
                    name, rs.getString("creationDate").split(".")[0]))
            }
            connection.close()
        }
        catch (e: SQLException) {
            Log.e("MyApp", e.toString())
            e.printStackTrace()
        }
        this.userList = dataSet
        this.notifyDataSetChanged()
    }

    fun createOrEditUser(chosenUserLogin: String, isEdit: Boolean) {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_create_edit_user, null)
        val changeTV = view.findViewById<TextView>(R.id.addTV)

        val button = view.findViewById<Button>(R.id.createUserButton)

        val loginET = view.findViewById<EditText>(R.id.loginET)
        val passwordET = view.findViewById<EditText>(R.id.passwordET)
        val fullnameET = view.findViewById<EditText>(R.id.fullnameET)
        val emailET = view.findViewById<EditText>(R.id.emailET)
        val phoneNumberET = view.findViewById<EditText>(R.id.phoneET)
        val roleSpinner = view.findViewById<Spinner>(R.id.spinnerRole)
        if (isEdit) {
            changeTV.text = "Изменить пользователя"
            button.text = "Изменить"
        }
        val roleData = arrayOf("Пользователь", "Локальный администратор",
            "Администратор")
        val roleAdapter = ArrayAdapter(context,
            android.R.layout.simple_spinner_item, roleData)
        roleSpinner.adapter = roleAdapter
        val serviceData = mutableListOf<String>()
        val districtData = mutableListOf<String>()
        val deviceData = mutableListOf<String>()
        var currentRole = ""
        var currentService = ""
        var currentDistrict = ""
        var currentDevice = ""
        var blocked = ""
        try {
            val connection = DatabaseConnector().createConnection()
            if (isEdit) {
                val infoStmt = connection.createStatement()
                val infoRS =
                    infoStmt.executeQuery("select * from users where login = '$chosenUserLogin'")
                infoRS.next()
                loginET.text = SpannableStringBuilder(infoRS.getString("login"))
                passwordET.text = SpannableStringBuilder(infoRS.getString("password"))
                fullnameET.text = SpannableStringBuilder(infoRS.getString("fullname"))
                emailET.text = SpannableStringBuilder(infoRS.getString("email"))
                phoneNumberET.text = SpannableStringBuilder(infoRS.getString("phonenumber"))
                blocked = infoRS.getString("blocked")
                when (infoRS.getString("role")) {
                    "0" -> currentRole = "Пользователь"
                    "1" -> currentRole = "Локальный администратор"
                    "2" -> currentRole = "Администратор"
                }
                val serviceNameStmt = connection.createStatement()
                val serviceNameRS = serviceNameStmt.executeQuery(
                    "select name from services where idservice = '${infoRS.getString("service")}'"
                )
                serviceNameRS.next()
                currentService = serviceNameRS.getString("name")
                val districtNameStmt = connection.createStatement()
                val districtNameRS = districtNameStmt.executeQuery(
                    "select name from districts where iddistrict = '${infoRS.getString("district")}'"
                )
                districtNameRS.next()
                currentDistrict = districtNameRS.getString("name")
                val deviceNameStmt = connection.createStatement()
                val deviceNameRS = deviceNameStmt.executeQuery(
                    "select name from devices where iddevice = '${infoRS.getString("device")}'"
                )
                deviceNameRS.next()
                currentDevice = deviceNameRS.getString("name")
                deviceData.add(currentDevice)
            }
            val serviceStmt = connection.createStatement()
            val serviceRS = serviceStmt.executeQuery("select name from services where deleted = '0'")
            while (serviceRS.next()) {
                serviceData.add(serviceRS.getString("name"))
            }
            val districtStmt = connection.createStatement()
            val districtRS = districtStmt.executeQuery("select name from districts where deleted = '0'")
            while (districtRS.next()) {
                districtData.add(districtRS.getString("name"))
            }
            val deviceStmt = connection.createStatement()
            val deviceRS = deviceStmt.executeQuery("select name from devices where iddevice not in (select device from users where deleted = '0')")
            while (deviceRS.next()) {
                deviceData.add(deviceRS.getString("name"))
            }
            connection.close()
        }
        catch (e: SQLException) {
            Log.e("MyApp", e.toString())
            e.printStackTrace()
        }

        val serviceSpinner = view.findViewById<Spinner>(R.id.spinnerService)
        val serviceAdapter = ArrayAdapter(context,
            android.R.layout.simple_spinner_item, serviceData)
        serviceSpinner.adapter = serviceAdapter
        val districtSpinner = view.findViewById<Spinner>(R.id.spinnerDistrict)
        val districtAdapter = ArrayAdapter(context,
            android.R.layout.simple_spinner_item, districtData)
        districtSpinner.adapter = districtAdapter
        val deviceSpinner = view.findViewById<Spinner>(R.id.spinnerDevice)
        val deviceAdapter = ArrayAdapter(context,
            android.R.layout.simple_spinner_item, deviceData)
        deviceSpinner.adapter = deviceAdapter

        val checkboxBlocked = view.findViewById<CheckBox>(R.id.checkBoxBlocked)
        if (isEdit) {
            roleSpinner.setSelection(roleAdapter.getPosition(currentRole))
            serviceSpinner.setSelection(serviceAdapter.getPosition(currentService))
            districtSpinner.setSelection(districtAdapter.getPosition(currentDistrict))
            deviceSpinner.setSelection(deviceAdapter.getPosition(currentDevice))
            if (blocked == "1") {
                checkboxBlocked.isChecked = true
            }
        }
        else {
            roleSpinner.prompt = "Роль пользователя"
            serviceSpinner.prompt = "Служба"
            districtSpinner.prompt = "Район"
            deviceSpinner.prompt = "Устройство"
        }
        val changeBuilder = AlertDialog.Builder(context)
        changeBuilder.setView(view)
        val ad = changeBuilder.create()
        ad.show()
        button.setOnClickListener {
            if (loginET.text.length !in 1..20) {
                Toast.makeText(context, "Недопустимая длина логина", Toast.LENGTH_SHORT).show()
            }
            else if (passwordET.text.length !in 1..20) {
                Toast.makeText(context, "Недопустимая длина пароля", Toast.LENGTH_SHORT).show()
            }
            else if (fullnameET.text.length !in 1..60) {
                Toast.makeText(context, "Недопустимая длина ФИО", Toast.LENGTH_SHORT).show()
            }
            else if (emailET.text.length !in 1..20) {
                Toast.makeText(context, "Недопустимая длина электронной почты", Toast.LENGTH_SHORT).show()
            }
            else if (phoneNumberET.text.length !in 1..16) {
                Toast.makeText(context, "Недопустимая длина номера телефона", Toast.LENGTH_SHORT).show()
            }
            else {
                try {
                    val connection = DatabaseConnector().createConnection()
                    val selectedDevice = deviceSpinner.selectedItem.toString()
                    val usedDeviceRS = connection.createStatement().
                    executeQuery("select device from users where device in" +
                            " (select iddevice from devices where name = '$selectedDevice') and login != '$chosenUserLogin' and deleted = '0'")
                    if (usedDeviceRS.next()) {
                        Toast.makeText(context, "Выбранное устройство уже было привязано к другому сотруднику", Toast.LENGTH_SHORT).show()
                        connection.close()
                    }
                    else {
                        var selectedRole = ""
                        var selectedServiceID = ""
                        var selectedDeviceID = ""
                        var selectedDistrictID = ""
                        when (roleSpinner.selectedItem.toString()) {
                            "Пользователь" -> selectedRole = "0"
                            "Локальный администратор" -> selectedRole = "1"
                            "Администратор" -> selectedRole = "2"
                        }
                        val serviceIDRS = connection.createStatement().executeQuery("select idservice from services where name = '${serviceSpinner.selectedItem}'")
                        serviceIDRS.next()
                        selectedServiceID = serviceIDRS.getString("idservice")
                        val districtIDRS = connection.createStatement().executeQuery("select iddistrict from districts where name = '${districtSpinner.selectedItem}'")
                        districtIDRS.next()
                        selectedDistrictID = districtIDRS.getString("iddistrict")
                        val deviceIDRS = connection.createStatement().executeQuery("select iddevice from devices where name = '${deviceSpinner.selectedItem}'")
                        deviceIDRS.next()
                        selectedDeviceID = deviceIDRS.getString("iddevice")
                        val idStmt = connection.createStatement()
                        val rs = idStmt.executeQuery(
                            "select iduser from users where" +
                                    " login = '$currentUser'"
                        )
                        rs.next()
                        val creatorID = rs.getString("iduser")
                        if (checkboxBlocked.isChecked) {
                            blocked = "1"
                        }
                        else {
                            blocked = "0"
                        }
                        var stmt = ""
                        var successStmt = ""
                        if (isEdit) {
                            stmt = "update users set login = '${loginET.text}'," +
                                    " password = '${passwordET.text}'," +
                                    " fullname = '${fullnameET.text}'," +
                                    " email = '${emailET.text}'," +
                                    " phonenumber = '${phoneNumberET.text}'," +
                                    " role = '${selectedRole}'," +
                                    " service = '$selectedServiceID'," +
                                    " district = '$selectedDistrictID'," +
                                    " device = '$selectedDeviceID'," +
                                    " changedby = '$creatorID'," +
                                    " changeddate = SYSTIMESTAMP, blocked = '$blocked' where login =" +
                                    " '$chosenUserLogin'"
                            successStmt = "Данные пользователя изменены"
                        }
                        else {
                            stmt = "insert into users (login, password, fullname, role, email," +
                                    " phonenumber, service, district, device, createdby," +
                                    " creationdate, blocked) values ('${loginET.text}', '${passwordET.text}'," +
                                    " '${fullnameET.text}', '$selectedRole', '${emailET.text}'," +
                                    " '${phoneNumberET.text}', '$selectedServiceID', '$selectedDistrictID', '$selectedDeviceID', '$creatorID', SYSTIMESTAMP, '0')"
                            successStmt = "Пользователь создан"
                        }
                        connection.createStatement().
                        executeQuery(stmt)
                        Toast.makeText(context, successStmt, Toast.LENGTH_SHORT).show()
                        connection.close()
                        refreshUserInfo()
                        ad.dismiss()
                    }

                }
                catch (e: SQLException) {
                    Log.e("MyApp", e.toString())
                    e.printStackTrace()
                }
            }
        }
    }
}