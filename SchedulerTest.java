import java.util.*;

public class SchedulerTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void completions(String algorithm, long... expected) {
        var input = List.of(new Scheduler.Process("P1", 0, 5), new Scheduler.Process("P2", 1, 3), new Scheduler.Process("P3", 2, 1));
        var result = Scheduler.run(input, algorithm, 2);
        for (int i = 0; i < expected.length; i++) check(result.metrics().get(i).completion() == expected[i], algorithm + " completion " + i);
    }
    public static void main(String[] args) {
        completions("fcfs", 5, 8, 9);
        completions("sjf", 5, 9, 6);
        completions("rr", 9, 8, 5);
        var idle = Scheduler.run(List.of(new Scheduler.Process("A", 3, 2), new Scheduler.Process("B", 10, 1)), "rr", 2);
        check(idle.timeline().get(0).id().equals("IDLE"), "initial idle");
        check(idle.metrics().get(1).completion() == 11, "later idle gap");
        var equal = Scheduler.run(List.of(new Scheduler.Process("Z", 0, 2), new Scheduler.Process("A", 0, 2)), "sjf", 2);
        check(equal.timeline().get(0).id().equals("Z"), "input-order ties");
        try { Scheduler.run(List.of(new Scheduler.Process("A", 0, 1)), "rr", 0); throw new AssertionError("accepted zero quantum"); }
        catch (IllegalArgumentException expected) { }
        try { Scheduler.run(List.of(new Scheduler.Process("A", 0, 1), new Scheduler.Process("A", 1, 2)), "fcfs", 2); throw new AssertionError("accepted duplicate IDs"); }
        catch (IllegalArgumentException expected) { }
        Random random = new Random(42);
        for (int trial = 0; trial < 100; trial++) {
            List<Scheduler.Process> input = new ArrayList<>();
            for (int i = 0; i < 8; i++) input.add(new Scheduler.Process("P" + i, random.nextInt(25), 1 + random.nextInt(15)));
            for (String algorithm : List.of("fcfs", "sjf", "rr")) {
                var result = Scheduler.run(input, algorithm, 3);
                long executed = result.timeline().stream().filter(s -> !s.id().equals("IDLE")).mapToLong(s -> s.end() - s.start()).sum();
                check(executed == input.stream().mapToLong(Scheduler.Process::burst).sum(), "CPU time conservation");
                for (var metric : result.metrics()) {
                    check(metric.waiting() >= 0, "nonnegative waiting");
                    check(metric.turnaround() == metric.waiting() + metric.burst(), "turnaround identity");
                    long ownTime = result.timeline().stream().filter(s -> s.id().equals(metric.id())).mapToLong(s -> s.end() - s.start()).sum();
                    check(ownTime == metric.burst(), "per-process time conservation");
                    check(result.timeline().stream().filter(s -> s.id().equals(metric.id())).allMatch(s -> s.start() >= metric.arrival()), "no execution before arrival");
                }
            }
        }
        System.out.println("PASS: reference schedules, idle gaps, ties, validation and 300 randomized schedule checks.");
    }
}
