package com.solaris.backend.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlertMonitoringJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertMonitoringJob.class);

    private final AlertMonitoringService monitoringService;

    @Scheduled(fixedDelayString = "${solaris.monitoring.fixed-delay:60000}")
    public void monitor() {
        try {
            monitoringService.evaluate();
        } catch (RuntimeException exception) {
            LOGGER.error("Solar alert monitoring failed", exception);
        }
    }
}
