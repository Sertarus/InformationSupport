package com.application.informationsupport.adapters

import android.app.Activity
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.application.informationsupport.R

class AddingItemListAdapter(
    val context: Activity,
    var addedItemList: MutableList<Pair<String, Int>>
) : RecyclerView.Adapter<AddingItemListAdapter.AddingItemHolder>() {

    private var counter = 1

    class AddingItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var nameTV: TextView = itemView.findViewById(R.id.nameTV)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddingItemHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.row_record, parent, false)
        return AddingItemHolder(view)
    }

    override fun getItemCount(): Int {
        return addedItemList.size
    }

    override fun onBindViewHolder(holder: AddingItemHolder, position: Int) {
        holder.nameTV.text = addedItemList[position].first
        holder.itemView.setOnClickListener {
            val options = arrayOf(
                "Изменение",
                "Удаление"
            )
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Выберите действие")
            builder.setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val view = LayoutInflater.from(context)
                            .inflate(R.layout.dialog_create_edit_simple_info, null)
                        val changeTV = view.findViewById<TextView>(R.id.addTV)
                        changeTV.text = "Изменить реквизит"
                        val nameET = view.findViewById<EditText>(R.id.nameET)
                        nameET.text = SpannableStringBuilder(holder.nameTV.text)
                        val button = view.findViewById<Button>(R.id.createSimpleInfoButton)
                        button.text = "Изменить"
                        val changeBuilder = AlertDialog.Builder(context)
                        changeBuilder.setView(view)
                        val ad = changeBuilder.create()
                        ad.show()
                        button.setOnClickListener {
                            if (nameET.text.toString().length in 1..20) {
                                addedItemList[position] = Pair(nameET.text.toString(), position + 1)
                                this.notifyDataSetChanged()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Длина имени реквизита должна составлять от 1 до 20 символов",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            ad.dismiss()
                        }
                    }

                    1 -> {
                        addedItemList.removeAt(position)
                        counter--
                        this.notifyDataSetChanged()
                    }
                }
            }
            builder.create().show()
        }
    }

    fun addNewRecord() {
        addedItemList.add(Pair("Реквизит$counter", counter))
        counter++
        this.notifyDataSetChanged()
    }

}