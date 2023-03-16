package com.inulogic.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class PingSourceSpec {

    /**
     * https://www.freeformatter.com/cron-expression-generator-quartz.html
     */
    private String schedule;
    private String contentType;
    private String data;
}
