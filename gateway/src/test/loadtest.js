import http from 'k6/http';
import { check } from 'k6';
import { Rate } from 'k6/metrics';

// --- Metrics ---
const errorRate = new Rate('errors');

// --- Test Options ---
export const options = {
    stages: [
        //{ duration: '10s', target: 1 , tags: {stage: '1_vu'}}, // 1 VU makes 50-70 request in 5 seconds, 10-14 req in sec
        //{ duration: '10s', target: 2 , tags: {stage: '2_vu'}},
        //{ duration: '10s', target: 4 , tags: {stage: '4_vu'}},
        //{ duration: '10s', target: 8 , tags: {stage: '8_vu'}},
        { duration: '10s', target: 16 , tags: {stage: '16_vu'}},
        { duration: '10s', target: 32 , tags: {stage: '32_vu'}},
        { duration: '10s', target: 64 , tags: {stage: '64_vu'}},
        { duration: '10s', target: 128 , tags: {stage: '128_vu'}},
        { duration: '10s', target: 512 , tags: {stage: '512_vu'}},
        { duration: '10s', target: 1024 , tags: {stage: '1024_vu'}},
        { duration: '10s', target: 2048 , tags: {stage: '2048_vu'}},
        { duration: '10s', target: 2600 , tags: {stage: '2600_vu'}}, // It dies somwhere here
        //{ duration: '10s', target: 4096 , tags: {stage: '4096_vu'}},
        //{ duration: '1m', target: 8192 , tags: {stage: '8192_vu'}},
        { duration: '15s', target: 0, tags: {stage: 'ramp_down'} },  // Graceful ramp-down
    ],
    thresholds: {
        'http_req_duration': ['p(95)<5000'],
        'errors': ['rate<0.01']
    }
};

// --- Test Configuration ---
const BASE_URL = __ENV.GATEWAY_URL || 'http://localhost:8080';
const url = BASE_URL + '/graphql';
const hash = '900150983cd24fb0d6963f7d28e17f72'; // "abc"
const maxLength = 3;
const payload = JSON.stringify({
    query: `mutation { crackHash(hash: "${hash}", maxLength: ${maxLength}) }`
});
const params = { headers: { 'Content-Type': 'application/json' } };

export default function () {
    const res = http.post(url, payload, params);

    const success = check(res, {
        'status is 200': (r) => r.status === 200,
    });

    errorRate.add(!success);
}