package com.queuectl.service;

import com.queuectl.entity.Job;
import com.queuectl.entity.JobState;
import com.queuectl.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JobAcquisitionService {

    private final JobRepository jobRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Job> acquireNextJob() {
        Optional<Job> optionalJob = jobRepository.findFirstByStateAndRunAtBeforeOrderByPriorityDescCreatedAtAsc(
                JobState.PENDING,
                LocalDateTime.now());

        if (optionalJob.isPresent()) {
            Job job = optionalJob.get();
            job.setState(JobState.PROCESSING);
            jobRepository.save(job);
            return Optional.of(job);
        }

        return Optional.empty();
    }
}
