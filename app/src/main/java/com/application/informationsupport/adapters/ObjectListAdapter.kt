package com.application.informationsupport.adapters

import android.app.Activity
import android.content.Intent
import android.database.SQLException
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.application.informationsupport.ObjectInfoActivity
import com.application.informationsupport.R
import com.application.informationsupport.database.DatabaseConnector
import com.application.informationsupport.models.ModelObjectList
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class ObjectListAdapter(
    val context: Activity,
    var objectList: MutableList<ModelObjectList>,
    val currentUser: String,
    val isSearch: Boolean,
    private val url: String?,
    private val username: String?,
    private val pass: String?
) :
    RecyclerView.Adapter<ObjectListAdapter.ObjectListHolder>() {

    var currentPath = mutableListOf<String>()

    class ObjectListHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var typeIV: ImageView = itemView.findViewById(R.id.imageView)
        var nameTV: TextView = itemView.findViewById(R.id.nameTV)
        var creatorTV: TextView = itemView.findViewById(R.id.creatorTV)
        var dateTV: TextView = itemView.findViewById(R.id.dateTV)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ObjectListHolder {
        val view =
            LayoutInflater.from(context).inflate(R.layout.row_data_object_short, parent, false)
        return ObjectListHolder(view)
    }

    override fun getItemCount(): Int {
        return if (context.title.toString() != "Информационное обеспечение" && (objectList.isEmpty() || objectList[0].name != "") && !isSearch) objectList.size +
                1 else objectList.size
    }

    override fun onBindViewHolder(holder: ObjectListHolder, position: Int) {
        val title = context.title.toString()
        val notRootScreen = title != "Информационное обеспечение"
        if (notRootScreen && (objectList.isEmpty() || objectList[0].name != "") && !isSearch) {
            objectList.add(0, ModelObjectList("", "", "", false))
        }
        val name = objectList[position].name
        val creator = objectList[position].creator
        val date = objectList[position].date
        val isFolder = objectList[position].isFolder

        holder.nameTV.text = name
        holder.creatorTV.text = creator
        holder.dateTV.text = date
        if (isFolder) holder.typeIV.setImageResource(R.drawable.ic_folder)
        else if ((position != 0 || !notRootScreen) || isSearch) holder.typeIV.setImageResource(R.drawable.ic_object)
        else holder.typeIV.setImageResource(R.drawable.ic_arrow_back)

        holder.itemView.setOnClickListener {
            if (position == 0 && notRootScreen && !isSearch) {
                currentPath.removeAt(currentPath.size - 1)
                if (currentPath.isEmpty()) context.title = "Информационное обеспечение"
                else {
                    context.title = currentPath[currentPath.size - 1]
                }
                refreshObjectList("")
            } else if (isFolder) {
                currentPath.add(holder.nameTV.text.toString())
                context.title = holder.nameTV.text.toString()
                refreshObjectList("")
            } else {
                val intent = Intent(context, ObjectInfoActivity::class.java)
                intent.putExtra("name", name)
                intent.putExtra("type", "dataobject")
                intent.putExtra("isAdmin", false)
                context.startActivity(intent)
            }
        }
    }

    fun refreshObjectList(text: String) {
        val dataSet = mutableListOf<ModelObjectList>()
        try {
            val connection = DatabaseConnector(url, username, pass).createConnection()
            val currentUserInfoRS = connection.createStatement()
                .executeQuery("select * from users where login = '$currentUser'")
            currentUserInfoRS.next()
            val currentUserService = currentUserInfoRS.getString("service")
            val currentUserDistrict = currentUserInfoRS.getString("district")
            if (context.title == "Информационное обеспечение") {
                val branchRS = connection.createStatement()
                    .executeQuery("select * from branches where higherbranch is null and idbranch in (select branch from branches_services where service = '$currentUserService') and idbranch in (select branch from branches_districts where district = '$currentUserDistrict') and deleted = '0'  and name like '%$text%'")
                while (branchRS.next()) {
                    val newStmt = connection.createStatement()
                    val nameSet = newStmt.executeQuery(
                        "select login from users where iduser = '" +
                                branchRS.getString("createdby") + "'"
                    )
                    nameSet.next()
                    val creatorName = nameSet.getString("login")
                    dataSet.add(
                        ModelObjectList(
                            branchRS.getString("name"),
                            creatorName,
                            branchRS.getString("creationdate").split(".")[0],
                            true
                        )
                    )
                }
            } else {
                val branchRS = connection.createStatement()
                    .executeQuery("select * from branches where higherbranch in (select idbranch from branches where name = '${context.title}') and idbranch in (select branch from branches_services where service = '$currentUserService') and idbranch in (select branch from branches_districts where district = '$currentUserDistrict')  and deleted = '0'  and name like '%$text%'")
                while (branchRS.next()) {
                    val newStmt = connection.createStatement()
                    val nameSet = newStmt.executeQuery(
                        "select login from users where iduser = '" +
                                branchRS.getString("createdby") + "'"
                    )
                    nameSet.next()
                    val creatorName = nameSet.getString("login")
                    dataSet.add(
                        ModelObjectList(
                            branchRS.getString("name"),
                            creatorName,
                            branchRS.getString("creationdate").split(".")[0],
                            true
                        )
                    )
                }
                val dataObjectRS = connection.createStatement()
                    .executeQuery("select name, createdby, creationdate from dataobjects where branch in (select idbranch from branches where name = '${context.title}') and deleted = '0'  and name like '%$text%'")
                while (dataObjectRS.next()) {
                    val newStmt = connection.createStatement()
                    val nameSet = newStmt.executeQuery(
                        "select login from users where iduser = '" +
                                dataObjectRS.getString("createdby") + "'"
                    )
                    nameSet.next()
                    val creatorName = nameSet.getString("login")
                    dataSet.add(
                        ModelObjectList(
                            dataObjectRS.getString("name"),
                            creatorName,
                            dataObjectRS.getString("creationdate").split(".")[0]
                        )
                    )
                }
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
            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
        }
        this.objectList = dataSet
        this.notifyDataSetChanged()
    }
}