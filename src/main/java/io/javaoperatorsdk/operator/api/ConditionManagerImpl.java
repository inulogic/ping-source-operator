package io.javaoperatorsdk.operator.api;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.Condition;

public class ConditionManagerImpl implements ConditionManager {
    private ConditionTypeSet set;
    private ConditionsSetAware status;

    public ConditionManagerImpl(ConditionTypeSet set, ConditionsSetAware status) {
        this.set = set;
        this.status = status;
    }

    @Override
    public void initialize() {

        initializeCondition(set.getReadyConditionType(), UNKNOWN);

        set.getDependentsConditionType()
                .forEach(ct -> initializeCondition(ct, UNKNOWN));
    }

    private void initializeCondition(String conditionType, String status) {
        initializeCondition(conditionType, status, null, null);
    }

    private void initializeCondition(
            String conditionType, String status, String reason, String message) {
        var ready = new Condition();
        ready.setType(conditionType);
        ready.setStatus(status);
        ready.setReason(reason);
        ready.setMessage(message);

        setCondition(ready);
    }

    private void setCondition(Condition condition) {

        var conditionType = condition.getType();

        List<Condition> conditions = new ArrayList<>();
        var it = status
                .getConditions().iterator();
        while (it.hasNext()) {
            var existingCondition = it.next();
            if (existingCondition.getType().equals(conditionType)) {
                condition.setLastTransitionTime(existingCondition.getLastTransitionTime());
                condition.setAdditionalProperties(existingCondition.getAdditionalProperties());
                if (condition.equals(existingCondition)) {
                    return;
                }
            } else {
                conditions.add(existingCondition);
            }
        }

        condition.setLastTransitionTime(OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        conditions.add(condition);

        conditions.sort((c1, c2) -> c1.getType().compareTo(c2.getType()));

        status.setConditions(conditions);
    }

    @Override
    public void setTrue(String conditionType) {
        initializeCondition(conditionType, TRUE);
        recomputeReady(conditionType);
    }

    @Override
    public void setFalse(String conditionType, String reason, String message) {
        initializeCondition(conditionType, FALSE, reason, message);
        recomputeReady(conditionType);
    }

    private void recomputeReady(String conditionType) {
        Condition c = findUnreadyDependent();
        if (c != null) {
            initializeCondition(set.getReadyConditionType(), c.getStatus());
        } else if (!conditionType.equals(set.getReadyConditionType())) {
            initializeCondition(set.getReadyConditionType(), TRUE);
        }
    }

    private Condition findUnreadyDependent() {
        if (set.getDependentsConditionType().isEmpty()) {
            return null;
        }

        List<Condition> existingDependents = status.getConditions().stream()
                .filter(c -> !c.getType().equals(set.getReadyConditionType()))
                .sorted((c0, c1) -> {
                    var date0 = OffsetDateTime.parse(
                            c0.getLastTransitionTime(), DateTimeFormatter.ISO_DATE_TIME);
                    var date1 = OffsetDateTime.parse(
                            c1.getLastTransitionTime(), DateTimeFormatter.ISO_DATE_TIME);
                    return date0.compareTo(date1);
                })
                .collect(Collectors.toList());

        return existingDependents.stream()
                .filter(c -> c.getStatus().equals(FALSE))
                .findFirst()
                .orElseGet(
                        () -> existingDependents.stream()
                                .filter(c -> c.getStatus().equals("UNKOWN"))
                                .findFirst()
                                .orElse(null));
    }
}
