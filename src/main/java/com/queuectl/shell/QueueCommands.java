package com.queuectl.shell;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queuectl.dto.EnqueueRequest;
import com.queuectl.entity.Job;
import com.queuectl.entity.JobState;
import com.queuectl.repository.JobRepository;
import com.queuectl.service.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ShellComponent
@RequiredArgsConstructor
public class QueueCommands {

    private final JobRepository jobRepository;
    private final WorkerService workerService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final com.queuectl.service.ConfigurationService configurationService;

    @ShellMethod(key = "enqueue", value = "Add a new job to the queue using JSON")
    public String enqueue(String json) {
        try {
            EnqueueRequest req = objectMapper.readValue(json, EnqueueRequest.class);

            int priority = req.getPriority() != null ? req.getPriority() : 0;
            int maxRetries = req.getMaxRetries() != null ? req.getMaxRetries() : configurationService.getMaxRetries();
            int timeout = req.getTimeout() != null ? req.getTimeout() : configurationService.getTimeoutSeconds();

            Job job = Job.builder()
                    .id(req.getId())
                    .command(req.getCommand())
                    .state(JobState.PENDING)
                    .priority(priority)
                    .maxRetries(maxRetries)
                    .timeout(timeout)
                    .attempts(0)
                    .runAt(LocalDateTime.now())
                    .build();

            jobRepository.save(job);
            return "Job enqueued successfully: " + job.getId();
        } catch (Exception e) {
            return "Error parsing JSON: " + e.getMessage();
        }
    }

    @ShellMethod(key = "worker start", value = "Start one or more workers")
    public String workerStart(@ShellOption(defaultValue = "1") int count) {
        if (count <= 0)
            return "Worker count must be greater than 0";
        workerService.startWorkers(count);
        return "Started " + count + " workers in the background.";
    }

    @ShellMethod(key = "worker stop", value = "Stop running workers gracefully")
    public String workerStop() {
        workerService.stopWorkers();
        return "Graceful stop signal sent to workers.";
    }

    @ShellMethod(key = "config set", value = "Manage configuration parameters")
    public String configSet(String key, String value) {
        try {
            configurationService.set(key, value);
            return "Configuration updated: " + key + " = " + value;
        } catch (Exception e) {
            return "Error updating configuration: " + e.getMessage();
        }
    }

    @ShellMethod(key = "status", value = "Show summary of all job states")
    public String status() {
        List<Job> allJobs = jobRepository.findAll();
        Map<JobState, Long> stats = allJobs.stream()
                .collect(Collectors.groupingBy(Job::getState, Collectors.counting()));

        StringBuilder sb = new StringBuilder("--- Queue Status ---\n");
        for (JobState state : JobState.values()) {
            sb.append(state).append(": ").append(stats.getOrDefault(state, 0L)).append("\n");
        }
        return sb.toString();
    }

    @ShellMethod(key = "list", value = "List jobs by state")
    public String list(@ShellOption(defaultValue = "PENDING") JobState state) {
        List<Job> jobs = jobRepository.findByState(state);
        if (jobs.isEmpty())
            return "No jobs found in state: " + state;

        return jobs.stream()
                .map(j -> String.format("[%s] ID: %s | Command: %s", j.getState(), j.getId(), j.getCommand()))
                .collect(Collectors.joining("\n"));
    }

    @ShellMethod(key = "dlq list", value = "List all dead letter queue jobs")
    public String dlqList() {
        List<Job> deadJobs = jobRepository.findByState(JobState.DEAD);
        if (deadJobs.isEmpty()) {
            return "DLQ is empty.";
        }
        return deadJobs.stream()
                .map(j -> String.format("ID: %s | Command: %s | Attempts: %d | Updated: %s",
                        j.getId(), j.getCommand(), j.getAttempts(), j.getUpdatedAt()))
                .collect(Collectors.joining("\n"));
    }

    @ShellMethod(key = "dlq retry", value = "Retry one or all dead letter queue jobs")
    public String dlqRetry(
            @ShellOption(defaultValue = org.springframework.shell.standard.ShellOption.NULL) String jobId) {
        if (jobId == null || jobId.trim().isEmpty()) {
            List<Job> deadJobs = jobRepository.findByState(JobState.DEAD);
            if (deadJobs.isEmpty()) {
                return "No dead jobs found in DLQ.";
            }
            for (Job job : deadJobs) {
                job.setState(JobState.PENDING);
                job.setAttempts(0);
                job.setRunAt(LocalDateTime.now());
                jobRepository.save(job);
            }
            return "Moved " + deadJobs.size() + " dead jobs back to PENDING.";
        }

        return jobRepository.findById(jobId).map(job -> {
            if (job.getState() != JobState.DEAD) {
                return "Job is not in DLQ.";
            }
            job.setState(JobState.PENDING);
            job.setAttempts(0);
            job.setRunAt(LocalDateTime.now());
            jobRepository.save(job);
            return "Job " + jobId + " moved back to PENDING.";
        }).orElse("Job not found: " + jobId);
    }

    @ShellMethod(key = "stats", value = "Show execution statistics and metrics")
    public String stats() {
        List<Job> allJobs = jobRepository.findAll();
        long total = allJobs.size();
        long completed = allJobs.stream().filter(j -> j.getState() == JobState.COMPLETED).count();
        long failedDead = allJobs.stream().filter(j -> j.getState() == JobState.DEAD).count();

        double successRatio = total == 0 ? 0.0 : (double) completed / total * 100.0;

        double avgExecutionTime = allJobs.stream()
                .filter(j -> j.getExecutionTimeMillis() != null)
                .mapToLong(Job::getExecutionTimeMillis)
                .average()
                .orElse(0.0);

        return String.format("--- Queue Statistics ---\n" +
                "Total Jobs: %d\n" +
                "Completed: %d\n" +
                "Dead (DLQ): %d\n" +
                "Success Ratio: %.2f%%\n" +
                "Average Execution Time: %.2f ms",
                total, completed, failedDead, successRatio, avgExecutionTime);
    }
}