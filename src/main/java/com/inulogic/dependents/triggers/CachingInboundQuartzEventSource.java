package com.inulogic.dependents.triggers;

import javax.enterprise.context.ApplicationScoped;

import com.inulogic.api.PingSource;
import com.inulogic.quartz.PingSourceQuartz;
import com.inulogic.quartz.TriggerKeyUtil;

import io.javaoperatorsdk.operator.processing.event.source.CacheKeyMapper;
import io.javaoperatorsdk.operator.processing.event.source.inbound.CachingInboundEventSource;

/**
 * Manage quartz jobs and triggers as a secondary resource with CachingInboundEventSource
 * This will allow to do reconcile operation without calling Quartz datasource
 */
@ApplicationScoped
public class CachingInboundQuartzEventSource extends CachingInboundEventSource<PingSourceQuartz, PingSource> {

    public CachingInboundQuartzEventSource(ResourceFetcher<PingSourceQuartz, PingSource> pingSourcePollingResourceSupplier) {
        super(pingSourcePollingResourceSupplier, PingSourceQuartz.class, CacheKeyMapper.singleResourceCacheKeyMapper());
    }

    public void handleRecentResourceCreate(PingSourceQuartz quartz) {
        handleResourceEvent(TriggerKeyUtil.toResourceID(quartz.getTriggerKey()), quartz);
    }

    public void handleResourceDeleteEvent(PingSourceQuartz quartz) {
        handleResourceDeleteEvent(TriggerKeyUtil.toResourceID(quartz.getTriggerKey()), this.cacheKeyMapper.keyFor(quartz));
    }
}
