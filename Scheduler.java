import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Single CPU, one burst per process, zero context-switch cost. Java 17+. */
public final class Scheduler {
    public record Process(String id, long arrival, long burst) {
        public Process {
            if (id == null || id.isBlank() || arrival < 0 || burst <= 0)
                throw new IllegalArgumentException("Nonempty id, nonnegative arrival and positive burst required.");
        }
    }
    public record Slice(String id, long start, long end) {}
    public record Metric(String id, long arrival, long burst, long completion, long turnaround, long waiting) {}
    public record Result(List<Slice> timeline, List<Metric> metrics) {
        public double averageWaiting() { return metrics.stream().mapToLong(Metric::waiting).average().orElse(0); }
        public double averageTurnaround() { return metrics.stream().mapToLong(Metric::turnaround).average().orElse(0); }
        public long totalTurnaround() { return metrics.stream().mapToLong(Metric::turnaround).sum(); }
    }

    private static List<Process> validate(List<Process> input) {
        if (input == null || input.isEmpty()) throw new IllegalArgumentException("At least one process required.");
        Set<String> ids = new HashSet<>();
        long upperBound = 0, maxArrival = 0;
        for (Process p : input) {
            if (p == null || !ids.add(p.id())) throw new IllegalArgumentException("Process IDs must be unique.");
            upperBound = Math.addExact(upperBound, p.burst());
            maxArrival = Math.max(maxArrival, p.arrival());
        }
        // Bound aggregate metric sums as well as the clock.
        Math.multiplyExact(Math.addExact(upperBound, maxArrival), input.size());
        List<Process> sorted = new ArrayList<>(input);
        sorted.sort(Comparator.comparingLong(Process::arrival)); // stable ties preserve CSV order
        return sorted;
    }

    public static Result run(List<Process> input, String algorithm, long quantum) {
        List<Process> pending = validate(input);
        if (!Set.of("fcfs", "sjf", "rr").contains(algorithm))
            throw new IllegalArgumentException("Algorithm must be fcfs, sjf or rr.");
        if (algorithm.equals("rr") && quantum <= 0) throw new IllegalArgumentException("Quantum must be positive.");
        List<Slice> trace = new ArrayList<>();
        Map<String, Long> completed = new HashMap<>();
        long time = 0;
        if (algorithm.equals("rr")) {
            ArrayDeque<Process> ready = new ArrayDeque<>();
            Map<String, Long> remaining = new HashMap<>();
            for (Process p : pending) remaining.put(p.id(), p.burst());
            int next = 0;
            while (next < pending.size() || !ready.isEmpty()) {
                if (ready.isEmpty() && time < pending.get(next).arrival()) {
                    trace.add(new Slice("IDLE", time, pending.get(next).arrival()));
                    time = pending.get(next).arrival();
                }
                while (next < pending.size() && pending.get(next).arrival() <= time) ready.addLast(pending.get(next++));
                Process current = ready.removeFirst();
                long duration = Math.min(quantum, remaining.get(current.id()));
                if (trace.size() >= 1_000_000) throw new IllegalArgumentException("Timeline exceeds 1,000,000 slices; increase quantum.");
                trace.add(new Slice(current.id(), time, time + duration));
                time += duration;
                remaining.put(current.id(), remaining.get(current.id()) - duration);
                // Arrivals at a quantum boundary enter before the preempted process.
                while (next < pending.size() && pending.get(next).arrival() <= time) ready.addLast(pending.get(next++));
                if (remaining.get(current.id()) > 0) ready.addLast(current);
                else completed.put(current.id(), time);
            }
        } else {
            while (!pending.isEmpty()) {
                if (time < pending.get(0).arrival()) {
                    trace.add(new Slice("IDLE", time, pending.get(0).arrival()));
                    time = pending.get(0).arrival();
                }
                int index = 0;
                if (algorithm.equals("sjf")) {
                    for (int i = 1; i < pending.size() && pending.get(i).arrival() <= time; i++) {
                        if (pending.get(i).burst() < pending.get(index).burst()) index = i;
                    }
                }
                Process p = pending.remove(index);
                trace.add(new Slice(p.id(), time, time + p.burst()));
                time += p.burst();
                completed.put(p.id(), time);
            }
        }
        List<Metric> metrics = new ArrayList<>();
        for (Process p : input) {
            long end = completed.get(p.id()), turn = end - p.arrival();
            metrics.add(new Metric(p.id(), p.arrival(), p.burst(), end, turn, turn - p.burst()));
        }
        return new Result(List.copyOf(trace), List.copyOf(metrics));
    }

    public static List<Process> readCsv(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        if (lines.isEmpty() || !lines.get(0).trim().equals("id,arrival,burst"))
            throw new IllegalArgumentException("CSV header must be id,arrival,burst.");
        List<Process> result = new ArrayList<>();
        for (int line = 1; line < lines.size(); line++) {
            if (lines.get(line).isBlank()) continue;
            String[] fields = lines.get(line).split(",", -1);
            if (fields.length != 3) throw new IllegalArgumentException("Expected three columns at line " + (line + 1));
            String id = fields[0].trim();
            if (!id.matches("[A-Za-z0-9_-]+") || id.equals("IDLE"))
                throw new IllegalArgumentException("IDs must use letters, digits, underscores or hyphens and cannot be IDLE.");
            result.add(new Process(id, Long.parseLong(fields[1].trim()), Long.parseLong(fields[2].trim())));
        }
        validate(result);
        return result;
    }

    public static void main(String[] args) {
        try {
            if (args.length < 2 || args.length > 3)
                throw new IllegalArgumentException("Usage: java -cp build Scheduler data/processes.csv fcfs|sjf|rr|all [quantum]");
            List<Process> processes = readCsv(Path.of(args[0]));
            String algorithm = args[1].toLowerCase(Locale.ROOT);
            long quantum = args.length == 3 ? Long.parseLong(args[2]) : 2;
            for (String name : algorithm.equals("all") ? List.of("fcfs", "sjf", "rr") : List.of(algorithm)) {
                Result result = run(processes, name, quantum);
                System.out.println("\nAlgorithm: " + name + (name.equals("rr") ? ", quantum=" + quantum : ""));
                System.out.println("id,arrival,burst,completion,turnaround,waiting");
                for (Metric m : result.metrics()) System.out.printf(Locale.ROOT, "%s,%d,%d,%d,%d,%d%n", m.id(), m.arrival(), m.burst(), m.completion(), m.turnaround(), m.waiting());
                System.out.printf(Locale.ROOT, "Average waiting: %.3f%nAverage turnaround: %.3f%nTotal turnaround: %d%n", result.averageWaiting(), result.averageTurnaround(), result.totalTurnaround());
                System.out.println("Timeline:");
                for (Slice slice : result.timeline()) System.out.printf("%s [%d,%d)%n", slice.id(), slice.start(), slice.end());
            }
        } catch (IOException | IllegalArgumentException | ArithmeticException error) {
            System.err.println("Error: " + error.getMessage());
            System.exit(2);
        }
    }
}
