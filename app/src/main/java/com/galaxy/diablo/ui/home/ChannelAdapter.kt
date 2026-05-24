package com.galaxy.diablo.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.galaxy.diablo.R
import com.galaxy.diablo.data.Channel
import com.galaxy.diablo.data.PrefsManager

class ChannelAdapter(
    private var items: List<Channel>,
    private val onClick: (Channel, Int) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.VH>() {

    fun submit(list: List<Channel>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val logo: ImageView = v.findViewById(R.id.iv_logo)
        val fallback: TextView = v.findViewById(R.id.tv_logo_fallback)
        val name: TextView = v.findViewById(R.id.tv_name)
        val star: ImageView = v.findViewById(R.id.iv_favorite_star)
        val drmBadge: TextView = v.findViewById(R.id.tv_drm_badge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ch = items[position]
        holder.name.text = ch.name
        holder.star.visibility = if (PrefsManager.isFavorite(ch.name)) View.VISIBLE else View.GONE
        holder.drmBadge.visibility = if (ch.hasDrm) View.VISIBLE else View.GONE
        holder.fallback.text = ch.monogram

        if (!ch.logoUrl.isNullOrBlank()) {
            holder.logo.visibility = View.VISIBLE
            holder.logo.load(ch.logoUrl) {
                crossfade(true)
                listener(
                    onSuccess = { _, _ -> holder.fallback.visibility = View.GONE },
                    onError = { _, _ -> holder.fallback.visibility = View.VISIBLE; holder.logo.visibility = View.GONE }
                )
            }
        } else {
            holder.logo.visibility = View.GONE
            holder.fallback.visibility = View.VISIBLE
        }

        holder.itemView.setOnClickListener { onClick(ch, position) }
        holder.itemView.setOnLongClickListener {
            val nowFav = PrefsManager.toggleFavorite(ch.name)
            holder.star.visibility = if (nowFav) View.VISIBLE else View.GONE
            true
        }
    }

    override fun getItemCount(): Int = items.size
}
