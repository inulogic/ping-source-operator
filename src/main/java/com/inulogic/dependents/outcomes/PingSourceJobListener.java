package com.inulogic.dependents.outcomes;

import static com.inulogic.quartz.TriggerKeyUtil.toResourceID;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.Trigger;

import com.inulogic.api.PingSource;

import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.CacheKeyMapper;
import io.javaoperatorsdk.operator.processing.event.source.ExternalResourceCachingEventSource;
import io.javaoperatorsdk.operator.processing.event.source.ResourceEventAware;

/**
 * Manage job outcomes as external resources
 */
@ApplicationScoped
public class PingSourceJobListener extends ExternalResourceCachingEventSource<PingSourceOutcome, PingSource>
        implements JobListener, ResourceEventAware<PingSource> {

    protected PingSourceJobListener() {
        super(PingSourceOutcome.class, CacheKeyMapper.singleResourceCacheKeyMapper());
    }

    public static final String LISTENER_NAME = PingSourceJobListener.class.getName();

    @Override
    public String getName() {
        return LISTENER_NAME; //must return a name
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        // nothing to do
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        // nothing to do
    }

    //Run this after job has been executed
    @Override
    public void jobWasExecuted(JobExecutionContext context,
            JobExecutionException jobException) {

        Trigger trigger = context.getTrigger();

        var pso = new PingSourceOutcome();
        map(context, jobException, pso);

        // update the cache and trigger reconciler
        handleResources(toResourceID(trigger.getKey()), pso);
    }

    private void map(JobExecutionContext context, JobExecutionException jobException, PingSourceOutcome existing) {
        if (jobException != null)
            existing.setException(jobException.getMessage());
        else
            existing.setException(null);

        existing.setNextFireTime(Optional.ofNullable(context.getNextFireTime()));
        existing.setPreviousFireTime(context.getScheduledFireTime());
    }

    @Override
    public void onResourceDeleted(PingSource resource) {
        // remove ping source outcome from the cache
        handleDelete(ResourceID.fromResource(resource));
    }
}
