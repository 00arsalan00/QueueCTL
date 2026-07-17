package com.queuectl.repository;

import com.queuectl.entity.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Job> findFirstByStateAndRunAtBeforeOrderByPriorityDescCreatedAtAsc(
            JobState state,
            LocalDateTime now
    );

    List<Job> findByStateAndUpdatedAtBefore(JobState jobState, LocalDateTime threshold);
}
