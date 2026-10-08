import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { PasswordInput } from '@/components/ui/password-input';
import { FormField } from '@/components/ui/form-field';
import { DividerLabel } from '@/components/ui/divider-label';
import { GoogleButton } from '@/components/feature/auth/google-button';
import { useAuthProviders, useLogin } from '@/hooks/use-auth';
import { applyServerErrors } from '@/lib/form-errors';
import { safeRedirect } from '@/lib/utils';
import { AuthLayout } from './auth-layout';

const schema = z.object({
  email: z.string().trim().min(1, 'Enter your email').email('Enter a valid email'),
  password: z.string().min(1, 'Enter your password'),
});
type Values = z.infer<typeof schema>;

export function LoginPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const next = params.get('next');
  const login = useLogin();
  const providers = useAuthProviders();
  const { register, handleSubmit, setError, formState: { errors } } = useForm<Values>({ resolver: zodResolver(schema) });

  const onSubmit = handleSubmit((values) =>
    login.mutate(values, {
      onSuccess: (res) => navigate(res.user.onboarded ? safeRedirect(next) : '/onboarding', { replace: true }),
      onError: (err) => applyServerErrors(err, setError, ['email', 'password']),
    }),
  );

  return (
    <AuthLayout title="Welcome back" subtitle="Sign in to see what your circles are sharing.">
      <div className="space-y-6">
        {providers.data?.google && (
          <>
            <GoogleButton next={next} />
            <DividerLabel>or</DividerLabel>
          </>
        )}
        <form onSubmit={onSubmit} noValidate className="space-y-4">
          <FormField label="Email" error={errors.email?.message}>
            <Input type="email" autoComplete="email" inputMode="email" autoFocus {...register('email')} />
          </FormField>
          <FormField label="Password" error={errors.password?.message}>
            <PasswordInput autoComplete="current-password" {...register('password')} />
          </FormField>
          <Button type="submit" size="lg" className="w-full" loading={login.isPending}>
            Sign in
          </Button>
        </form>
        <p className="text-sm text-muted">
          New here?{' '}
          <Link to={next ? `/register?next=${encodeURIComponent(next)}` : '/register'} className="font-medium text-accent-ink hover:underline">
            Create an account
          </Link>
        </p>
      </div>
    </AuthLayout>
  );
}
