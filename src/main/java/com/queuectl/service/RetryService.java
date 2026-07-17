package com.queuectl.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class RetryService {

    @Value("${queue.retry.backoff-base:2}")
    private int backoffBase;

    public LocalDateTime calculateNextRunTime(int attempts) {
        long delaySeconds = (long) Math.pow(backoffBase, attempts);
        return LocalDateTime.now().plusSeconds(delaySeconds);
    }


}
