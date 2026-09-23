# CPU Scheduling Algorithm Simulator

**[Open interactive demo](https://suraj-cpu-simulator.m16labs-0951.chatgpt.site)**


A Java command-line simulator comparing First Come First Served (FCFS), non-preemptive Shortest Job First (SJF), and Round Robin scheduling. Reports per-process completion, turnaround and waiting times, averages, total turnaround, and an execution timeline.

## Implementation

This repository includes the command-line implementation, an interactive browser demo, tests and documentation. Reported results apply to the documented implementation and runtime. References and data sources are listed in `SOURCES.md`.

## Build and run

Requires JDK 17 or newer. No external Java libraries or build tool are required. From this project folder:

```sh
mkdir -p build
javac -d build Scheduler.java SchedulerTest.java
java -cp build Scheduler processes.csv all 2
java -cp build SchedulerTest
```

Use `fcfs`, `sjf`, or `rr` instead of `all` to select one algorithm. The last argument is the Round Robin quantum; it defaults to 2.

## Input and assumptions

```csv
id,arrival,burst
P1,0,5
P2,1,3
P3,2,1
```

Times are nonnegative integer units, and bursts must be positive. IDs must be unique and contain only letters, digits, underscores or hyphens; `IDLE` is reserved. The simulator models one CPU, one burst per process, no I/O blocking and zero context-switch cost.

- FCFS preserves arrival order.
- SJF chooses the shortest burst among available processes without preemption. Ties use arrival order, then original CSV order.
- Round Robin enqueues arrivals at a quantum boundary before requeuing the preempted process.
- Idle intervals are explicitly recorded.
- Turnaround = completion - arrival; waiting = turnaround - burst.

Inputs that can overflow aggregate time arithmetic are rejected. A one-million-slice limit prevents impractical Round Robin timeline allocation.

## Example to verify by hand

For the included input, completion times in P1/P2/P3 order are:

| Algorithm | Completion times |
| --- | --- |
| FCFS | 5, 8, 9 |
| SJF | 5, 9, 6 |
| Round Robin, quantum 2 | 9, 8, 5 |

## Tests

`SchedulerTest` checks the reference schedules, idle gaps, deterministic ties, invalid quantum and duplicate IDs. It also checks 100 seeded random workloads under all three algorithms for CPU time conservation, per-process execution totals, nonnegative waiting time and no execution before arrival.

This is an algorithm simulator, not an operating-system kernel scheduler or a benchmark of actual hardware performance.

## Browser interface

The `docs/` folder contains a standalone browser interface. To run it locally from the repository root:

```sh
npm ci
npm start
```

Open http://localhost:8211. Serve these files over HTTP or HTTPS; opening `index.html` directly as a file does not support the worker and module imports.

The online interface implements the same scheduling rules in JavaScript. Its process metrics and execution timelines were compared with `Scheduler.java` across 35 workloads and all three algorithms (105 comparisons). The original Java CLI remains the reference implementation. Edit the process table or choose a preset, adjust the Round Robin quantum, and compare algorithms. The interface includes a proportional Gantt timeline, exact execution intervals, per-process metrics, and a full JSON download. See `OPERATIONS.md` for browser checks and monitoring.

The public demo is hosted independently of this computer. The project can also be served from the `docs/` directory on a static host.

## Independent application

This app has its own deployment and source repository. It has no shared navigation or runtime dependency on the other portfolio projects.

## Independent server

Run `npm ci` and `npm start` to serve frontend and API together on http://127.0.0.1:8211. Use `npm run build` for the Worker deployment bundle. The Node entry point is `server/local.mjs`; the hosted Worker entry point is `server/worker.mjs`. `npm run test:api` verifies its contracts. A non-root Dockerfile is included.

The browser now submits workloads to `POST /api/v1/simulations`. The Java CLI remains the reference algorithm implementation. The hosted server calculates the result and does not save workloads.
