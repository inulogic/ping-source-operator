package com.inulogic.quartz;

import java.util.Date;
import java.util.Optional;

import org.quartz.TriggerKey;

import lombok.Data;

@Data
public class PingSourceQuartz {

    private TriggerKey triggerKey;
    private Optional<Date> previousFireTime;
    private Optional<Date> nextFireTime;
    private String schedule;
    private String contentType;
    private String data;
}
