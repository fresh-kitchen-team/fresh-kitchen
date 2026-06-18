import http from 'k6/http';
import { check } from 'k6';
import { devLogin } from '../lib/auth.js';

const BASE_URL = (__ENV.BASE_URL || 'https://api.app-fresh.com').replace(/\/$/, '');
const USER_ID = Number(__ENV.USER_ID || 1);

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ['rate==1.0'],
    http_req_failed: ['rate==0.0'],
  },
};


export default function () {
  console.log(`[smoke] BASE_URL=${BASE_URL}, USER_ID=${USER_ID}`);

  const token = devLogin(BASE_URL, USER_ID);
  console.log(`[smoke] token acquired (length=${token ? token.length : 0})`);

  const h = { headers: { Authorization: `Bearer ${token}` } };

  const r1 = http.get(`${BASE_URL}/api/v1/home/summary`, {
    ...h,
    tags: { name: 'home/summary' },
  });
  const r2 = http.get(`${BASE_URL}/api/v1/items`, {
    ...h,
    tags: { name: 'items/list' },
  });
  const r3 = http.get(`${BASE_URL}/api/v1/items?name=seed`, {
    ...h,
    tags: { name: 'items/search' },
  });
  const r4 = http.get(`${BASE_URL}/api/v1/analytics/summary`, {
    ...h,
    tags: { name: 'analytics/summary' },
  });
  const r5 = http.get(
    `${BASE_URL}/api/v1/analytics/expiring-items?maxDDay=7`,
    { ...h, tags: { name: 'analytics/expiring' } }
  );

  check(r1, { 'home 200': (r) => r.status === 200 });
  check(r2, { 'items 200': (r) => r.status === 200 });
  check(r3, { 'items search 200': (r) => r.status === 200 });
  check(r4, { 'analytics summary 200': (r) => r.status === 200 });
  check(r5, { 'analytics expiring 200': (r) => r.status === 200 });

  // 데이터 양 점검 — 본격 부하 전에 시드 충분한지 확인
  const itemsLen = (r2.json('data') || []).length;
  const expiringLen = (r5.json('data') || []).length;
  const homeTotal = r1.json('data.totalCount');

  console.log(`[smoke] /home/summary totalCount = ${homeTotal}`);
  console.log(`[smoke] /items length           = ${itemsLen}`);
  console.log(`[smoke] /analytics/expiring     = ${expiringLen}`);

  if (itemsLen < 100) {
    console.warn(
      `[smoke] ⚠️ items가 ${itemsLen}개뿐입니다. 부하테스트 의미를 위해 500+ 권장.`
    );
  }

  // 각 응답 상세 로깅 (디버깅용)
  [
    ['home/summary', r1],
    ['items/list', r2],
    ['items/search', r3],
    ['analytics/summary', r4],
    ['analytics/expiring', r5],
  ].forEach(([name, r]) => {
    console.log(
      `[smoke] ${name.padEnd(20)} status=${r.status} time=${r.timings.duration.toFixed(0)}ms`
    );
    if (r.status !== 200) {
      console.error(`  body: ${r.body}`);
    }
  });
}
