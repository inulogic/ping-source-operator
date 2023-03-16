package io.javaoperatorsdk.operator.api;

public interface ConditionManager {

    public static final String TRUE = "True";
    public static final String FALSE = "False";
    public static final String UNKNOWN = "Unknown";

    // add all conditions defined in the conditionSet
    void initialize();

    // set a condition to True and set the ready condition to True if all depedents
    // are True
    void setTrue(String conditionType);

    // set a condition to False and set theready condition to False
    void setFalse(String conditionType, String reason, String message);
    // some override of True/False to pass reason/message

}
