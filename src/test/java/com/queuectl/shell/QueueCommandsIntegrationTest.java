package com.queuectl.shell;

import com.queuectl.entity.Job;
import com.queuectl.entity.JobState;
import com.queuectl.repository.JobRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@ActiveProfiles("test")
class QueueCommandsIntegrationTest {

    @Autowired
    private QueueCommands queueCommands;

    @Autowired
    private JobRepository jobRepository;

    @AfterEach
    void stopWorkers() {
        queueCommands.workerStop();
    }

    @BeforeEach
    void cleanSlate() {
        jobRepository.deleteAll();
    }

    @Test
    void basicJob_completesSuccessfully() {
        String jobId = "success-" + UUID.randomUUID();
        queueCommands.enqueue("{\"id\":\"" + jobId + "\",\"command\":\"echo hello\"}");

        queueCommands.workerStart(1);

        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    Job job = jobRepository.findById(jobId).orElseThrow();
                    assertEquals(JobState.COMPLETED, job.getState());
                });
    }

    @Test
    void failingJob_retriesWithBackoffThenMovesToDlq() {
        String jobId = "fail-" + UUID.randomUUID();
        // maxRetries=1 keeps the test fast: 1 initial attempt + 1 retry, then DEAD
        queueCommands.enqueue("{\"id\":\"" + jobId + "\",\"command\":\"exit 1\",\"maxRetries\":1}");

        queueCommands.workerStart(1);

        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    Job job = jobRepository.findById(jobId).orElseThrow();
                    assertEquals(JobState.DEAD, job.getState());
                    assertEquals(2, job.getAttempts()); // 1 initial + 1 retry
                });

        // Confirm it now shows up in the DLQ listing
        String dlqOutput = queueCommands.dlqList();
        assertTrue(dlqOutput.contains(jobId));
    }

    @Test
    void dlqRetry_movesDeadJobBackToPending() {
        String jobId = "requeue-" + UUID.randomUUID();
        queueCommands.enqueue("{\"id\":\"" + jobId + "\",\"command\":\"exit 1\",\"maxRetries\":0}");

        queueCommands.workerStart(1);

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() ->
                        assertEquals(JobState.DEAD, jobRepository.findById(jobId).orElseThrow().getState()));

        queueCommands.workerStop();
        queueCommands.dlqRetry(jobId);

        Job requeued = jobRepository.findById(jobId).orElseThrow();
        assertEquals(JobState.PENDING, requeued.getState());
        assertEquals(0, requeued.getAttempts());
    }

    @Test
    void invalidCommand_failsGracefullyWithoutCrashingWorker() {
        String jobId = "invalid-" + UUID.randomUUID();
        queueCommands.enqueue("{\"id\":\"" + jobId + "\",\"command\":\"totallyFakeCommandXYZ\",\"maxRetries\":0}");

        queueCommands.workerStart(1);

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    Job job = jobRepository.findById(jobId).orElseThrow();
                    // Should end up DEAD (or FAILED-then-DEAD), never leave the worker stuck
                    assertEquals(JobState.DEAD, job.getState());
                });
    }

    @Test
    void multipleWorkers_processJobsWithoutDuplicateExecution() {
        List<String> jobIds = List.of(
                "multi-" + UUID.randomUUID(),
                "multi-" + UUID.randomUUID(),
                "multi-" + UUID.randomUUID(),
                "multi-" + UUID.randomUUID()
        );

        for (String id : jobIds) {
            queueCommands.enqueue("{\"id\":\"" + id + "\",\"command\":\"echo hi\"}");
        }

        queueCommands.workerStart(3);

        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    long completedCount = jobIds.stream()
                            .map(id -> jobRepository.findById(id).orElseThrow())
                            .filter(j -> j.getState() == JobState.COMPLETED)
                            .count();
                    assertEquals(jobIds.size(), completedCount);
                });

        // Every job should have exactly 0 retries (each ran exactly once, no duplicates)
        for (String id : jobIds) {
            Job job = jobRepository.findById(id).orElseThrow();
            assertEquals(0, job.getAttempts(), "Job " + id + " should not have been retried/duplicated");
        }
    }

    @Test
    void status_reflectsAccurateJobCounts() {
        String pendingJobId = "status-" + UUID.randomUUID();
        queueCommands.enqueue("{\"id\":\"" + pendingJobId + "\",\"command\":\"echo queued\"}");

        // Don't start workers - job should stay PENDING
        String statusOutput = queueCommands.status();

        assertTrue(statusOutput.contains("PENDING"));
    }
}
