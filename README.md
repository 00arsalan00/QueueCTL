# QueueCTL

## Project Overview
**QueueCTL** is a CLI-based background job queue system designed to manage background jobs with worker processes. It handles retries using exponential backoff and maintains a Dead Letter Queue (DLQ) for permanently failed jobs.

## Description
This system implements a minimal, production-grade job queue that supports:
- Enqueuing and managing background jobs.
- Running multiple worker processes in parallel.
- Automatic retries with exponential backoff.
- Moving jobs to a Dead Letter Queue after exhausting retries.
- Persistent job storage using SQLite to survive restarts.
- Full control via a clean CLI interface.

## Tech Stack
- **Framework:** Spring Boot 3.3.0
- **Language:** Java 17
- **Database:** SQLite (Embedded & Persistent)
- **CLI Engine:** Spring Shell
- **Persistence:** Spring Data JPA / Hibernate
- **Build Tool:** Maven

##  How to Use (Preview)
```bash
# Build the project
mvn clean install

# Add a new job to the queue
queuectl enqueue '{"id":"job1","command":"echo hello"}'

# Start 3 worker processes
queuectl worker start --count 3

# Check system status
queuectl status
```

## Phase 1: The Domain & Persistence Layer

In this phase, we are building the **Source of Truth** for the entire system. Before we can run jobs, we must define exactly what a "Job" is and how it is saved.

## Phase 2: The Execution Engine

In this phase, we move from Storage to Action. We are building the brain that knows how to execute shell commands and the management system that runs multiple workers in parallel




