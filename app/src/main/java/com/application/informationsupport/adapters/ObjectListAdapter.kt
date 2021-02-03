package com.application.informationsupport.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.application.informationsupport.DataActivity
import com.application.informationsupport.R
import com.application.informationsupport.models.ModelObjectList

class ObjectListAdapter(val context: Activity, val objectMap: Map<ModelObjectList, Map<ModelObjectList, Nothing?>?>) :
    RecyclerView.Adapter<ObjectListAdapter.ObjectListHolder>() {

    var currentObjectMap = objectMap
    var objectList = currentObjectMap.keys.toMutableList()
    var currentPath = mutableListOf<ModelObjectList>()

    class ObjectListHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {
        var typeIV: ImageView = itemView.findViewById(R.id.imageView)
        var nameTV: TextView = itemView.findViewById(R.id.nameTV)
        var creatorTV: TextView = itemView.findViewById(R.id.creatorTV)
        var dateTV: TextView = itemView.findViewById(R.id.dateTV)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ObjectListHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.row_data_object, parent, false)
        return ObjectListHolder(view)
    }

    override fun getItemCount(): Int {
        return if (context.title.toString() != "Информационное обеспечение") objectList.size +
                1 else objectList.size
    }

    override fun onBindViewHolder(holder: ObjectListHolder, position: Int) {
        val title = context.title.toString()
        val notRootScreen = title != "Информационное обеспечение"
        if (notRootScreen && objectList[0].name != "") {
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
        else if (position != 0 || !notRootScreen) holder.typeIV.setImageResource(R.drawable.ic_object)
        else holder.typeIV.setImageResource(R.drawable.ic_arrow_back)

        holder.itemView.setOnClickListener {
            if (position == 0 && notRootScreen) {
                var tempMap = objectMap
                for (i in 0 until currentPath.size - 1) {
                    tempMap = tempMap[currentPath[i]]!!
                }
                currentObjectMap = tempMap
                objectList = currentObjectMap.keys.toMutableList()
                currentPath.removeAt(currentPath.size - 1)
                if (currentPath.isEmpty()) context.title = "Информационное обеспечение"
                else {
                    context.title = currentPath.last().name
                }
                notifyDataSetChanged()
            }
            else if (isFolder) {
                if (objectMap[ModelObjectList(name, creator, date, isFolder)] != null){
                    context.setTitle(name)
                    val currentNode = ModelObjectList(name, creator, date, isFolder)
                    currentObjectMap = objectMap[currentNode]!!
                    objectList = currentObjectMap.keys.toMutableList()
                    currentPath.add(currentNode)
                    notifyDataSetChanged()
                }
            }
            else {
                val intent = Intent(context, DataActivity::class.java)
                intent.putExtra("name", name)
                intent.putExtra("date", date)
                intent.putExtra("creator", creator)
                context.startActivity(intent)
            }
        }
    }


}