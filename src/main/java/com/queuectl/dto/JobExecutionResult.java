package com.queuectl.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JobExecutionResult {
    private final int exitCode;
    private final String output;
    private final long durationMillis;
}
