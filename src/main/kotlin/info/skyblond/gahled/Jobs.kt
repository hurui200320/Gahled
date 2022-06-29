package info.skyblond.gahled

import info.skyblond.gahled.domains.KeyValueOperations
import mu.KotlinLogging
import org.quartz.*


class StartCollectingJob : Job {
    private val logger = KotlinLogging.logger { }

    /**
     * Start the collecting phase.
     * 1. set up a new version
     * 2. switch to collecting state
     * 3. call bot to send post about collecting
     * */
    override fun execute(context: JobExecutionContext) {
        logger.info { "Switched to collecting phase" }
//        val date = Date()
//        val sf = SimpleDateFormat("yyyyMMdd")
//        val newVersion = sf.format(date).toLong()
        val newVersion = System.currentTimeMillis()
        KeyValueOperations.setCollectingCurrentVersion(newVersion)
        KeyValueOperations.setCurrentState(KeyValueOperations.State.COLLECTING)
        StatusService.gahledBot!!.startCollecting()
    }
}

fun setUpStartCollectingJob(scheduler: Scheduler) {
    val job = JobBuilder.newJob(StartCollectingJob::class.java)
        .withIdentity("startCollectingJob", "gahledGroup").build()

    val trigger = TriggerBuilder.newTrigger()
        .withIdentity("startCollectingTrigger", "gahledGroup")
        .withSchedule(CronScheduleBuilder.cronSchedule(ConfigService.getBotStartCollectingCron()))
        .build()

    scheduler.scheduleJob(job, trigger)
}

class StartVotingJob : Job {
    private val logger = KotlinLogging.logger { }

    /**
     * Start the voting phase.
     * 1. switch to voting stats
     * 2. call bot to send post about voting
     * */
    override fun execute(context: JobExecutionContext) {
        logger.info { "Switched to voting phase" }
        KeyValueOperations.setCurrentState(KeyValueOperations.State.VOTING)
        StatusService.gahledBot!!.startPoll()
    }
}

fun setUpStartVotingJob(scheduler: Scheduler) {
    val job = JobBuilder.newJob(StartVotingJob::class.java)
        .withIdentity("startVotingJob", "gahledGroup").build()

    val trigger = TriggerBuilder.newTrigger()
        .withIdentity("startVotingTrigger", "gahledGroup")
        .withSchedule(CronScheduleBuilder.cronSchedule(ConfigService.getBotStartVotingCron()))
        .build()

    scheduler.scheduleJob(job, trigger)
}

class StartPublishingJob : Job {
    private val logger = KotlinLogging.logger { }

    /**
     * Start the publishing phase.
     * 1. switch to ready mode
     * 2. call bot to send post about stop voting
     * */
    override fun execute(context: JobExecutionContext) {
        logger.info { "Switched to publishing phase" }
        KeyValueOperations.setCurrentState(KeyValueOperations.State.READY)
        StatusService.gahledBot!!.stopPoll()
    }
}

fun setUpStartPublishingJob(scheduler: Scheduler) {
    val job = JobBuilder.newJob(StartPublishingJob::class.java)
        .withIdentity("startPublishingJob", "gahledGroup").build()

    val trigger = TriggerBuilder.newTrigger()
        .withIdentity("startPublishingTrigger", "gahledGroup")
        .withSchedule(CronScheduleBuilder.cronSchedule(ConfigService.getBotStartPublishingCron()))
        .build()

    scheduler.scheduleJob(job, trigger)
}
