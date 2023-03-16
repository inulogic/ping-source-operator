package com.inulogic.dependents.outcomes;

import java.util.Date;
import java.util.Optional;

import lombok.Data;

@Data
public class PingSourceOutcome {

    private String exception;
    private Date previousFireTime;
    private Optional<Date> nextFireTime;

    public boolean isSuccess() {
        return exception == null;
    }
}
