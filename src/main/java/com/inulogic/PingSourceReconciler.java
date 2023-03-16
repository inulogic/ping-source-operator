package com.inulogic;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jboss.logging.Logger;

import com.inulogic.api.PingSource;
import com.inulogic.dependents.outcomes.PingSourceJobListener;
import com.inulogic.dependents.outcomes.PingSourceOutcome;
import com.inulogic.dependents.triggers.JobService;
import com.inulogic.quartz.PingSourceQuartz;
import com.inulogic.quartz.TriggerKeyUtil;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.ConditionManagerImpl;
import io.javaoperatorsdk.operator.api.ConditionTypeSet;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Constants;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

@ControllerConfiguration(namespaces = Constants.WATCH_CURRENT_NAMESPACE)
public class PingSourceReconciler implements Reconciler<PingSource>,
        Cleaner<PingSource>,
        EventSourceInitializer<PingSource>,
        ErrorStatusHandler<PingSource> {

    private static final String EXCEPTION = "Exception";

    private static final String ERROR = "Error";

    private static final String TRIGGER = "Trigger";

    private static final String SCHEDULED = "Scheduled";

    private static final String READY = "Ready";

    private static final Logger logger = Logger.getLogger(PingSourceReconciler.class);

    public static final int POLL_PERIOD = 1000;

    @Inject
    PingSourceJobListener listener;

    @Inject
    JobService jobService;

    @Inject
    KubernetesClient client;

    ConditionTypeSet set = new ConditionTypeSet(READY, List.of(SCHEDULED, TRIGGER));

    @Override
    public ErrorStatusUpdateControl<PingSource> updateErrorStatus(PingSource resource, Context<PingSource> context,
            Exception e) {
        var manager = new ConditionManagerImpl(set, resource.getStatus());
        manager.setFalse(set.getReadyConditionType(), ERROR, e.getMessage());

        return ErrorStatusUpdateControl.updateStatus(resource);
    }

    @Override
    public Map<String, EventSource> prepareEventSources(EventSourceContext<PingSource> context) {
        return EventSourceInitializer.nameEventSources(
                jobService.getQuartzEventSource(),
                listener);
    }

    @Override
    public UpdateControl<PingSource> reconcile(PingSource resource, Context<PingSource> context) {

        var manager = new ConditionManagerImpl(set, resource.getStatus());
        if (resource.getStatus().getConditions().isEmpty()) {
            manager.initialize();
        }

        var spec = resource.getSpec();

        return context.getSecondaryResource(PingSourceQuartz.class).map(pingSourceQuartz -> {
            if (!pingSourceQuartz.getSchedule().equals(spec.getSchedule()) ||
                    !pingSourceQuartz.getContentType().equals(spec.getContentType()) ||
                    !pingSourceQuartz.getData().equals(spec.getData())) {
                jobService.unscheduleJob(pingSourceQuartz);
                jobService.scheduleJob(TriggerKeyUtil.toTriggerKey(resource), spec);
            }

            resource.getStatus().setNextFireTime(pingSourceQuartz.getNextFireTime().map(Date::toString).orElse(null));
            resource.getStatus()
                    .setPreviousFireTime(pingSourceQuartz.getPreviousFireTime().map(Date::toString).orElse(null));

            updateLastOutcome(resource, context, manager);

            return UpdateControl.updateStatus(resource);
        }).orElseGet(() -> {
            
            logger.infof("Create new schedule %s", resource.getMetadata().getName());

            jobService.scheduleJob(TriggerKeyUtil.toTriggerKey(resource), spec);

            manager.setTrue(SCHEDULED);

            return UpdateControl.updateStatus(resource);
        });
    }

    private void updateLastOutcome(PingSource resource, Context<PingSource> context, ConditionManagerImpl manager) {
        var outcome = context.getSecondaryResource(PingSourceOutcome.class);
        outcome.ifPresent(o -> {
            if (o.isSuccess()) {
                manager.setTrue(TRIGGER);
            } else {
                manager.setFalse(TRIGGER, EXCEPTION, o.getException());
            }
            resource.getStatus()
                    .setNextFireTime(o.getNextFireTime().map(Date::toString).orElse(null));
            resource.getStatus()
                    .setPreviousFireTime(o.getPreviousFireTime().toString());
        });
    }

    @Override
    public DeleteControl cleanup(PingSource resource, Context<PingSource> context) {

        var pingSourceQuartz = context.getSecondaryResource(PingSourceQuartz.class);
        if (pingSourceQuartz.isPresent()) {
            jobService.unscheduleJob(pingSourceQuartz.get());
        }

        return DeleteControl.defaultDelete();
    }
}
