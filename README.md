# Workflow Execution Service

A minimal, high-performance workflow execution service built using **Spring Boot 3.x** and **Java 21**. It supports defining workflows, executing them asynchronously, tracking live execution progress, and storing execution histories in memory. 

This project also includes a **live glassmorphic web dashboard** that displays executions dynamically as they transition through steps in real-time.

---

## Architecture Overview

The application follows a clean layered design:

```
[ Web Dashboard / REST Clients ]
               │
               ▼ (REST API)
     [ WorkflowController ]
       /                \
      ▼                  ▼
[ Repositories ]    [ WorkflowEngine ]
 (In-Memory Map)    (Virtual Threads Async Orchestrator)
                         │
                         ▼ (Dynamic Bean Lookup)
                    [ StepExecutors ]
               (validate, approve, execute)
```

1. **REST Controller (`WorkflowController`)**: Exposes REST endpoints to create workflows, trigger executions, poll execution status, and view run history.
2. **Workflow Engine (`WorkflowEngine`)**: Coordinates workflow runs. Once an execution is started, the engine schedules an asynchronous task using **Java 21 Virtual Threads**, ensuring maximum scalability.
3. **Step Executors (`StepExecutor`)**: An extensible interface for executing step logic. Concrete implementations (like `ValidateStepExecutor`, `ApproveStepExecutor`, and `ExecuteStepExecutor`) are registered as Spring Beans with names matching their step configuration names.
4. **Execution Context**: An in-memory key-value store per execution. The output of completed steps is stored in the context under the step's name (e.g., `context.put("validate", validateOutput)`), allowing subsequent steps to query and act on prior outputs.
5. **Repositories (`WorkflowRepository`, `WorkflowExecutionRepository`)**: Maintain thread-safe in-memory stores using `ConcurrentHashMap`.

---

## REST API Documentation

### 1. Register a Workflow
* **Endpoint**: `POST /api/workflows`
* **Request Body**:
  ```json
  {
    "workflowId": "contract-review",
    "steps": [
      {
        "name": "validate",
        "input": {
          "contractId": "123"
        }
      },
      {
        "name": "approve"
      },
      {
        "name": "execute"
      }
    ]
  }
  ```
* **Response**: `201 Created` with the registered workflow object.

### 2. List Workflows
* **Endpoint**: `GET /api/workflows`
* **Response**: `200 OK` list of all registered definitions.

### 3. Start Workflow Execution
* **Endpoint**: `POST /api/workflows/{workflowId}/execute`
* **Response**: `201 Created` with the initial execution state:
  ```json
  {
    "executionId": "b182ea6b-12d8-4c8d-b0df-d2c67c5f8382",
    "workflowId": "contract-review",
    "status": "RUNNING",
    "currentStep": 1,
    "steps": [
      { "name": "validate", "status": "PENDING", "input": { "contractId": "123" } },
      { "name": "approve", "status": "PENDING", "input": {} },
      { "name": "execute", "status": "PENDING", "input": {} }
    ]
  }
  ```

### 4. Track Execution State / History
* **Endpoint**: `GET /api/executions/{executionId}`
* **Response**: `200 OK` with detailed progress:
  ```json
  {
    "executionId": "b182ea6b-12d8-4c8d-b0df-d2c67c5f8382",
    "status": "COMPLETED",
    "currentStep": 3,
    "steps": [
      {
        "name": "validate",
        "status": "COMPLETED",
        "input": { "contractId": "123" },
        "output": { "valid": true },
        "startedAt": "2026-06-17T01:00:00Z",
        "completedAt": "2026-06-17T01:00:01.5Z"
      },
      ...
    ],
    "context": {
      "validate": { "valid": true },
      "approve": { "approvedBy": "system" },
      "execute": { "result": "success" }
    }
  }
  ```

---

## Key Assumptions

1. **Sequential Flow**: Workflows are linear and execute in the index order of the steps defined.
2. **Context Propagation**: Steps are interdependent; if a step depends on previous inputs, it checks the cumulative `context` object keyed by the previous step names (e.g., the `approve` step checks `context.get("validate")`).
3. **Execution Mode**: Executions run asynchronously. Clients obtain an execution ID immediately and are expected to poll the status to track completion.

---

## Design Tradeoffs Made

1. **In-Memory H2 Database with JPA**: We transitioned from basic `ConcurrentHashMap` caches to an H2 database using Spring Data JPA.
   * *Tradeoff*: H2 runs in-memory (`jdbc:h2:mem:workflowdb`), requiring zero installation or configuration overhead (no Docker or external db required) while enabling correct database table mappings, JPA entity relationships, and transaction isolation. Although data is cleared upon JVM termination, this design is readily swappable to durable database engines.
2. **Virtual Threads for Concurrency**: We used Java 21's new Virtual Thread Executor (`Executors.newVirtualThreadPerTaskExecutor()`).
   * *Tradeoff*: Running workflow steps asynchronously usually consumes platform threads that sleep during delays. Virtual threads are extremely lightweight, allowing thousands of concurrent workflow runs to sleep without exhausting the OS thread pool.
3. **Simulated Latency for Tracking Visibility**: We introduced a `1500ms` sleep per step inside the workflow runner.
   * *Tradeoff*: In a pure automation microservice, steps complete in milliseconds. We added this delay so that the real-time web dashboard's visual progress tracker (PENDING -> RUNNING -> COMPLETED) is satisfying and observable.

---

## What We Would Improve with an Additional Week

1. **Persistent Relational DB / migrations**: Move from an in-memory database to a persistent external instance (e.g. PostgreSQL) and integrate database schema migration tooling (e.g. Flyway or Liquibase).
2. **Real-time Push (WebSockets/SSE)**: Replace frontend polling with Server-Sent Events (SSE) or WebSockets to update the dashboard immediately upon state transitions.
3. **Branching and Conditionals**: Enhance the workflow definition structure to support conditional branches (`if`/`else`), loops, and parallel step execution.
4. **Dynamic Expression Evaluation**: Integrate Spring Expression Language (SpEL) to map input parameters dynamically (e.g., mapping `${validate.output.valid}` to a step input).
5. **Interactive Pause/Resume (Human-in-the-loop)**: Allow steps to request a `SUSPENDED` status, pausing the execution until an external user submits approval/input via a REST API to resume it.

---

## Instructions for Running Locally

### Prerequisites
* **Java 21** or later installed.
* **Maven 3.8+** installed.

### Build the Application
Run the Maven package command to download dependencies, compile code, run tests, and package the JAR:
```bash
mvn clean package
```

### Run the Application
Start the Spring Boot server:
```bash
mvn spring-boot:run
```

### Access the Web Dashboard
Open your browser and navigate to:
```
http://localhost:8080/
```
The page will load in dark mode, pre-populated with the `contract-review` workflow definition. Click **Execute Workflow** to watch the visual graph run in real-time.
