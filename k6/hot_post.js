import { TREND_STATS, randomUser, viewCycle, buildSummary } from './common.js';

const VUS = parseInt(__ENV.VUS || '64');
const HOT_POST_ID = 1;

export const options = {
    summaryTrendStats: TREND_STATS,
    vus: VUS,
    duration: '15s',
    thresholds: {
        'http_req_duration{op:view}': ['max>=0'],
    },
};

export default function () {
    viewCycle(HOT_POST_ID, randomUser());
}

export function handleSummary(data) {
    return buildSummary('hot_post', data);
}
