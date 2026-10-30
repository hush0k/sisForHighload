package kz.kbtu.sisforhighload.rest.dto;

import kz.kbtu.sisforhighload.counter.CounterSnapshot;

import java.util.List;

public record BatchCountersResponse(List<CounterSnapshot> counters) {
}