import { expect, test } from '@playwright/test';
import { createUser, signIn } from './fixtures';

test('react: add a vibe, change it, comment and reply', async ({ page, request }) => {
  const author = await createUser(request, { circles: ['third-wave-coffee'] });
  const reader = await createUser(request, { circles: ['third-wave-coffee'] });

  // Author posts (via UI), reader reacts and comments.
  await signIn(page, author.email);
  const text = `Brewing question ${Date.now()}`;
  await page.getByRole('button', { name: 'New post' }).first().click();
  await page.getByLabel('Circle', { exact: true }).selectOption({ label: '☕ Third Wave Coffee' });
  await page.getByLabel('Post text').fill(text);
  await page.getByRole('button', { name: 'Post', exact: true }).click();
  await expect(page.locator('article', { hasText: text })).toBeVisible();
  const href = await page.locator('article', { hasText: text }).getByRole('link', { name: /now|1m/ }).getAttribute('href');

  await page.context().clearCookies();
  await signIn(page, reader.email);
  await page.goto(href!);
  const card = page.locator('article').first();

  await card.getByRole('button', { name: 'Add a vibe' }).click();
  await page.getByRole('menuitemradio', { name: 'Spark' }).click();
  await expect(card.getByRole('button', { name: 'Your vibe: Spark. Change or remove' })).toBeVisible();

  // One vibe per person: changing replaces it; choosing it again removes it.
  await card.getByRole('button', { name: /Your vibe/ }).click();
  await page.getByRole('menuitemradio', { name: 'Deep' }).click();
  await expect(card.getByRole('button', { name: 'Your vibe: Deep. Change or remove' })).toBeVisible();
  await card.getByRole('button', { name: /Your vibe/ }).click();
  await page.getByRole('menuitemradio', { name: 'Deep' }).click();
  await expect(card.getByRole('button', { name: 'Add a vibe' })).toBeVisible();

  await page.getByLabel('Add a comment…').fill('A washed Ethiopian, always.');
  await page.getByRole('button', { name: 'Comment', exact: true }).click();
  await expect(page.getByText('A washed Ethiopian, always.')).toBeVisible();
  await page.getByRole('button', { name: 'Reply' }).first().click();
  await page.getByLabel(/Reply to/).fill('Also a natural Kenyan.');
  await page.getByRole('button', { name: 'Reply', exact: true }).last().click();
  await expect(page.getByText('Also a natural Kenyan.')).toBeVisible();
  await expect(card.getByRole('button', { name: '2 comments' })).toBeVisible();
});
