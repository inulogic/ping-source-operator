package com.inulogic.dependents.triggers;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

import com.inulogic.api.PingSourceSpec;
import com.inulogic.quartz.PingSourceJob;
import com.inulogic.quartz.PingSourceQuartz;

@ApplicationScoped
public class JobService {
    
    @Inject
    org.quartz.Scheduler quartz;

    CachingInboundQuartzEventSource quartzEventSource;

    public JobService() {
        quartzEventSource = new CachingInboundQuartzEventSource(new PingSourcePollingResourceFetcher(this));
    }

    public CachingInboundQuartzEventSource getQuartzEventSource() {
        return quartzEventSource;
    }

    public void scheduleJob(TriggerKey key, PingSourceSpec spec) {
        JobDetail job = JobBuilder.newJob(PingSourceJob.class)
                .withIdentity(key.getName(), key.getGroup())
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(key)
                .withSchedule(CronScheduleBuilder.cronSchedule(spec.getSchedule()))
                .build();
        trigger.getJobDataMap().put("data", spec.getData());
        trigger.getJobDataMap().put("content-type", spec.getContentType());

        try {
            quartz.scheduleJob(job, trigger);

            // update secondary resource cache
            quartzEventSource.handleRecentResourceCreate(toQuartz(trigger));
        } catch (SchedulerException e) {
            throw new IllegalStateException(e);
        }
    }

    public void unscheduleJob(PingSourceQuartz pingSourceQuartz) {
        try {
            quartz.unscheduleJob(pingSourceQuartz.getTriggerKey());

            // update secondary resource cache
            quartzEventSource.handleResourceDeleteEvent(pingSourceQuartz);
        } catch (SchedulerException e) {
            throw new IllegalStateException(e);
        }
    }

    public Optional<PingSourceQuartz> get(TriggerKey key) {
        try {
            if (!quartz.checkExists(key))
                return Optional.empty();

            var trigger = quartz
                    .getTrigger(key);
            return Optional.of(toQuartz(trigger));
        } catch (SchedulerException e) {
            throw new IllegalStateException(e);
        }
    }

    private PingSourceQuartz toQuartz(Trigger trigger) {
        var pingSourceQuartz = new PingSourceQuartz();

        pingSourceQuartz.setTriggerKey(trigger.getKey());
        if (trigger instanceof CronTrigger) {
            pingSourceQuartz.setSchedule(((CronTrigger) trigger).getCronExpression());
        }

        pingSourceQuartz.setNextFireTime(Optional.ofNullable(trigger.getNextFireTime()));
        pingSourceQuartz.setPreviousFireTime(Optional.ofNullable(trigger.getPreviousFireTime()));
        pingSourceQuartz.setData(trigger.getJobDataMap().getString("data"));
        pingSourceQuartz.setContentType(trigger.getJobDataMap().getString("content-type"));

        return pingSourceQuartz;
    }
}
