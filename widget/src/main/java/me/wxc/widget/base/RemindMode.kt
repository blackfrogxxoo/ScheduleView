package me.wxc.widget.base

sealed interface RemindMode : java.io.Serializable {
    val remindMinutes: Int
    val showText: String

    object Never : RemindMode {
        override val remindMinutes: Int = -1
        override val showText: String = "无"
    }

    object OnTime : RemindMode {
        override val remindMinutes: Int = 0
        override val showText: String = "准时"
    }

    object FiveMinutes : RemindMode {
        override val remindMinutes: Int = 5
        override val showText: String = "提前5分钟"
    }

    object HalfHour : RemindMode {
        override val remindMinutes: Int = 30
        override val showText: String = "提前30分钟"
    }

    object OneHour : RemindMode {
        override val remindMinutes: Int = 60
        override val showText: String = "提前1小时"
    }

    object OneDay : RemindMode {
        override val remindMinutes: Int = 24 * 60
        override val showText: String = "提前1天"
    }

    companion object {
        val Int.parseLocalRemindMode: RemindMode
            get() = when (this) {
                0 -> OnTime
                5 -> FiveMinutes
                30 -> HalfHour
                60 -> OneHour
                24 * 60 -> OneDay
                else -> Never
            }
    }
}