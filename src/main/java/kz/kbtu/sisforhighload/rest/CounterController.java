package kz.kbtu.sisforhighload.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import kz.kbtu.sisforhighload.counter.CounterService;
import kz.kbtu.sisforhighload.counter.CounterSnapshot;
import kz.kbtu.sisforhighload.rest.dto.BatchCountersResponse;
import kz.kbtu.sisforhighload.rest.dto.FinishViewBody;
import kz.kbtu.sisforhighload.rest.dto.ViewTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CounterController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final CounterService counterService;

    @PutMapping("/posts/{postId}/like")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void likePost(@PathVariable @Positive long postId,
                         @RequestHeader(USER_ID_HEADER) long userId) {
        counterService.likePost(postId, userId);
    }

    @DeleteMapping("/posts/{postId}/like")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void unlikePost(@PathVariable @Positive long postId,
                           @RequestHeader(USER_ID_HEADER) long userId) {
        counterService.unlikePost(postId, userId);
    }

    @PostMapping("/posts/{postId}/view_started")
    public ViewTokenResponse startView(@PathVariable @Positive long postId,
                                       @RequestHeader(USER_ID_HEADER) long userId) {
        String viewToken = counterService.startView(postId, userId);
        return new ViewTokenResponse(viewToken);
    }

    @PostMapping("/posts/{postId}/view_finished")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void finishView(@PathVariable @Positive long postId,
                           @RequestHeader(USER_ID_HEADER) long userId,
                           @RequestBody @Valid FinishViewBody body) {
        counterService.finishView(postId, userId, body.viewToken());
    }

    @GetMapping("/posts/{postId}/counters")
    public CounterSnapshot getCounters(@PathVariable @Positive long postId,
                                       @RequestHeader(USER_ID_HEADER) long userId) {
        return counterService.getCounters(postId, userId);
    }

    @GetMapping("/counters")
    public BatchCountersResponse getCountersBatch(
            @RequestParam("post_ids") @Size(min = 1, max = 100) List<Long> postIds,
            @RequestHeader(USER_ID_HEADER) long userId) {
        List<CounterSnapshot> counters = counterService.getCountersBatch(postIds, userId);
        return new BatchCountersResponse(counters);
    }
}