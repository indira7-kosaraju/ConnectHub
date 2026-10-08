import { expect, test } from '@playwright/test';
import { TINY_PNG, createUser, signIn } from './fixtures';

test('post: share text and a photo with a circle, see it in the feed, edit it', async ({ page, request }) => {
  const user = await createUser(request, { circles: ['golden-hour-club'] });
  await signIn(page, user.email);

  const text = `Golden light test ${Date.now()}`;
  await page.getByRole('button', { name: 'New post' }).first().click();
  await page.getByLabel('Circle', { exact: true }).selectOption({ label: '📷 Golden Hour Club' });
  await page.getByLabel('Post text').fill(text);
  await page.locator('input[type=file][multiple]').setInputFiles({ name: 'frame.png', mimeType: 'image/png', buffer: TINY_PNG });
  await expect(page.getByRole('list', { name: 'Attached photos' }).getByRole('listitem')).toHaveCount(1);
  await page.getByRole('button', { name: 'Post', exact: true }).click();
  await expect(page.getByText('Shared with Golden Hour Club')).toBeVisible();

  const card = page.locator('article', { hasText: text });
  await expect(card).toBeVisible();
  await expect(card.getByRole('button', { name: /Open image 1 of 1/ })).toBeVisible();

  // Edit within the 15-minute window
  await card.getByRole('button', { name: 'Post options' }).click();
  await page.getByRole('menuitem', { name: 'Edit' }).click();
  await card.getByLabel('Edit post').fill(`${text} (edited)`);
  await card.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(card.getByText('· edited')).toBeVisible();

  // It lives in the circle too
  await page.goto('/c/golden-hour-club');
  await expect(page.locator('article', { hasText: `${text} (edited)` })).toBeVisible();
});
