package com.application.informationsupport.adapters

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.application.informationsupport.DataActivity
import com.application.informationsupport.ObjectInfoActivity
import com.application.informationsupport.R
import com.application.informationsupport.models.ModelServiceInfo

class ServiceItemAdapter (val context: Activity, val objectList: List<ModelServiceInfo>) :
    RecyclerView.Adapter<ServiceItemAdapter.ServiceItemHolder>() {

    class ServiceItemHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {
        var nameTV: TextView = itemView.findViewById(R.id.nameTV)
        var creatorTV: TextView = itemView.findViewById(R.id.creatorTV)
        var dateTV: TextView = itemView.findViewById(R.id.dateTV)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceItemHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.row_service, parent, false)
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
            val intent = Intent(context, ObjectInfoActivity::class.java)
            intent.putExtra("name", name)
            intent.putExtra("type", "service")
            context.startActivity(intent)
        }
    }


}