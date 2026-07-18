package com.queuectl.repository;

import com.queuectl.entity.Job;
import com.queuectl.entity.JobState;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
@ActiveProfiles("test")
class JobPersistenceTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void savedJob_isReadableAfterClearingPersistenceContext() {
        String jobId = "persist-" + UUID.randomUUID();
        Job job = Job.builder()
                .id(jobId)
                .command("echo persisted")
                .state(JobState.PENDING)
                .attempts(0)
                .maxRetries(3)
                .build();

        jobRepository.save(job);
        entityManager.flush();
        entityManager.clear();

        Optional<Job> reloaded = jobRepository.findById(jobId);

        assertTrue(reloaded.isPresent(), "Job should be readable straight from disk after cache is cleared");
        assertEquals("echo persisted", reloaded.get().getCommand());
        assertEquals(JobState.PENDING, reloaded.get().getState());
    }
}
