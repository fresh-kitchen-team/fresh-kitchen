import http from 'k6/http';
import { check } from 'k6';

export function devLogin(baseUrl, userId) {
  const res = http.post(
    `${baseUrl}/api/v1/auth/dev-login`,
    JSON.stringify({ userId }),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'auth/dev-login' },
    }
  );
  const ok = check(res, { 'dev-login 200': (r) => r.status === 200 });
  if (!ok) {
    throw new Error(
      `dev-login failed: status=${res.status} body=${res.body}`
    );
  }
  return res.json('data.accessToken');
}

export function loginAsVu(baseUrl, baseUserId, pool) {
  const userId = baseUserId + ((__VU - 1) % pool);
  return devLogin(baseUrl, userId);
}