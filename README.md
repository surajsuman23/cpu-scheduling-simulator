# CPU Scheduling Algorithm Simulator

A Java command-line simulator comparing First Come First Served (FCFS), non-preemptive Shortest Job First (SJF), and Round Robin scheduling. Reports per-process completion, turnaround and waiting times, averages, total turnaround, and an execution timeline.

## About this version

Rebuilt portfolio implementation based on an earlier project. This repository contains the current code, tests and documentation. Reported results apply to this version. References and data sources are listed in `SOURCES.md`.

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
