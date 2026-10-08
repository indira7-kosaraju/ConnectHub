import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { LogOut } from 'lucide-react';
import { PageHeader } from '@/components/ui/page-header';
import { Avatar } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { FormField } from '@/components/ui/form-field';
import { Input } from '@/components/ui/input';
import { PasswordInput } from '@/components/ui/password-input';
import { Label } from '@/components/ui/label';
import { Skeleton } from '@/components/ui/skeleton';
import { Switch } from '@/components/ui/switch';
import { ThemeToggle } from '@/components/feature/theme-toggle';
import { useLogout } from '@/hooks/use-auth';
import { useBlockMutations, useBlocks, useDeleteAccount, useSettingsMutations } from '@/hooks/use-settings';
import { applyServerErrors } from '@/lib/form-errors';
import { useAuthStore } from '@/stores/auth-store';

const NOTIFICATION_LABELS: [string, string, string][] = [
  ['reactions', 'Vibes', 'When someone reacts to your post'],
  ['comments', 'Comments', 'When someone comments on your post'],
  ['replies', 'Replies', 'When someone replies to your comment'],
  ['circles', 'Circles', 'Join requests and approvals'],
  ['messages', 'Messages', 'New direct messages'],
];

const passwordRule = z
  .string()
  .min(8, 'Use at least 8 characters')
  .max(72)
  .regex(/^(?=.*[A-Za-z])(?=.*\d).+$/, 'Include at least one letter and one number');

function SettingRow({ id, label, description, checked, onChange }: { id: string; label: string; description: string; checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <div className="flex items-center justify-between gap-4 py-3">
      <div>
        <Label htmlFor={id}>{label}</Label>
        <p className="text-[13px] text-muted">{description}</p>
      </div>
      <Switch id={id} checked={checked} onCheckedChange={onChange} />
    </div>
  );
}

function EmailForm() {
  const user = useAuthStore((s) => s.user)!;
  const { email } = useSettingsMutations();
  const schema = z.object({ email: z.string().trim().email('Enter a valid email'), password: z.string().min(1, 'Enter your current password') });
  const form = useForm<z.infer<typeof schema>>({ resolver: zodResolver(schema), defaultValues: { email: user.email, password: '' } });
  if (!user.hasPassword) {
    return <p className="text-sm text-muted">Your email ({user.email}) comes from Google sign-in. Set a password below to change it.</p>;
  }
  return (
    <form
      noValidate
      className="space-y-3"
      onSubmit={form.handleSubmit((v) =>
        email.mutate(v, {
          onSuccess: () => form.reset({ email: v.email, password: '' }),
          onError: (err) => applyServerErrors(err, form.setError, ['email', 'password']),
        }),
      )}
    >
      <FormField label="Email" error={form.formState.errors.email?.message}>
        <Input type="email" autoComplete="email" {...form.register('email')} />
      </FormField>
      <FormField label="Current password" error={form.formState.errors.password?.message}>
        <PasswordInput autoComplete="current-password" {...form.register('password')} />
      </FormField>
      <Button type="submit" size="sm" loading={email.isPending}>
        Update email
      </Button>
    </form>
  );
}

function PasswordForm() {
  const user = useAuthStore((s) => s.user)!;
  const { password } = useSettingsMutations();
  const schema = z.object({ current: user.hasPassword ? z.string().min(1, 'Enter your current password') : z.string(), next: passwordRule });
  const form = useForm<z.infer<typeof schema>>({ resolver: zodResolver(schema), defaultValues: { current: '', next: '' } });
  return (
    <form
      noValidate
      className="space-y-3"
      onSubmit={form.handleSubmit((v) =>
        password.mutate(
          { next: v.next, current: user.hasPassword ? v.current : undefined },
          {
            onSuccess: () => form.reset(),
            onError: (err) => applyServerErrors(err, form.setError, ['current', 'next']) || form.setError('current', { message: 'Check your current password' }),
          },
        ),
      )}
    >
      {user.hasPassword && (
        <FormField label="Current password" error={form.formState.errors.current?.message}>
          <PasswordInput autoComplete="current-password" {...form.register('current')} />
        </FormField>
      )}
      <FormField label={user.hasPassword ? 'New password' : 'Set a password'} error={form.formState.errors.next?.message} hint="At least 8 characters, with a letter and a number.">
        <PasswordInput autoComplete="new-password" {...form.register('next')} />
      </FormField>
      <Button type="submit" size="sm" loading={password.isPending}>
        {user.hasPassword ? 'Change password' : 'Set password'}
      </Button>
    </form>
  );
}

function BlockedUsers() {
  const blocks = useBlocks();
  const { unblock } = useBlockMutations();
  if (blocks.isPending) return <Skeleton className="h-10 w-full" />;
  if (!blocks.data?.length) return <p className="text-sm text-muted">You haven’t blocked anyone.</p>;
  return (
    <ul className="divide-y divide-line">
      {blocks.data.map((u) => (
        <li key={u.id} className="flex items-center gap-3 py-3">
          <Avatar size="sm" src={u.avatarUrl} name={u.displayName ?? u.username} />
          <span className="min-w-0 flex-1 truncate text-sm">
            <span className="font-medium">{u.displayName ?? u.username}</span> <span className="text-muted">@{u.username}</span>
          </span>
          <Button size="sm" variant="outline" loading={unblock.isPending && unblock.variables === u.username} onClick={() => unblock.mutate(u.username)}>
            Unblock
          </Button>
        </li>
      ))}
    </ul>
  );
}

function DeleteAccount() {
  const user = useAuthStore((s) => s.user)!;
  const remove = useDeleteAccount();
  const [open, setOpen] = useState(false);
  const [value, setValue] = useState('');
  const ok = user.hasPassword ? value.length > 0 : value === 'DELETE';
  return (
    <>
      <Button variant="danger" size="sm" onClick={() => setOpen(true)}>
        Delete my account
      </Button>
      <Dialog open={open} onOpenChange={(o) => !remove.isPending && setOpen(o)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete your account?</DialogTitle>
            <DialogDescription>
              Your posts, comments, moments, messages and memberships are removed for good. Circles you admin pass to their longest-standing member.
            </DialogDescription>
          </DialogHeader>
          <div className="px-6 pb-4">
            <FormField label={user.hasPassword ? 'Enter your password to confirm' : 'Type DELETE to confirm'} error={remove.error ? 'That didn’t match — try again' : undefined}>
              {user.hasPassword ? (
                <PasswordInput autoComplete="current-password" value={value} onChange={(e) => setValue(e.target.value)} />
              ) : (
                <Input value={value} onChange={(e) => setValue(e.target.value)} />
              )}
            </FormField>
          </div>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="danger"
              disabled={!ok}
              loading={remove.isPending}
              onClick={() => remove.mutate(user.hasPassword ? { password: value } : { confirm: value })}
            >
              Delete forever
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}

export function SettingsPage() {
  const user = useAuthStore((s) => s.user);
  const logout = useLogout();
  const { update } = useSettingsMutations();
  if (!user) return null;
  const prefs = user.notificationPrefs;

  return (
    <div className="space-y-4">
      <PageHeader title="Settings" />

      <Card>
        <CardHeader>
          <CardTitle>Appearance</CardTitle>
          <CardDescription>Choose how ConnectHub looks on this device.</CardDescription>
        </CardHeader>
        <CardContent>
          <ThemeToggle />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Privacy</CardTitle>
          <CardDescription>
            Your profile details are always visible to people in your circles. <Link to={`/u/${user.username}`} className="text-accent-ink hover:underline">View your profile</Link>
          </CardDescription>
        </CardHeader>
        <CardContent>
          <SettingRow
            id="private-profile"
            label="Private profile"
            description="Only people who share a circle with you can see your posts, circles, location and website."
            checked={user.privateProfile}
            onChange={(v) => update.mutate({ privateProfile: v })}
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Notifications</CardTitle>
          <CardDescription>Choose what reaches your bell. Changes apply instantly.</CardDescription>
        </CardHeader>
        <CardContent className="divide-y divide-line">
          {NOTIFICATION_LABELS.map(([key, label, description]) => (
            <SettingRow
              key={key}
              id={`notify-${key}`}
              label={label}
              description={description}
              checked={prefs[key] ?? true}
              onChange={(v) => update.mutate({ notificationPrefs: { [key]: v } })}
            />
          ))}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Account</CardTitle>
          <CardDescription>Signed in as {user.email}{user.provider === 'google' ? ' with Google' : ''}.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-8">
          <EmailForm />
          <PasswordForm />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Blocked people</CardTitle>
          <CardDescription>You and they can’t see each other’s content or message each other.</CardDescription>
        </CardHeader>
        <CardContent>
          <BlockedUsers />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Session</CardTitle>
        </CardHeader>
        <CardContent>
          <Button variant="outline" onClick={() => logout.mutate()} loading={logout.isPending}>
            <LogOut aria-hidden /> Sign out
          </Button>
        </CardContent>
      </Card>

      <Card className="border-danger/40">
        <CardHeader>
          <CardTitle className="text-danger">Delete account</CardTitle>
          <CardDescription>This can’t be undone.</CardDescription>
        </CardHeader>
        <CardContent>
          <DeleteAccount />
        </CardContent>
      </Card>
    </div>
  );
}
