package com.application.informationsupport.adapters

import android.app.Activity
import android.content.Intent
import android.database.SQLException
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.application.informationsupport.ObjectInfoActivity
import com.application.informationsupport.R
import com.application.informationsupport.database.DatabaseConnector
import com.application.informationsupport.models.ModelSimpleInfo

class CurrentEventAdapter(
    val context: Activity, private var objectList: List<ModelSimpleInfo>,
    private val currentUser: String
) :
    RecyclerView.Adapter<CurrentEventAdapter.CurrentEventHolder>() {

    class CurrentEventHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var nameTV: TextView = itemView.findViewById(R.id.nameTV)
        var creatorTV: TextView = itemView.findViewById(R.id.creatorTV)
        var dateTV: TextView = itemView.findViewById(R.id.dateTV)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrentEventHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.row_current_event, parent, false)
        return CurrentEventHolder(view)
    }

    override fun getItemCount(): Int {
        return objectList.size
    }

    override fun onBindViewHolder(holder: CurrentEventHolder, position: Int) {
        val name = objectList[position].name
        val creator = objectList[position].creator
        val creationDate = objectList[position].creationDate

        holder.nameTV.text = name
        holder.creatorTV.text = creator
        holder.dateTV.text = creationDate

        holder.itemView.setOnClickListener {
            val intent = Intent(context, ObjectInfoActivity::class.java)
            intent.putExtra("name", name)
            intent.putExtra("type", "currentEvent")
            context.startActivity(intent)
        }
    }

    fun refreshCurrentEvents() {
        val dataSet = mutableListOf<ModelSimpleInfo>()
        try {
            val connection = DatabaseConnector().createConnection()
            val serviceIDRS = connection.createStatement()
                .executeQuery("select service from users where login = '$currentUser' and deleted = '0'")
            serviceIDRS.next()
            val serviceID = serviceIDRS.getString("service")
            val districtIDRS = connection.createStatement()
                .executeQuery("select district from users where login = '$currentUser' and deleted = '0'")
            districtIDRS.next()
            val districtID = districtIDRS.getString("district")
            val eventsRS = connection.createStatement()
                .executeQuery("select * from events where CAST(systimestamp AS TIMESTAMP) between timestart and timeend and idevent in (select event from events_services where service = '$serviceID' and deleted = '0') and  idevent in (select event from events_districts where district = '$districtID' and deleted = '0') and deleted = '0'")
            while (eventsRS.next()) {
                val nameSet = connection.createStatement().executeQuery(
                    "select login from users where iduser = '" +
                            eventsRS.getString("createdby") + "'"
                )
                nameSet.next()
                val name = nameSet.getString("login")
                dataSet.add(
                    ModelSimpleInfo(
                        eventsRS.getString("name"),
                        name,
                        eventsRS.getString("creationdate").split(".")[0]
                    )
                )
            }
            connection.close()
        } catch (e: SQLException) {
            Log.e("MyApp", e.toString())
            e.printStackTrace()
        }
        this.objectList = dataSet
        this.notifyDataSetChanged()
    }

}