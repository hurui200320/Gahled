package info.skyblond.gahled

import info.skyblond.gahled.bot.GahledBot
import org.quartz.Scheduler

object StatusService {
    var gahledBot: GahledBot? = null

    var scheduler: Scheduler? = null
}
