import { sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import { TREND_STATS, randomPost, randomUser, getCounters, likePost, unlikePost, buildSummary } from './common.js';

const replicaLag = new Trend('replica_lag_ms', true);
const lagTimeouts = new Counter('replica_lag_timeouts');
const LAG_LIMIT_MS = 2000;
const TIMEOUT_MS = 5000;
const POLL_SECONDS = 0.01;

export const options = {
    summaryTrendStats: TREND_STATS,
    vus: 4,
    duration: '20s',
    thresholds: {
        replica_lag_ms: [`p(99)<${LAG_LIMIT_MS}`],
        replica_lag_timeouts: ['count==0'],
    },
};

function waitUntilVisible(postId, userId, startedAt) {
    while (Date.now() - startedAt < TIMEOUT_MS) {
        if (getCounters(postId, userId).json('liked_by_me') === true) {
            return Date.now() - startedAt;
        }
        sleep(POLL_SECONDS);
    }
    return -1;
}

export default function () {
    const postId = randomPost();
    const userId = randomUser() + 3000000;
    likePost(postId, userId);
    const lag = waitUntilVisible(postId, userId, Date.now());
    if (lag < 0) {
        lagTimeouts.add(1);
    } else {
        replicaLag.add(lag);
    }
    unlikePost(postId, userId);
}

export function handleSummary(data) {
    return buildSummary('lag', data);
}
