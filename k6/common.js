import http from 'k6/http';
import { check, sleep } from 'k6';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
export const STAGE = __ENV.STAGE || '0';
export const POSTS = parseInt(__ENV.POSTS || '1000000');
export const USERS = parseInt(__ENV.USERS || '1000000');
export const VIEW_PAUSE_SECONDS = 0.06;
export const HOT_POSTS = 100;
export const BATCH_SIZE = 20;
export const TREND_STATS = ['avg', 'p(50)', 'p(95)', 'p(99)', 'max'];

const API = `${BASE_URL}/api/v1`;

export function randomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

export function randomUser() {
    return randomInt(1, USERS);
}

export function randomPost() {
    return randomInt(1, POSTS);
}

export function feedPostIds() {
    const ids = new Set();
    while (ids.size < BATCH_SIZE) {
        const hot = Math.random() < 0.3;
        ids.add(hot ? randomInt(1, HOT_POSTS) : randomPost());
    }
    return Array.from(ids);
}

function params(userId, op) {
    return {
        headers: { 'X-User-Id': String(userId), 'Content-Type': 'application/json' },
        tags: { op: op },
    };
}

export function likePost(postId, userId) {
    return http.put(`${API}/posts/${postId}/like`, null, params(userId, 'like'));
}

export function unlikePost(postId, userId) {
    return http.del(`${API}/posts/${postId}/like`, null, params(userId, 'like'));
}

export function startView(postId, userId) {
    return http.post(`${API}/posts/${postId}/view_started`, null, params(userId, 'view'));
}

export function finishView(postId, userId, token) {
    const body = JSON.stringify({ view_token: token });
    return http.post(`${API}/posts/${postId}/view_finished`, body, params(userId, 'view'));
}

export function viewCycle(postId, userId) {
    const started = startView(postId, userId);
    check(started, { 'view_started 200': (r) => r.status === 200 });
    if (started.status !== 200) {
        return;
    }
    sleep(VIEW_PAUSE_SECONDS);
    const finished = finishView(postId, userId, started.json('view_token'));
    check(finished, { 'view_finished 202': (r) => r.status === 202 });
}

export function getCounters(postId, userId) {
    return http.get(`${API}/posts/${postId}/counters`, params(userId, 'read'));
}

export function getBatch(postIds, userId) {
    return http.get(`${API}/counters?post_ids=${postIds.join(',')}`, params(userId, 'read'));
}

function row(name, metric) {
    const v = metric.values;
    const cells = ['avg', 'p(50)', 'p(95)', 'p(99)', 'max'].map((k) => (v[k] === undefined ? '-' : v[k].toFixed(2)));
    return `| ${name} | ${cells.join(' | ')} |`;
}

function toMarkdown(scenario, data) {
    const lines = [`## Stage ${STAGE}, ${scenario}`, '', '| metric | avg ms | p50 ms | p95 ms | p99 ms | max ms |', '|---|---|---|---|---|---|'];
    Object.keys(data.metrics)
        .filter((name) => name.startsWith('http_req_duration') || name === 'replica_lag_ms')
        .forEach((name) => lines.push(row(name, data.metrics[name])));
    const reqs = data.metrics['http_reqs{phase:measure}'] || data.metrics['http_reqs'];
    const failed = data.metrics['http_req_failed'];
    lines.push('', `requests: ${reqs.values.count}, rps: ${reqs.values.rate.toFixed(1)}, failed: ${(failed.values.rate * 100).toFixed(2)}%`, '');
    return lines.join('\n');
}

export function buildSummary(scenario, data) {
    const path = `/results/stage${STAGE}-${scenario}`;
    return {
        [`${path}.json`]: JSON.stringify(data, null, 2),
        [`${path}.md`]: toMarkdown(scenario, data),
        stdout: toMarkdown(scenario, data),
    };
}
