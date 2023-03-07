package me.wxc.widget.flow.list

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class FlowListView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : RecyclerView(context, attrs) {
    init {
        layoutManager = LinearLayoutManager(context)
        adapter = Adapter()
    }

    inner class Adapter : ListAdapter<FlowListItemModel, VH>(
        object : DiffUtil.ItemCallback<FlowListItemModel>() {
            override fun areItemsTheSame(
                oldItem: FlowListItemModel,
                newItem: FlowListItemModel
            ) = oldItem.startTime == newItem.startTime

            override fun areContentsTheSame(
                oldItem: FlowListItemModel,
                newItem: FlowListItemModel
            ) = oldItem.startTime == newItem.endTime
                    && oldItem.endTime == newItem.endTime
                    && oldItem.models == newItem.models

        }
    ) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(parent.context)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            (holder.itemView as FlowListItemView).run {
                calendar.timeInMillis = getItem(position).startTime
                scheduleModels = getItem(position).models.toList()
            }
        }

    }

    class VH(context: Context) : ViewHolder(FlowListItemView(context))
}