package com.queuectl.service;

import com.queuectl.entity.Configuration;
import com.queuectl.repository.ConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfigurationService {

    private final ConfigurationRepository configurationRepository;

    @Value("${queue.defaults.max-retries:3}")
    private int defaultMaxRetries;

    @Value("${queue.defaults.backoff-base:2}")
    private int defaultBackoffBase;

    @Value("${queue.defaults.timeout-seconds:60}")
    private int defaultTimeoutSeconds;

    @Value("${queue.polling.interval-ms:1000}")
    private int defaultPollingIntervalMs;

    public int getMaxRetries() {
        return configurationRepository.findById("max-retries")
                .map(c -> Integer.parseInt(c.getValue()))
                .orElse(defaultMaxRetries);
    }

    public int getBackoffBase() {
        return configurationRepository.findById("backoff-base")
                .map(c -> Integer.parseInt(c.getValue()))
                .orElse(defaultBackoffBase);
    }

    public int getTimeoutSeconds() {
        return configurationRepository.findById("timeout-seconds")
                .map(c -> Integer.parseInt(c.getValue()))
                .orElse(defaultTimeoutSeconds);
    }

    public int getPollingIntervalMs() {
        return configurationRepository.findById("polling-interval")
                .map(c -> Integer.parseInt(c.getValue()))
                .orElse(defaultPollingIntervalMs);
    }

    public boolean isWorkerRunning() {
        return configurationRepository.findById("worker.running")
                .map(c -> Boolean.parseBoolean(c.getValue()))
                .orElse(true);
    }

    public void setWorkerRunning(boolean running) {
        Configuration config = configurationRepository.findById("worker.running")
                .orElseGet(() -> Configuration.builder().key("worker.running").build());
        config.setValue(String.valueOf(running));
        configurationRepository.save(config);
    }

    public void set(String key, String value) {
        Configuration config = configurationRepository.findById(key)
                .orElseGet(() -> Configuration.builder().key(key).build());
        config.setValue(value);
        configurationRepository.save(config);
    }
}
