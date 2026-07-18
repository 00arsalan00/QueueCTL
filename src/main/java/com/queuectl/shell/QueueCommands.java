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
    private final ObjectMapper objectMapper;
    private final com.queuectl.service.ConfigurationService configurationService;

    @ShellMethod(key = "enqueue", value = "Add a new job to the queue using JSON")
    public String enqueue(String json) {
        try {
            EnqueueRequest req = objectMapper.readValue(json, EnqueueRequest.class);

            Job job = Job.builder()
                    .id(req.getId())
                    .command(req.getCommand())
                    .state(JobState.PENDING)
                    .priority(req.getPriority())
                    .maxRetries(req.getMaxRetries())
                    .timeout(req.getTimeout())
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

    @ShellMethod(key = "dlq retry", value = "Retry a job from the Dead Letter Queue")
    public String dlqRetry(String jobId) {
        return jobRepository.findById(jobId).map(job -> {
            if (job.getState() != JobState.DEAD)
                return "Job is not in DLQ.";
            job.setState(JobState.PENDING);
            job.setAttempts(0);
            job.setRunAt(LocalDateTime.now());
            jobRepository.save(job);
            return "Job " + jobId + " moved back to PENDING.";
        }).orElse("Job not found.");
    }
}