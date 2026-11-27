import { check, sleep } from 'k6';
import http from 'k6/http';
import { STAGE, TREND_STATS, getCounters, likePost, unlikePost, startView, finishView, buildSummary, VIEW_PAUSE_SECONDS } from './common.js';

export const options = {
    vus: 1,
    iterations: 1,
    summaryTrendStats: TREND_STATS,
    thresholds: { checks: ['rate==1'] },
};

const POST_ID = 1;

function settle() {
    if (STAGE === '5') {
        sleep(1);
    }
}

function counters(userId) {
    settle();
    return getCounters(POST_ID, userId).json();
}

function completeView(userId) {
    const started = startView(POST_ID, userId);
    sleep(VIEW_PAUSE_SECONDS);
    return finishView(POST_ID, userId, started.json('view_token'));
}

export default function () {
    const userId = Date.now() % 1000000000 + 2000000;
    const other = userId + 1;
    const before = counters(userId);

    likePost(POST_ID, userId);
    likePost(POST_ID, userId);
    const liked = counters(userId);
    check(liked, {
        'double like counts once': (c) => c.likes === before.likes + 1,
        'liked_by_me is true': (c) => c.liked_by_me === true,
    });

    unlikePost(POST_ID, userId);
    const unliked = counters(userId);
    check(unliked, {
        'unlike restores likes': (c) => c.likes === before.likes,
        'liked_by_me is false': (c) => c.liked_by_me === false,
    });

    check(completeView(userId), { 'first view accepted': (r) => r.status === 202 });
    const afterFirst = counters(userId);
    check(afterFirst, { 'view counted once': (c) => c.views === before.views + 1 });

    completeView(userId);
    const afterSecond = counters(userId);
    check(afterSecond, { 'repeat view within an hour not counted': (c) => c.views === before.views + 1 });

    const wrong = finishView(POST_ID, userId, '00000000-0000-0000-0000-000000000000');
    check(wrong, { 'unknown token rejected': (r) => r.status === 400 });

    const foreign = startView(POST_ID, userId);
    sleep(VIEW_PAUSE_SECONDS);
    const stolen = finishView(POST_ID, other, foreign.json('view_token'));
    check(stolen, { 'foreign token rejected': (r) => r.status === 400 });
}

export function handleSummary(data) {
    return buildSummary('smoke', data);
}
