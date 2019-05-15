package com.example.dualbtcmd

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

class BtListAdapter(val items: ArrayList<String>) : RecyclerView.Adapter<BtListAdapter.ViewHolder>() {
    lateinit var clickListener: OnItemClickListener
    lateinit var longClickListener: OnItemLongClickListener
    private val images = intArrayOf(R.drawable.android_image_1, R.drawable.android_image_2,
        R.drawable.android_image_3, R.drawable.android_image_4, R.drawable.android_image_5,
        R.drawable.android_image_6, R.drawable.android_image_7, R.drawable.android_image_8)

    interface OnItemClickListener {
        fun onItemClick(view: View, position: Int, bda: String)
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(view: View, position: Int, bda: String): Boolean
    }

    fun setOnItemClickListener(listen: OnItemClickListener) {
        this.clickListener = listen
    }

    fun setOnItemLongClickListener(listen: OnItemLongClickListener): Boolean {
        this.longClickListener = listen
        return true
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var itemImage: ImageView
        var itemName: TextView
        var itemAddress: TextView

        init {
            itemImage = itemView.findViewById(R.id.itemImage)
            itemName = itemView.findViewById(R.id.itemName)
            itemAddress = itemView.findViewById(R.id.itemAddress)
            // itemView.setOnClickListener { v: View ->
            //     Log.d(KotlinLog, "recyclerView onclick $adapterPosition")
            // }
            itemView.setOnClickListener(View.OnClickListener {
                clickListener.onItemClick(it, adapterPosition, items[adapterPosition].removeRange(0, items[adapterPosition].lastIndexOf(" + ") + 3))
            })

            itemView.setOnLongClickListener(View.OnLongClickListener {
                longClickListener.onItemLongClick(it, adapterPosition, items[adapterPosition].removeRange(0, items[adapterPosition].lastIndexOf(" + ") + 3))
            })
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // LayoutInflater.from(viewGroup.context).inflate(R.layout.card_layout, viewGroup, false).setOnClickListener(this)
        return ViewHolder(LayoutInflater.from(viewGroup.context).inflate(R.layout.card_layout, viewGroup, false))
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemName.text = items[position].removeRange(items[position].lastIndexOf(" + "), items[position].lastIndex + 1)
        holder.itemAddress.text = items[position].removeRange(0, items[position].lastIndexOf(" + ") + 3)
        holder.itemImage.setImageResource(images[position % images.size])
    }
}
