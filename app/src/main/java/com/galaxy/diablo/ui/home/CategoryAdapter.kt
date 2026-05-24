package com.galaxy.diablo.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.galaxy.diablo.R
import com.galaxy.diablo.data.Category

class CategoryAdapter(
    private var items: List<Category>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.VH>() {

    private var selectedPosition: Int = 0

    fun submit(list: List<Category>, keepSelection: Boolean = false) {
        items = list
        if (!keepSelection || selectedPosition >= list.size) selectedPosition = 0
        notifyDataSetChanged()
    }

    fun selected(): Category? = items.getOrNull(selectedPosition)

    fun selectByName(name: String): Boolean {
        val idx = items.indexOfFirst { it.name == name }
        if (idx >= 0) {
            val old = selectedPosition
            selectedPosition = idx
            notifyItemChanged(old)
            notifyItemChanged(idx)
            return true
        }
        return false
    }

    inner class VH(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tv_category)
        val count: TextView = view.findViewById(R.id.tv_category_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.name.text = c.name
        holder.count.text = c.count.toString()
        holder.itemView.isSelected = (position == selectedPosition)
        holder.itemView.setOnClickListener {
            val old = selectedPosition
            selectedPosition = position
            notifyItemChanged(old)
            notifyItemChanged(position)
            onClick(position)
        }
        // Also fire selection on remote-focus
        holder.itemView.setOnFocusChangeListener { _, focused ->
            if (focused && position != selectedPosition) {
                val old = selectedPosition
                selectedPosition = position
                notifyItemChanged(old)
                notifyItemChanged(position)
                onClick(position)
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
