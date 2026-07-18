package com.queuectl.service;

import com.queuectl.dto.JobExecutionResult;
import com.queuectl.entity.Job;
import com.queuectl.entity.JobState;
import com.queuectl.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class JobFinalizerTest {

    private JobRepository jobRepository;
    private RetryService retryService;
    private JobFinalizer jobFinalizer;

    @BeforeEach
    void setUp() {
        jobRepository = Mockito.mock(JobRepository.class);
        retryService = Mockito.mock(RetryService.class);
        jobFinalizer = new JobFinalizer(jobRepository, retryService);
    }

    private Job buildJob(int attempts, int maxRetries) {
        return Job.builder()
                .id("job-1")
                .command("echo test")
                .state(JobState.PROCESSING)
                .attempts(attempts)
                .maxRetries(maxRetries)
                .build();
    }

    @Test
    void successfulExecution_movesJobToCompleted() {
        Job job = buildJob(0, 3);
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));

        jobFinalizer.finalizeJob("job-1", new JobExecutionResult(0, "ok", 50));

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        assertEquals(JobState.COMPLETED, captor.getValue().getState());
    }

    @Test
    void failureUnderMaxRetries_movesJobBackToPendingWithBackoff() {
        Job job = buildJob(0, 3);
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(retryService.calculateNextRunTime(1)).thenReturn(LocalDateTime.now().plusSeconds(2));

        jobFinalizer.finalizeJob("job-1", new JobExecutionResult(1, "boom", 50));

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        Job saved = captor.getValue();

        assertEquals(JobState.PENDING, saved.getState());
        assertEquals(1, saved.getAttempts());
        verify(retryService).calculateNextRunTime(1);
    }

    @Test
    void failureExhaustingMaxRetries_movesJobToDeadLetterQueue() {

        Job job = buildJob(3, 3);
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));

        jobFinalizer.finalizeJob("job-1", new JobExecutionResult(1, "boom again", 50));

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        Job saved = captor.getValue();

        assertEquals(JobState.DEAD, saved.getState());
        assertEquals(4, saved.getAttempts());

        verify(retryService, never()).calculateNextRunTime(anyInt());
    }

    @Test
    void unknownJobId_throwsRatherThanSilentlyFailing() {
        when(jobRepository.findById("missing")).thenReturn(Optional.empty());

        org.junit.jupiter.api.function.Executable call =
                () -> jobFinalizer.finalizeJob("missing", new JobExecutionResult(0, "n/a", 0));

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, call);
    }
}
