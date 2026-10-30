package kz.kbtu.sisforhighload.benchmark;

import java.util.Arrays;
import java.util.Locale;

public record LatencyStats(String name, double avgMs, double p50Ms, double p95Ms, double p99Ms) {

    private static final double NANOS_IN_MILLI = 1_000_000.0;

    public static LatencyStats from(String name, long[] durationsNanos) {
        long[] sorted = durationsNanos.clone();
        Arrays.sort(sorted);
        double avgMs = Arrays.stream(sorted).average().orElse(0) / NANOS_IN_MILLI;
        return new LatencyStats(name, avgMs,
                percentile(sorted, 50), percentile(sorted, 95), percentile(sorted, 99));
    }

    public String toMarkdownRow() {
        return String.format(Locale.US, "| %s | %.3f | %.3f | %.3f | %.3f |",
                name, avgMs, p50Ms, p95Ms, p99Ms);
    }

    private static double percentile(long[] sorted, int percent) {
        int index = (int) Math.ceil(percent / 100.0 * sorted.length) - 1;
        return sorted[Math.max(index, 0)] / NANOS_IN_MILLI;
    }
}