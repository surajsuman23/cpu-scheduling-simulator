# Verification

Tested locally on 2026-09-22. These checks cover the current implementation.

Java 17: compilation passed. Reference completion times, idle gaps, ties and invalid inputs passed; 100 random workloads across three algorithms passed time-conservation and arrival-time checks. The provided CSV was run through all three algorithms.

## Online interface verification — 2026-09-23

The JavaScript scheduler matched Java process metrics and complete timelines in 105 algorithm/workload comparisons. Invalid and duplicate process input checks passed.
