import { expect, type APIRequestContext, type Page } from '@playwright/test';

export const PASSWORD = 'Password123';

export interface TestUser {
  email: string;
  username: string;
  displayName: string;
  id: string;
}

const uid = () => Math.random().toString(36).slice(2, 8);

/** A valid 8×8 PNG for upload tests. */
export const TINY_PNG = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAIAAABLbSncAAAAEklEQVR4nGP4z8CAFWEXHbQSACj/P8Fu7N9hAAAAAElFTkSuQmCC',
  'base64',
);

async function json<T>(res: Awaited<ReturnType<APIRequestContext['post']>>): Promise<T> {
  expect(res.ok(), `${res.url()} → ${res.status()} ${await res.text()}`).toBeTruthy();
  return ((await res.json()) as { data: T }).data;
}

/**
 * Creates a fully onboarded user through the API (fast), optionally joining circles.
 * UI flows under test (sign-up, onboarding…) are exercised separately through the browser.
 */
export async function createUser(request: APIRequestContext, opts: { name?: string; circles?: string[] } = {}): Promise<TestUser> {
  const id = uid();
  const displayName = opts.name ?? `E2E ${id}`;
  const email = `e2e_${id}@test.dev`;
  const auth = await json<{ accessToken: string; user: { id: string } }>(
    await request.post('/api/v1/auth/register', { data: { displayName, email, password: PASSWORD } }),
  );
  const headers = { Authorization: `Bearer ${auth.accessToken}` };
  const username = `e2e_${id}`;
  await json(await request.put('/api/v1/me/username', { headers, data: { username } }));
  const interests = await json<{ id: string }[]>(await request.get('/api/v1/interests'));
  await json(await request.put('/api/v1/me/interests', { headers, data: { interestIds: interests.slice(0, 3).map((i) => i.id) } }));
  await json(await request.post('/api/v1/me/onboarding/complete', { headers }));
  for (const slug of opts.circles ?? []) {
    await json(await request.post(`/api/v1/circles/${slug}/join`, { headers }));
  }
  return { email, username, displayName, id: auth.user.id };
}

export async function signIn(page: Page, email: string) {
  await page.goto('/login');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password', { exact: true }).fill(PASSWORD);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page).toHaveURL(/\/$/);
}
