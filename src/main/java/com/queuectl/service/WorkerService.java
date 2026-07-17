package com.queuectl.service;


import com.queuectl.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkerService {
    private final JobRepository jobRepository;
    private final JobRunner jobRunner;
    private final ThreadPoolTaskExecutor executor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public void startWorkers(int count){
        executor.setCorePoolSize(count);
        executor.setMaxPoolSize(count);
        running.set(true);

        log.debug("Starting {} workers...");


        new Thread(()->{
            while(running.get()){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }
}
