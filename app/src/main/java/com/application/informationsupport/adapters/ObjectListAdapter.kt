package com.application.informationsupport.adapters

import android.app.Activity
import android.content.Intent
import android.database.SQLException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import java.sql.Blob
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
        return if (context.title.toString() != "Информационное обеспечение" && context.title.toString() != "Новая информация" && (objectList.isEmpty() || objectList[0].name != "") && !isSearch) objectList.size +
                1 else objectList.size
    }

    override fun onBindViewHolder(holder: ObjectListHolder, position: Int) {
        val title = context.title.toString()
        val notRootScreen = title != "Информационное обеспечение" && title != "Новая информация"
        if (notRootScreen && (objectList.isEmpty() || objectList[0].name != "") && !isSearch) {
            objectList.add(0, ModelObjectList("", "", "", false, null))
        }
        val name = objectList[position].name
        val creator = objectList[position].creator
        val date = objectList[position].date
        val isFolder = objectList[position].isFolder
        val image = objectList[position].image

        holder.nameTV.text = name
        holder.creatorTV.text = creator
        holder.dateTV.text = date
        if (isFolder) holder.typeIV.setImageResource(R.drawable.ic_folder)
        else if ((position != 0 || !notRootScreen) || isSearch) {
            if (image == null) {
                holder.typeIV.setImageResource(R.drawable.ic_object)
            }
            else {
                holder.typeIV.setImageBitmap(Bitmap.createScaledBitmap(image, 200, 200, false))
            }
        }
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
                intent.putExtra("user", currentUser)
                intent.putExtra("type", "dataobject")
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
                            true, null
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
                            true, null
                        )
                    )
                }
                val dataObjectRS = connection.createStatement()
                    .executeQuery("select name, createdby, creationdate, image from dataobjects where branch in (select idbranch from branches where name = '${context.title}') and deleted = '0'  and name like '%$text%'")
                while (dataObjectRS.next()) {
                    val newStmt = connection.createStatement()
                    val nameSet = newStmt.executeQuery(
                        "select login from users where iduser = '" +
                                dataObjectRS.getString("createdby") + "'"
                    )
                    nameSet.next()
                    val creatorName = nameSet.getString("login")
                    val image = dataObjectRS.getBlob("image")
                    var bitmap: Bitmap? = null
                    if (image != null) {
                        val bytes = image.getBytes(1L, image.length().toInt())
                        bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                    dataSet.add(
                        ModelObjectList(
                            dataObjectRS.getString("name"),
                            creatorName,
                            dataObjectRS.getString("creationdate").split(".")[0], false, bitmap)
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

    fun refreshNewObjectList() {
        val dataSet = mutableListOf<ModelObjectList>()
        try {
            val connection = DatabaseConnector(url, username, pass).createConnection()
            val currentUserInfoRS = connection.createStatement()
                .executeQuery("select * from users where login = '$currentUser'")
            currentUserInfoRS.next()
            val currentUserService = currentUserInfoRS.getString("service")
            val currentUserDistrict = currentUserInfoRS.getString("district")
            val rs = connection.createStatement().executeQuery("select name, branch, image, d.createdby, d.creationdate, u.login from dataobjects d left join users u on u.iduser = d.createdby where (extract (day from (SYSTIMESTAMP - d.creationdate)) < 1 or extract (day from (SYSTIMESTAMP - d.changeddate)) < 1) and d.deleted = 0 and branch in (select branch from branches_districts where district = $currentUserDistrict and deleted = 0) and branch in (select branch from branches_services where service = $currentUserService and deleted = 0) order by d.creationdate")
            while (rs.next()) {
                val image = rs.getBlob("image")
                var bitmap: Bitmap? = null
                if (image != null) {
                    val bytes = image.getBytes(1L, image.length().toInt())
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                dataSet.add(ModelObjectList(rs.getString("name"), rs.getString("login"), rs.getString("creationdate").split(".")[0], false, bitmap))
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