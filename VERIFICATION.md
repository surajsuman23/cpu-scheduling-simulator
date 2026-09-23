# Verification

Tested locally on 2026-09-22. These checks cover the current implementation.

Java 17: compilation passed. Reference completion times, idle gaps, ties and invalid inputs passed; 100 random workloads across three algorithms passed time-conservation and arrival-time checks. The provided CSV was run through all three algorithms.

## Online interface verification — 2026-09-23

The JavaScript scheduler matched Java process metrics and complete timelines in 105 algorithm/workload comparisons. Invalid and duplicate process input checks passed.

## Scheduling lab release

[Browser verification run 35885050274](https://github.com/surajsuman23/cpu-scheduling-simulator/actions/runs/35885050274) passed for application commit `31f5b41`. The four cases cover Chromium, Firefox, WebKit and mobile WebKit: workload edits, Round Robin timeline selection, presets, invalid duplicate IDs, JSON downloads, narrow-screen layout and axe accessibility rules. This is automated flow coverage, not a manual assistive-technology audit.

The unchanged scheduling engine was also rechecked against Java across 105 algorithm/workload comparisons on 2026-09-23.
