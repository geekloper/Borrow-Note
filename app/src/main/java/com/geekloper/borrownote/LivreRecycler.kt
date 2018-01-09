package com.geekloper.borrownote

import android.graphics.BitmapFactory
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import java.util.*

data class LivreRVD(val id: Long,
                   val titre: String,
                   val date: Date,
                   val image: String?)

class LivreRecyclerAdapter(val list: List<LivreRVD>, val listener: (LivreRVD) -> Unit) : RecyclerView.Adapter<LivreRecyclerAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var titre: TextView = view.findViewById(R.id.tv_nom_livre)
        var date: TextView = view.findViewById(R.id.tv_date)
        var image: ImageView = view.findViewById(R.id.iv_image)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.titre.text = list[position].titre
        holder.date.text = list[position].date.toLocaleString()
        if(list[position].image != null) {
            holder.image.setImageBitmap(BitmapFactory.decodeFile(list[position].image))
        } else {
            holder.image.setImageBitmap(BitmapFactory.decodeResource(holder.image.context.resources, R.drawable.pas_d_image))

        }
        holder.itemView.setOnClickListener{ listener(list[position]) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MessageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_message, parent, false))

    override fun getItemCount() = list.size
}