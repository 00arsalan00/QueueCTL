package com.queuectl.service;

import com.queuectl.dto.JobExecutionResult;
import com.queuectl.entity.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkerService {

    private final JobAcquisitionService jobAcquisitionService;
    private final JobRunner jobRunner;
    private final JobFinalizer jobFinalizer;
    private final ThreadPoolTaskExecutor executor;
    private final ConfigurationService configurationService;
    private final AtomicBoolean localRunning = new AtomicBoolean(false);

    public void startWorkers(int count) {
        executor.setCorePoolSize(count);
        executor.setMaxPoolSize(count);
        executor.initialize();
        localRunning.set(true);
        configurationService.setWorkerRunning(true);

        new Thread(this::pollLoop).start();
    }

    public void stopWorkers() {
        localRunning.set(false);
        configurationService.setWorkerRunning(false);
    }

    private void pollLoop() {
        while (localRunning.get() && configurationService.isWorkerRunning()) {
            try {
                int activeCount = executor.getActiveCount();
                int maxPoolSize = executor.getMaxPoolSize();

                if (activeCount < maxPoolSize) {
                    jobAcquisitionService.acquireNextJob()
                            .ifPresent(job -> executor.submit(() -> executeAndFinalize(job)));
                }

                Thread.sleep(configurationService.getPollingIntervalMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in worker polling loop", e);
            }
        }
    }

    private void executeAndFinalize(Job job) {
        try {
            int timeout = job.getTimeout() > 0 ? job.getTimeout() : configurationService.getTimeoutSeconds();
            JobExecutionResult result = jobRunner.executeJob(job.getCommand(), timeout);
            jobFinalizer.finalizeJob(job.getId(), result);
        } catch (Exception e) {
            log.error("Fatal error running job " + job.getId(), e);
        }
    }
}
