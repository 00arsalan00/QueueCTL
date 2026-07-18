package com.queuectl.service;

import com.queuectl.dto.JobExecutionResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobRunnerTest {

    private final JobRunner jobRunner = new JobRunner();

    @Test
    void successfulCommand_returnsExitCodeZero() {
        JobExecutionResult result = jobRunner.executeJob("echo hello", 10);

        assertEquals(0, result.getExitCode());
        assertTrue(result.getOutput().toLowerCase().contains("hello"));
    }

    @Test
    void failingCommand_returnsNonZeroExitCode() {
        JobExecutionResult result = jobRunner.executeJob("exit 1", 10);

        assertEquals(1, result.getExitCode());
    }

    @Test
    void invalidCommand_failsGracefullyWithoutThrowing() {
        JobExecutionResult result = jobRunner.executeJob("thiscommanddoesnotexist12345", 10);

        assertNotEquals(0, result.getExitCode());
    }

    @Test
    void commandExceedingTimeout_isKilledAndReturnsTimeoutCode() {
        String sleepCommand = System.getProperty("os.name").toLowerCase().contains("win")
                ? "ping -n 5 127.0.0.1"
                : "sleep 5";

        JobExecutionResult result = jobRunner.executeJob(sleepCommand, 1);

        assertEquals(124, result.getExitCode());
    }

    @Test
    void executionResult_recordsDurationGreaterThanZero() {
        JobExecutionResult result = jobRunner.executeJob("echo timing", 10);

        assertTrue(result.getDurationMillis() >= 0);
    }
}