package io.javaoperatorsdk.operator.api;

import java.util.List;

import io.fabric8.kubernetes.api.model.Condition;

public interface ConditionsSetAware {
    void setConditions(List<Condition> conditions);

    List<Condition> getConditions();
}
