import { TREND_STATS, randomPost, randomUser, feedPostIds, getBatch, likePost, unlikePost, viewCycle, buildSummary } from './common.js';

const VUS = parseInt(__ENV.VUS || '64');

export const options = {
    summaryTrendStats: TREND_STATS,
    scenarios: {
        warmup: { executor: 'constant-vus', vus: VUS, duration: '10s', tags: { phase: 'warmup' } },
        measure: { executor: 'constant-vus', vus: VUS, duration: '30s', startTime: '10s', tags: { phase: 'measure' } },
    },
    thresholds: {
        'http_req_duration{phase:measure}': ['max>=0'],
        'http_req_duration{phase:measure,op:read}': ['max>=0'],
        'http_req_duration{phase:measure,op:view}': ['max>=0'],
        'http_req_duration{phase:measure,op:like}': ['max>=0'],
        'http_reqs{phase:measure}': ['count>=0'],
    },
};

export default function () {
    const roll = Math.random();
    const userId = randomUser();
    if (roll < 0.7) {
        getBatch(feedPostIds(), userId);
    } else if (roll < 0.95) {
        viewCycle(randomPost(), userId);
    } else {
        const postId = randomPost();
        likePost(postId, userId);
        unlikePost(postId, userId);
    }
}

export function handleSummary(data) {
    return buildSummary('mixed', data);
}
