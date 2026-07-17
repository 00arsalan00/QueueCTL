package com.queuectl.service;

import com.queuectl.entity.Job;
import com.queuectl.entity.JobState;
import com.queuectl.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecoveryService {

    private final JobRepository jobRepository;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void recoverStuckJob() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);

        List<Job> stuckJobs = jobRepository.findByStateAndUpdatedAtBefore(
                JobState.PROCESSING, threshold
        );

        for (Job job : stuckJobs) {
            log.warn("Recovering stuck job: {}", job.getId());

            job.setState(JobState.PENDING);
            job.setAttempts(job.getAttempts() + 1);

            jobRepository.save(job);
        }
    }
}