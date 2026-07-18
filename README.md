# QueueCTL

**QueueCTL** is a CLI-based background job queue system built using Spring Boot, Spring Shell, and SQLite. It provides persistent background job management, concurrency control, automatic exponential backoff retries, and a Dead Letter Queue (DLQ) for permanently failed tasks.

---

## Technology Stack
- **Runtime:** Java 17
- **Framework:** Spring Boot 3.3.0
- **CLI Engine:** Spring Shell
- **Database:** SQLite (Embedded & Persistent)
- **ORM / Persistence:** Spring Data JPA / Hibernate
- **Build Tool:** Maven

---

## ️ Architecture Overview

### 1. Job Lifecycle State Machine
Jobs transition through states dynamically:
- **`PENDING`**: Waiting to run (where current time $\ge$ scheduled `runAt` time).
- **`PROCESSING`**: Claimed and actively executing inside a worker thread.
- **`COMPLETED`**: Successfully run (command exited with code `0`).
- **`DEAD`**: Failed repeatedly and moved permanently to the Dead Letter Queue (DLQ).

### 2. Concurrency & Overlap Prevention
To support multiple workers with zero duplicate processing:
- **Pessimistic write locking** is applied when querying the next available job in SQLite.
- The fetching operation runs in an isolated transaction (`Propagation.REQUIRES_NEW`) to claiming a job (`PENDING` $\rightarrow$ `PROCESSING`) and committing the change immediately.
- This releases the database lock before the command starts executing, ensuring separate workers can query other jobs instantly.

### 3. Graceful Shutdown & Multi-Process Coordination
Because running `queuectl worker stop` executes in a separate terminal process from `queuectl worker start`, state is coordinated via the SQLite database:
- Running `worker stop` sets `worker.running = false` in a configuration table.
- Worker threads poll this state. If the flag goes `false`, the polling loop exits cleanly.
- Workers currently executing tasks finish their active commands and persist results before shutting down.

### 4. Dynamic Live Configurations
Parameters (like `max-retries`, `backoff-base`, and `timeout-seconds`) reside in the database:
- Dynamic updates via `config set` are implemented across separate CLI sessions.
- In-flight enqueues automatically fall back to these configurations when properties are omitted from client payloads.

---

## CLI Commands & Examples

### Enqueue Job
Adds a new job. `priority`, `maxRetries`, and `timeout` are optional.
```bash
enqueue '{"id":"job1","command":"echo Hello World"}'
# Output: Job enqueued successfully: job1
```

### Worker Management
Start one or more background threads:
```bash
worker start --count 3
# Output: Started 3 workers in the background.
```

Stop workers gracefully:
```bash
worker stop
# Output: Graceful stop signal sent to workers.
```

### Queue Inspect
View status overview:
```bash
status
# Output:
# --- Queue Status ---
# PENDING: 0
# PROCESSING: 0
# COMPLETED: 1
# DEAD: 0
```

List jobs matching a state:
```bash
list --state COMPLETED
# Output: [COMPLETED] ID: job1 | Command: echo Hello World
```

### Dead Letter Queue (DLQ)
List dead jobs:
```bash
dlq list
# Output: ID: job2 | Command: invalidcmd | Attempts: 4 | Updated: 2026-07-18T10:00:00
```

Retry dead jobs (bulk or selective):
```bash
dlq retry
# Output: Moved 1 dead jobs back to PENDING.

dlq retry job2
# Output: Job job2 moved back to PENDING.
```

### Configurations
Live update database configuration settings:
```bash
config set max-retries 5
# Output: Configuration updated: max-retries = 5
```

### Metrics & Statistics (Bonus)
Show failure rates, success ratios, and average execution time of processed tasks:
```bash
stats
# Output:
# --- Queue Statistics ---
# Total Jobs: 2
# Completed: 1
# Dead (DLQ): 1
# Success Ratio: 50.00%
# Average Execution Time: 120.00 ms
```

---

## Web Dashboard (Bonus)
A REST dashboard endpoint is hosted on port `8086` while the application runs:
- Endpoint: `http://localhost:8086/jobs`
- Resolves: Live JSON listing of all jobs, their durations, configurations, execution counts, and logged output results.

---

## Setup & Execution

1. Build the project:
   ```bash
   mvn clean install
   ```
2. Start the interactive Spring Boot shell:
   ```bash
   mvn spring-boot:run
   ```
3. Type `help` in the shell prompt to see listing of all operations.

---

## Testing Instructions
1. Run `worker start --count 2` to start the background engine.
2. Queue up a successful task: `enqueue '{"command":"echo success"}'`. Check `status` to ensure it transitions to `COMPLETED`.
3. Queue up a failing command to check retry and DLQ logic: `enqueue '{"maxRetries": 3, "command": "exit 1"}'`.
4. Observe details in console output logging exponential delay. Running `dlq list` will display the job after 4 trials (1 initial + 3 retries).
