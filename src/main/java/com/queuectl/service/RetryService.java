package com.queuectl.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RetryService {

    private final ConfigurationService configurationService;

    public LocalDateTime calculateNextRunTime(int attempts) {
        long delaySeconds = (long) Math.pow(configurationService.getBackoffBase(), attempts);
        return LocalDateTime.now().plusSeconds(delaySeconds);
    }
}
