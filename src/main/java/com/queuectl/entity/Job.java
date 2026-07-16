package com.queuectl.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String command;

    @Enumerated(EnumType.STRING)
    private JobState state;

    private int priority;
    private int attempts;

    @Column(name = "max_retries")
    private int maxRetries;

    private int timeout;

    @Column(name = "run_at")
    private LocalDateTime runAt;

    @Lob
    @Column(name = "log_output")
    private String logOutput;

    @Column(name = "execution_time")
    private Long executionTimeMillis;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if(runAt == null) runAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


}
