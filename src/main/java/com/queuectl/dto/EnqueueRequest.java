package com.queuectl.dto;

import lombok.Data;

@Data
public class EnqueueRequest {
    private String id;
    private String command;
    private int priority = 0;
    private int maxRetries = 3;
    private int timeout = 60;
}