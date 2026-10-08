import { expect, test } from '@playwright/test';
import { createUser, signIn } from './fixtures';

test('DM: start a conversation, deliver live, typing indicator and read receipt', async ({ browser, request }) => {
  const a = await createUser(request, { name: 'Alice E2E', circles: ['trail-miles'] });
  const b = await createUser(request, { name: 'Bob E2E', circles: ['trail-miles'] });

  const alice = await (await browser.newContext()).newPage();
  const bob = await (await browser.newContext()).newPage();
  await signIn(alice, a.email);
  await signIn(bob, b.email);

  // Alice starts the conversation from Bob's profile.
  await alice.goto(`/u/${b.username}`);
  await alice.getByRole('button', { name: 'Message', exact: true }).click();
  await expect(alice).toHaveURL(/\/messages\/.+/);
  const threadUrl = alice.url();

  await bob.goto(threadUrl);
  const bobBox = bob.getByRole('textbox', { name: 'Message', exact: true });
  const aliceBox = alice.getByRole('textbox', { name: 'Message', exact: true });
  await expect(bobBox).toBeVisible();
  await expect(alice.getByRole('status').filter({ hasText: 'Reconnecting' })).toHaveCount(0, { timeout: 15_000 });

  // Typing indicator, then live delivery.
  await aliceBox.pressSequentially('Long run on Sunday?', { delay: 30 });
  await expect(bob.getByText('typing…').first()).toBeVisible();
  await aliceBox.press('Enter');
  await expect(bob.getByText('Long run on Sunday?')).toBeVisible();

  // Bob has the thread open, so Alice sees "Seen".
  await expect(alice.getByText('Seen')).toBeVisible();

  await bobBox.fill("I'm in — 7am at the trailhead");
  await bobBox.press('Enter');
  await expect(alice.getByText("I'm in — 7am at the trailhead")).toBeVisible();

  // Both appear in each other's inbox.
  await alice.goto('/messages');
  await expect(alice.getByRole('link', { name: /Bob E2E/ })).toBeVisible();
});
