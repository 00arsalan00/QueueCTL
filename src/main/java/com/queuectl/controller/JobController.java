package com.queuectl.controller;

import com.queuectl.entity.Job;
import com.queuectl.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class JobController {

    private final JobRepository jobRepository;

    @GetMapping("/jobs")
    public List<Job> getJobs() {
        return jobRepository.findAll();
    }
}
