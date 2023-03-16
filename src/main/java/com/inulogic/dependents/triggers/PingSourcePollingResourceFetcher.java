package com.inulogic.dependents.triggers;

import java.util.Collections;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import org.quartz.TriggerKey;

import com.inulogic.api.PingSource;
import com.inulogic.quartz.PingSourceQuartz;
import com.inulogic.quartz.TriggerKeyUtil;

import io.javaoperatorsdk.operator.processing.event.source.inbound.CachingInboundEventSource.ResourceFetcher;

/**
 * This fetcher will help rebuild the cache when no incoming event occured since startup
 */
@ApplicationScoped
public class PingSourcePollingResourceFetcher implements ResourceFetcher<PingSourceQuartz, PingSource> {
    
    private JobService service;

    public PingSourcePollingResourceFetcher(JobService service) {
        this.service = service;
    }

    @Override
    public Set<PingSourceQuartz> fetchResources(PingSource primaryResource) {
        TriggerKey key = TriggerKeyUtil.toTriggerKey(primaryResource);

        return service.get(key).map(Set::of).orElseGet(Collections::emptySet);
    }
}