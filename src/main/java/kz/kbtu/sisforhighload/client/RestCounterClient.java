package kz.kbtu.sisforhighload.client;

import kz.kbtu.sisforhighload.counter.CounterSnapshot;
import kz.kbtu.sisforhighload.rest.dto.BatchCountersResponse;
import kz.kbtu.sisforhighload.rest.dto.FinishViewBody;
import kz.kbtu.sisforhighload.rest.dto.ViewTokenResponse;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class RestCounterClient implements AutoCloseable {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;
    private final String baseUrl;
    private final Duration timeout;
    private final RetryExecutor retryExecutor;

    public RestCounterClient(String baseUrl, Duration timeout, RetryExecutor retryExecutor) {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeout)
                .build();
        this.jsonMapper = JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();
        this.baseUrl = baseUrl;
        this.timeout = timeout;
        this.retryExecutor = retryExecutor;
    }

    public void likePost(long postId, long userId) {
        HttpRequest request = requestFor("/posts/" + postId + "/like", userId)
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();
        send("rest.likePost", request);
    }

    public void unlikePost(long postId, long userId) {
        HttpRequest request = requestFor("/posts/" + postId + "/like", userId)
                .DELETE()
                .build();
        send("rest.unlikePost", request);
    }

    public String startView(long postId, long userId) {
        HttpRequest request = requestFor("/posts/" + postId + "/view_started", userId)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        String body = send("rest.startView", request);
        return jsonMapper.readValue(body, ViewTokenResponse.class).viewToken();
    }

    public void finishView(long postId, long userId, String viewToken) {
        String json = jsonMapper.writeValueAsString(new FinishViewBody(viewToken));
        HttpRequest request = requestFor("/posts/" + postId + "/view_finished", userId)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        send("rest.finishView", request);
    }

    public CounterSnapshot getCounters(long postId, long userId) {
        HttpRequest request = requestFor("/posts/" + postId + "/counters", userId).GET().build();
        String body = send("rest.getCounters", request);
        return jsonMapper.readValue(body, CounterSnapshot.class);
    }

    public BatchCountersResponse getCountersBatch(List<Long> postIds, long userId) {
        String joinedIds = postIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        HttpRequest request = requestFor("/counters?post_ids=" + joinedIds, userId).GET().build();
        String body = send("rest.batchGetCounters", request);
        return jsonMapper.readValue(body, BatchCountersResponse.class);
    }

    @Override
    public void close() {
        httpClient.close();
    }

    private HttpRequest.Builder requestFor(String path, long userId) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(timeout)
                .header(USER_ID_HEADER, Long.toString(userId));
    }

    private String send(String operation, HttpRequest request) {
        return retryExecutor.execute(operation, () -> sendOnce(request), this::isRetryable);
    }

    private String sendOnce(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new HttpStatusException(response.statusCode(), response.body());
        }
        return response.body();
    }

    // IOException включает HttpTimeoutException (таймаут) и ConnectException (сервер недоступен)
    private boolean isRetryable(Exception error) {
        if (error instanceof IOException) {
            return true;
        }
        return error instanceof HttpStatusException httpError && httpError.isServerUnavailable();
    }
}