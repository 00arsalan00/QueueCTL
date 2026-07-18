package com.queuectl.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class RetryServiceTest {

    private ConfigurationService configurationService;
    private RetryService retryService;

    @BeforeEach
    void setUp() {
        configurationService = Mockito.mock(ConfigurationService.class);
        retryService = new RetryService(configurationService);
    }

    @Test
    void firstAttempt_withBaseTwo_delaysAboutOneSecond() {

        when(configurationService.getBackoffBase()).thenReturn(2);

        LocalDateTime before = LocalDateTime.now();
        LocalDateTime nextRun = retryService.calculateNextRunTime(0);

        long secondsUntilRun = Duration.between(before, nextRun).getSeconds();
        assertTrue(secondsUntilRun >= 1 && secondsUntilRun <= 2,
                "Expected ~1s delay for attempts=0, base=2, got " + secondsUntilRun + "s");
    }

    @Test
    void thirdAttempt_withBaseTwo_delaysAboutEightSeconds() {

        when(configurationService.getBackoffBase()).thenReturn(2);

        LocalDateTime before = LocalDateTime.now();
        LocalDateTime nextRun = retryService.calculateNextRunTime(3);

        long secondsUntilRun = Duration.between(before, nextRun).getSeconds();
        assertTrue(secondsUntilRun >= 7 && secondsUntilRun <= 9,
                "Expected ~8s delay for attempts=3, base=2, got " + secondsUntilRun + "s");
    }

    @Test
    void delayGrowsExponentially_notLinearly() {
        when(configurationService.getBackoffBase()).thenReturn(3);

        LocalDateTime base = LocalDateTime.now();
        long delayAttempt1 = Duration.between(base, retryService.calculateNextRunTime(1)).getSeconds();
        long delayAttempt2 = Duration.between(base, retryService.calculateNextRunTime(2)).getSeconds();

        assertTrue(delayAttempt2 > delayAttempt1 * 2,
                "Backoff should grow exponentially: attempt1=" + delayAttempt1 + "s, attempt2=" + delayAttempt2 + "s");
    }
}
