package com.application.informationsupport.adapters

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import com.application.informationsupport.models.ModelEvent
import java.util.*

class EventAdapter(
    val context: Activity,
    private var eventList: List<ModelEvent>,
    private val currentUser: String
) : RecyclerView.Adapter<EventAdapter.EventHolder>() {

    class EventHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var nameTV: TextView = itemView.findViewById(R.id.nameTV)
        var creatorTV: TextView = itemView.findViewById(R.id.creatorTV)
        var startTimeTV: TextView = itemView.findViewById(R.id.startTimeTV)
        var endTimeTV: TextView = itemView.findViewById(R.id.endTimeTV)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.row_event, parent, false)
        return EventHolder(view)
    }

    override fun getItemCount(): Int {
        return eventList.size
    }

    override fun onBindViewHolder(holder: EventHolder, position: Int) {
        val name = eventList[position].name
        val creator = eventList[position].creator
        val startTime = eventList[position].startTime
        val endTime = eventList[position].endTime
        holder.nameTV.text = name
        holder.creatorTV.text = creator
        holder.startTimeTV.text = startTime
        holder.endTimeTV.text = endTime
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
                        intent.putExtra("type", "event")
                        context.startActivity(intent)
                    }
                    1 -> {

                    }
                }
            }
        }
    }

    fun createOrEditEvent(chosenEventName: String, isEdit: Boolean) {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_create_edit_event, null)
        val infoTV = view.findViewById<TextView>(R.id.addTV)
        val nameET = view.findViewById<EditText>(R.id.nameET)
        val startDateET = view.findViewById<EditText>(R.id.startDateET)
        val startTimeET = view.findViewById<EditText>(R.id.startTimeET)
        val endDateET = view.findViewById<EditText>(R.id.endDateET)
        val endTimeET = view.findViewById<EditText>(R.id.endTimeET)
        val descriptionET = view.findViewById<EditText>(R.id.descriptionET)
        val serviceRV = view.findViewById<RecyclerView>(R.id.serviceRecyclerView)
        val districtRV = view.findViewById<RecyclerView>(R.id.districtRecyclerView)
        val addButton = view.findViewById<Button>(R.id.createEventButton)
        try {
            val connection = DatabaseConnector().createConnection()
            val servicesRS = connection.createStatement().executeQuery("select name from services where deleted = '0'")
            val serviceData = mutableListOf<Pair<String, Boolean>>()
            while (servicesRS.next()) {
                serviceData.add(Pair(servicesRS.getString("name"), false))
            }
            serviceRV.adapter = CheckAdapter(context, serviceData, "service")
            val districtsRS = connection.createStatement().executeQuery("select name from districts where deleted = '0'")
            val districtData = mutableListOf<Pair<String, Boolean>>()
            while (districtsRS.next()) {
                districtData.add(Pair(districtsRS.getString("name"), false))
            }
            districtRV.adapter = CheckAdapter(context, districtData, "district")
            if (isEdit) {
                infoTV.text = "Изменить мероприятие"
                addButton.text = "Изменить"
                val eventInfoRS = connection.createStatement()
                    .executeQuery("select * from events where name = '$chosenEventName'")
                eventInfoRS.next()
                nameET.text = SpannableStringBuilder(eventInfoRS.getString("name"))
                val startTimestamp = eventInfoRS.getString("timestart").split(".")[0]
                startDateET.text = SpannableStringBuilder(startTimestamp.split(" ")[0])
                startTimeET.text = SpannableStringBuilder(startTimestamp.split(" ")[1])
                val endTimestamp = eventInfoRS.getString("timeend").split(".")[0]
                endDateET.text = SpannableStringBuilder(endTimestamp.split(" ")[0])
                endTimeET.text = SpannableStringBuilder(endTimestamp.split(" ")[1])
                descriptionET.text = SpannableStringBuilder(eventInfoRS.getString("description"))
                val checkedServicesRS = connection.createStatement().executeQuery("select name from services where idservice in (select service from events_services where event = '${eventInfoRS.getString("idevent")}')")
                while (checkedServicesRS.next()) {
                    for (i in 0 until serviceData.size) {
                        val name = serviceData[i].first
                        if (name == checkedServicesRS.getString("name")) {
                            serviceData[i] = Pair(name, true)
                            break
                        }
                    }
                }
                serviceRV.adapter = CheckAdapter(context, serviceData, "service")
                val checkedDistrictsRS = connection.createStatement().executeQuery("select name from districts where iddistrict in (select district from events_districts where event = '${eventInfoRS.getString("idevent")}')")
                while (checkedDistrictsRS.next()) {
                    for (i in 0 until districtData.size) {
                        val name = districtData[i].first
                        if (name == checkedDistrictsRS.getString("name")) {
                            districtData[i] = Pair(name, true)
                            break
                        }
                    }
                }
                districtRV.adapter = CheckAdapter(context, districtData, "district")
            }
            connection.close()
        } catch (e: SQLException) {
            Log.e("MyApp", e.toString())
            e.printStackTrace()
        }
        startDateET.setOnClickListener {
            val calendar = Calendar.getInstance()
            val cYear = calendar.get(Calendar.YEAR)
            val cMonth = calendar.get(Calendar.MONTH)
            val cDay = calendar.get(Calendar.DAY_OF_MONTH)
            val datePickerDialog = DatePickerDialog(context,
                DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                    startDateET.text = SpannableStringBuilder("${year}-${month}-${dayOfMonth}")
                }, cYear, cMonth, cDay
            )
            datePickerDialog.show()
        }
        startTimeET.setOnClickListener {
            val calendar = Calendar.getInstance()
            val cHour = calendar.get(Calendar.HOUR_OF_DAY)
            val cMinute = calendar.get(Calendar.MINUTE)
            val timePickerDialog = TimePickerDialog(
                context,
                TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                    startTimeET.text = SpannableStringBuilder("${hour}:${minute}:00")
                },
                cHour,
                cMinute,
                true
            )
            timePickerDialog.show()
        }
        endDateET.setOnClickListener {
            val calendar = Calendar.getInstance()
            val cYear = calendar.get(Calendar.YEAR)
            val cMonth = calendar.get(Calendar.MONTH)
            val cDay = calendar.get(Calendar.DAY_OF_MONTH)
            val datePickerDialog = DatePickerDialog(context,
                DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                    endDateET.text = SpannableStringBuilder("${year}-${month}-${dayOfMonth}")
                }, cYear, cMonth, cDay
            )
            datePickerDialog.show()
        }
        endTimeET.setOnClickListener {
            val calendar = Calendar.getInstance()
            val cHour = calendar.get(Calendar.HOUR_OF_DAY)
            val cMinute = calendar.get(Calendar.MINUTE)
            val timePickerDialog = TimePickerDialog(
                context,
                TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                    endTimeET.text = SpannableStringBuilder("${hour}:${minute}:00")
                },
                cHour,
                cMinute,
                true
            )
            timePickerDialog.show()
        }
        val changeBuilder = AlertDialog.Builder(context)
        changeBuilder.setView(view)
        val ad = changeBuilder.create()
        ad.show()
        addButton.setOnClickListener {
            var chosenService = false
            var chosenDistrict = false
            (serviceRV.adapter as CheckAdapter).checkList.forEach {
                if (it.second) {
                    chosenService = true
                    return@forEach
                }
            }
            (districtRV.adapter as CheckAdapter).checkList.forEach {
                if (it.second) {
                    chosenDistrict = true
                    return@forEach
                }
            }
            if (nameET.text.length !in 1..20) {
                Toast.makeText(context, "Длина названия должна быть от 1 до 20 символов", Toast.LENGTH_SHORT).show()
            }
            else if (descriptionET.text.length !in 1..200) {
                Toast.makeText(context, "Длина описания должна быть от 1 до 200 символов", Toast.LENGTH_SHORT).show()
            }
            else if (!chosenService) {
                Toast.makeText(context, "Не выбрано ни одной службы", Toast.LENGTH_SHORT).show()
            }
            else if (!chosenDistrict) {
                Toast.makeText(context, "Не выбрано ни одного района", Toast.LENGTH_SHORT).show()
            }
            else {
                try {
                    val connection = DatabaseConnector().createConnection()
                    val ifNameExistRS = connection.createStatement().executeQuery("select * from events where name = '${nameET.text}'")
                    if (ifNameExistRS.next()) {
                        Toast.makeText(context, "Мероприятие с таким названием уже существует", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        val rs = connection.createStatement().executeQuery(
                            "select iduser from users where" +
                                    " login = '$currentUser'"
                        )
                        rs.next()
                        val creatorID = rs.getString("iduser")
                        if (isEdit) {
                            val eventIDRS = connection.createStatement().executeQuery("select idevent from events where name = '$chosenEventName'")
                            eventIDRS.next()
                            val eventID = eventIDRS.getString("idevent")
                            connection.createStatement().executeQuery("update events set name = '${nameET.text}', description = '${descriptionET.text}', timestart = TO_TIMESTAMP('${startDateET.text} ${startTimeET.text}.000000000', 'YYYY-MM-DD HH24:MI:SS:FF'), timeend = TO_TIMESTAMP('${endDateET.text} ${endTimeET.text}.000000000', 'YYYY-MM-DD HH24:MI:SS:FF'), changedby = '$creatorID', changeddate = SYSTIMESTAMP")
                            connection.createStatement().executeQuery("update events_services set changedby = '$creatorID', changeddate = SYSTIMESTAMP, deleted = '1'")
                            connection.createStatement().executeQuery("update events_districts set changedby = '$creatorID', changeddate = SYSTIMESTAMP, deleted = '1'")
                        }
                        else {
                            connection.createStatement().executeQuery("insert into events (name, description, timestart, timeend, createdby, creationdate) values (${nameET.text}, ${descriptionET.text}, TO_TIMESTAMP('${startDateET.text} ${startTimeET.text}.000000000', 'YYYY-MM-DD HH24:MI:SS:FF'), TO_TIMESTAMP('${endDateET.text} ${endTimeET.text}.000000000', 'YYYY-MM-DD HH24:MI:SS:FF'), '$creatorID', SYSTIMESTAMP)")
                        }
                        val eventIDRS = connection.createStatement().executeQuery("select idevent from events where name = '${nameET.text}'")
                        eventIDRS.next()
                        val eventID = eventIDRS.getString("idevent")
                        (serviceRV.adapter as CheckAdapter).checkList.forEach {
                            if (it.second) {
                                val serviceIDRS = connection.createStatement()
                                    .executeQuery("select idservice from services where name = '${it.first}'")
                                serviceIDRS.next()
                                val serviceID = serviceIDRS.getString("idservice")
                                connection.createStatement().executeQuery("insert into events_services (event, service, createdby, creationdate) values ('$eventID', '$serviceID', '$creatorID', SYSTIMESTAMP)")
                            }
                        }
                        (districtRV.adapter as CheckAdapter).checkList.forEach {
                            if (it.second) {
                                val districtIDRS = connection.createStatement()
                                    .executeQuery("select iddistrict from districts where name = '${it.first}'")
                                districtIDRS.next()
                                val districtID = districtIDRS.getString("iddistrict")
                                connection.createStatement().executeQuery("insert into events_districts (event, service, createdby, creationdate) values ('$eventID', '$districtID', '$creatorID', SYSTIMESTAMP)")
                            }
                        }
                        if (isEdit) {
                            Toast.makeText(context, "Мероприятие изменено", Toast.LENGTH_SHORT).show()
                        }
                        else {
                            Toast.makeText(context, "Мероприятие создано", Toast.LENGTH_SHORT).show()
                        }
                    }
                    connection.close()
                }
                catch (e: SQLException) {
                    Log.e("MyApp", e.toString())
                    e.printStackTrace()
                }
            }
        }
        ad.dismiss()
    }
}