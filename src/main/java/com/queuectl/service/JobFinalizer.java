package com.queuectl.service;

import com.queuectl.dto.JobExecutionResult;
import com.queuectl.entity.Job;
import com.queuectl.entity.JobState;
import com.queuectl.repository.JobRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobFinalizer {
    private final JobRepository jobRepository;
    private final RetryService retryService;

    @Transactional
    public void finalizeJob(String jobId, JobExecutionResult result) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

        job.setLogOutput(result.getOutput());
        job.setExecutionTimeMillis(result.getDurationMillis());

        if (result.getExitCode() == 0) {
            handleSuccess(job);
        } else {
            handleFailure(job);
        }

        jobRepository.save(job);
    }

    private void handleFailure(Job job) {

        job.setAttempts(job.getAttempts() + 1);

        if (job.getAttempts() <= job.getMaxRetries()) {
            job.setState(JobState.PENDING);
            job.setRunAt(retryService.calculateNextRunTime(job.getAttempts()));
            log.warn("Job {} failed. Retrying at {}", job.getId(), job.getRunAt());
        } else {
            log.error("Job {} exhausted all retries. Moving to DLQ.", job.getId());
            job.setState(JobState.DEAD);
        }
    }

    private void handleSuccess(Job job) {
        log.info("Job {} completed successfully.", job.getId());
        job.setState(JobState.COMPLETED);
    }
}
