package me.wxc.widget.flow.list

import me.wxc.widget.base.IScheduleModel

data class FlowListItemModel(
    override val startTime: Long,
    override val endTime: Long,
    val models: MutableList<IScheduleModel>
) : IScheduleModel