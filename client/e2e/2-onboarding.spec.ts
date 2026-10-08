import { expect, test } from '@playwright/test';
import { PASSWORD } from './fixtures';

test('onboarding: username → photo → interests → join a suggested circle → home', async ({ page }) => {
  const id = Date.now().toString(36);
  await page.goto('/register');
  await page.getByLabel('Name').fill('Grace Hopper');
  await page.getByLabel('Email').fill(`grace_${id}@test.dev`);
  await page.getByLabel('Password', { exact: true }).fill(PASSWORD);
  await page.getByRole('button', { name: 'Create account' }).click();

  // 1. Username
  const username = page.getByRole('textbox', { name: 'Username' });
  await username.fill(`grace_${id}`);
  await expect(page.getByText(`@grace_${id} is yours.`)).toBeVisible();
  await page.getByRole('button', { name: 'Continue' }).click();

  // 2. Photo (crop + upload)
  await expect(page.getByRole('heading', { name: 'Add a photo' })).toBeVisible();
  await page.locator('input[type=file]').setInputFiles({
    name: 'me.png',
    mimeType: 'image/png',
    buffer: (await import('./fixtures')).TINY_PNG,
  });
  await page.getByRole('button', { name: 'Save photo' }).click();
  await expect(page.getByText('Looking good!')).toBeVisible();
  await page.getByRole('button', { name: 'Continue' }).click();

  // 3. Interests (at least three)
  const finish = page.getByRole('button', { name: 'Continue' });
  for (const name of ['Photography', 'Coffee']) await page.getByRole('button', { name, exact: true }).click();
  await expect(finish).toBeDisabled();
  await page.getByRole('button', { name: 'Books', exact: true }).click();
  await finish.click();

  // 4. Suggested circles
  await expect(page.getByRole('heading', { name: 'Circles picked for you' })).toBeVisible();
  await page.getByRole('button', { name: /^Join / }).first().click();
  await page.getByRole('button', { name: 'Finish' }).click();

  await expect(page).toHaveURL(/\/$/);
  await expect(page.getByText('Welcome to ConnectHub, Grace!')).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Today', exact: true })).toBeVisible();
});
