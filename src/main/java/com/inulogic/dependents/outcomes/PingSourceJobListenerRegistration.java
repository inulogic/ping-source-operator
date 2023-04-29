package com.inulogic.dependents.outcomes;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;

import org.quartz.SchedulerException;

import io.quarkus.runtime.StartupEvent;

@Dependent
public class PingSourceJobListenerRegistration {

    void onStart(@Observes StartupEvent event, org.quartz.Scheduler scheduler, PingSourceJobListener listener)
            throws SchedulerException {
        scheduler.getListenerManager().addJobListener(listener);
    }
}
