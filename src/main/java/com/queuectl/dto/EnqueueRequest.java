package com.queuectl.dto;

import lombok.Data;

@Data
public class EnqueueRequest {
    private String id;
    private String command;
    private Integer priority;
    private Integer maxRetries;
    private Integer timeout;
}