import { expect, test } from '@playwright/test';
import { PASSWORD } from './fixtures';

test('sign up: validates the form, creates the account and lands in onboarding', async ({ page }) => {
  await page.goto('/');
  await expect(page).toHaveURL(/\/login\?next=%2F/); // signed-out visitors are sent to sign in

  await page.getByRole('link', { name: 'Create an account' }).click();
  await page.getByLabel('Name').fill('Ada Lovelace');
  await page.getByLabel('Email').fill('not-an-email');
  await page.getByLabel('Password', { exact: true }).fill('short');
  await page.getByRole('button', { name: 'Create account' }).click();
  await expect(page.getByText('Enter a valid email')).toBeVisible();
  await expect(page.getByText('Use at least 8 characters')).toBeVisible();

  await page.getByLabel('Email').fill(`ada_${Date.now()}@test.dev`);
  await page.getByLabel('Password', { exact: true }).fill(PASSWORD);
  await page.getByRole('button', { name: 'Create account' }).click();

  await expect(page).toHaveURL(/\/onboarding$/);
  await expect(page.getByRole('heading', { name: 'Pick a username' })).toBeVisible();
  // The username is pre-filled from the name and checked live.
  await expect(page.getByRole('textbox', { name: 'Username' })).toHaveValue('ada_lovelace');
});
