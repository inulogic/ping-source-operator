package io.javaoperatorsdk.operator.api;

import java.util.Collection;

public class ConditionTypeSet {
    private String readyConditionType;
    private Collection<String> dependentsConditionType;

    public String getReadyConditionType() {
        return readyConditionType;
    }

    public Collection<String> getDependentsConditionType() {
        return dependentsConditionType;
    }

    public void setDependentsConditionType(Collection<String> dependentsConditionType) {
        this.dependentsConditionType = dependentsConditionType;
    }

    public void setReadyConditionType(String readyConditionType) {
        this.readyConditionType = readyConditionType;
    }

    public ConditionTypeSet(String ready, Collection<String> dependentsConditionType) {
        this.readyConditionType = ready;
        this.dependentsConditionType = dependentsConditionType;
    }
}
