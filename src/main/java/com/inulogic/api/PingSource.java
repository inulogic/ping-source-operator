package com.inulogic.api;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;

@Group("com.inulogic")
@Version("v1alpha1")
@RegisterForReflection
public class PingSource extends CustomResource<PingSourceSpec, PingSourceStatus> implements Namespaced {
    
    @Override
    protected PingSourceStatus initStatus() {
        return new PingSourceStatus();
    }
}
