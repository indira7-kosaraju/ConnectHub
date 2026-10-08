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
import { useAuthProviders, useRegister } from '@/hooks/use-auth';
import { applyServerErrors } from '@/lib/form-errors';
import { AuthLayout } from './auth-layout';

// Mirrors the server's RegisterRequest constraints.
const schema = z.object({
  displayName: z.string().trim().min(1, 'What should we call you?').max(60, 'Keep it under 60 characters'),
  email: z.string().trim().min(1, 'Enter your email').email('Enter a valid email').max(254),
  password: z
    .string()
    .min(8, 'Use at least 8 characters')
    .max(72, 'Use 72 characters or fewer')
    .regex(/^(?=.*[A-Za-z])(?=.*\d).+$/, 'Include at least one letter and one number'),
});
type Values = z.infer<typeof schema>;

export function RegisterPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const signup = useRegister();
  const providers = useAuthProviders();
  const { register, handleSubmit, setError, formState: { errors } } = useForm<Values>({ resolver: zodResolver(schema) });

  const onSubmit = handleSubmit((values) =>
    signup.mutate(values, {
      onSuccess: () => navigate('/onboarding', { replace: true }),
      onError: (err) => applyServerErrors(err, setError, ['displayName', 'email', 'password']),
    }),
  );

  return (
    <AuthLayout title="Join ConnectHub" subtitle="Pick your interests, find your circles.">
      <div className="space-y-6">
        {providers.data?.google && (
          <>
            <GoogleButton next={params.get('next')} />
            <DividerLabel>or</DividerLabel>
          </>
        )}
        <form onSubmit={onSubmit} noValidate className="space-y-4">
          <FormField label="Name" error={errors.displayName?.message}>
            <Input autoComplete="name" autoFocus {...register('displayName')} />
          </FormField>
          <FormField label="Email" error={errors.email?.message}>
            <Input type="email" autoComplete="email" inputMode="email" {...register('email')} />
          </FormField>
          <FormField label="Password" error={errors.password?.message} hint="At least 8 characters, with a letter and a number.">
            <PasswordInput autoComplete="new-password" {...register('password')} />
          </FormField>
          <Button type="submit" size="lg" className="w-full" loading={signup.isPending}>
            Create account
          </Button>
        </form>
        <p className="text-sm text-muted">
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-accent-ink hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </AuthLayout>
  );
}
