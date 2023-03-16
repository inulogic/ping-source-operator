package com.inulogic.quartz;

import org.quartz.TriggerKey;

import com.inulogic.api.PingSource;

import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class TriggerKeyUtil {
    private TriggerKeyUtil() {
    }

    public static ResourceID toResourceID(TriggerKey triggerKey) {
        return new ResourceID(triggerKey.getName(), triggerKey.getGroup());
    }

    public static TriggerKey toTriggerKey(PingSource primaryResource) {
        return TriggerKey.triggerKey(primaryResource.getMetadata().getName(), primaryResource.getMetadata().getNamespace());
    }
}
