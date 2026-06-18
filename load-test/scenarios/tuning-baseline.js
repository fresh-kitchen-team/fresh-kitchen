


import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.1.0/index.js';
import { devLogin } from '../lib/auth.js';

const BASE_URL = (__ENV.BASE_URL || 'https://api.app-fresh.com').replace(/\/$/, '');
const USER_ID_BASE = Number(__ENV.USER_ID_BASE || 1);
const USER_POOL = Number(__ENV.USER_POOL || 50);

const homeTrend = new Trend('t_home', true);
const itemsTrend = new Trend('t_items', true);
const searchTrend = new Trend('t_search', true);
const summaryTrend = new Trend('t_summary', true);
const expiringTrend = new Trend('t_expiring', true);
const errorRate = new Rate('errors');
const reqCounter = new Counter('endpoint_calls');

export const options = {
    scenarios: {
    query_rampup: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 50 },
        { duration: '1m', target: 200 },
        { duration: '3m', target: 200 },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '15s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.02'],
    'http_req_duration{name:home/summary}': ['p(95)<800', 'p(99)<1500'],
    'http_req_duration{name:items/list}': ['p(95)<800', 'p(99)<1500'],
    'http_req_duration{name:items/search}': ['p(95)<1000', 'p(99)<2000'],
    'http_req_duration{name:analytics/summary}': ['p(95)<1200', 'p(99)<2500'],
    'http_req_duration{name:analytics/expiring}': ['p(95)<1000', 'p(99)<2000'],
  },
};

let token;
function getToken() {
  if (token) return token;
  const userId = USER_ID_BASE + ((__VU - 1) % USER_POOL);
  token = devLogin(BASE_URL, userId);
  return token;
}

const WEIGHTED = [
  { name: 'home', weight: 30 },
  { name: 'items', weight: 25 },
  { name: 'search', weight: 15 },
  { name: 'summary', weight: 15 },
  { name: 'expiring', weight: 15 },
];

function pickEndpoint() {
  const total = WEIGHTED.reduce((s, w) => s + w.weight, 0);
  let r = Math.random() * total;
  for (const w of WEIGHTED) {
    if ((r -= w.weight) < 0) return w.name;
  }
  return WEIGHTED[0].name;
}

const SEARCH_TERMS = ['seed-1', 'seed-2', 'seed-3', 'seed', '1', '2'];

export default function () {
  const headers = { Authorization: `Bearer ${getToken()}` };
  const ep = pickEndpoint();
  reqCounter.add(1, { endpoint: ep });

  if (ep === 'home') {
    const r = http.get(`${BASE_URL}/api/v1/home/summary`, {
      headers,
      tags: { name: 'home/summary' },
    });
    homeTrend.add(r.timings.duration);
    errorRate.add(r.status !== 200);
    check(r, { 'home 200': (x) => x.status === 200 });
  } else if (ep === 'items') {
    const r = http.get(`${BASE_URL}/api/v1/items`, {
      headers,
      tags: { name: 'items/list' },
    });
    itemsTrend.add(r.timings.duration);
    errorRate.add(r.status !== 200);
    check(r, { 'items 200': (x) => x.status === 200 });
  } else if (ep === 'search') {
    const term =
      SEARCH_TERMS[Math.floor(Math.random() * SEARCH_TERMS.length)];
    const r = http.get(`${BASE_URL}/api/v1/items?name=${term}`, {
      headers,
      tags: { name: 'items/search' },
    });
    searchTrend.add(r.timings.duration);
    errorRate.add(r.status !== 200);
    check(r, { 'search 200': (x) => x.status === 200 });
  } else if (ep === 'summary') {
    const r = http.get(`${BASE_URL}/api/v1/analytics/summary`, {
      headers,
      tags: { name: 'analytics/summary' },
    });
    summaryTrend.add(r.timings.duration);
    errorRate.add(r.status !== 200);
    check(r, { 'summary 200': (x) => x.status === 200 });
  } else {
    const dday = [3, 7, 10][Math.floor(Math.random() * 3)];
    const r = http.get(
      `${BASE_URL}/api/v1/analytics/expiring-items?maxDDay=${dday}`,
      { headers, tags: { name: 'analytics/expiring' } }
    );
    expiringTrend.add(r.timings.duration);
    errorRate.add(r.status !== 200);
    check(r, { 'expiring 200': (x) => x.status === 200 });
  }

  sleep(Math.random() * 1.5 + 0.5);
}

export function handleSummary(data) {
  return {
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
    'summary.json': JSON.stringify(data, null, 2),
  };
}