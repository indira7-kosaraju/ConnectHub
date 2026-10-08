import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AnimatePresence, MotionConfig, motion } from 'framer-motion';
import { Logo } from '@/components/ui/logo';
import { toast } from '@/components/ui/toaster';
import { UsernameStep } from '@/components/feature/onboarding/username-step';
import { AvatarStep } from '@/components/feature/onboarding/avatar-step';
import { InterestsStep } from '@/components/feature/onboarding/interests-step';
import { CirclesStep } from '@/components/feature/onboarding/circles-step';
import { useAuthStore } from '@/stores/auth-store';
import { cn } from '@/lib/utils';

const STEPS = 4;

export function OnboardingPage() {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  // Resume at the first unfinished step (e.g. after a reload).
  const [step, setStep] = useState(() =>
    !user?.username ? 0 : !user.avatarUrl ? 1 : (user.interests.length ?? 0) < 3 ? 2 : 3,
  );
  const [direction, setDirection] = useState(1);

  const go = (to: number) => {
    setDirection(to > step ? 1 : -1);
    setStep(to);
  };

  const done = () => {
    toast.success(`Welcome to ConnectHub, ${user?.displayName?.split(' ')[0] ?? 'friend'}!`);
    navigate('/', { replace: true });
  };

  return (
    <MotionConfig reducedMotion="user">
      <div className="flex min-h-dvh flex-col bg-bg">
      <header className="mx-auto flex w-full max-w-2xl items-center justify-between px-6 pt-6">
        <Logo />
        <span className="text-sm text-muted">
          {step + 1} of {STEPS}
        </span>
      </header>
      <div
        className="mx-auto mt-6 flex w-full max-w-2xl gap-1.5 px-6"
        role="progressbar"
        aria-label="Onboarding progress"
        aria-valuemin={1}
        aria-valuemax={STEPS}
        aria-valuenow={step + 1}
      >
        {Array.from({ length: STEPS }, (_, i) => (
          <span key={i} className={cn('h-1 flex-1 rounded-full transition-colors duration-300', i <= step ? 'bg-accent' : 'bg-line')} />
        ))}
      </div>

      <main className="mx-auto flex w-full max-w-2xl flex-1 flex-col overflow-x-hidden px-6 pb-6 pt-10">
        <AnimatePresence mode="wait" custom={direction} initial={false}>
          <motion.div
            key={step}
            custom={direction}
            initial={{ opacity: 0, x: direction * 24 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: direction * -24 }}
            transition={{ duration: 0.22, ease: 'easeOut' }}
            className="flex flex-1 flex-col"
          >
            {step === 0 && <UsernameStep onNext={() => go(1)} />}
            {step === 1 && <AvatarStep onNext={() => go(2)} onBack={() => go(0)} />}
            {step === 2 && <InterestsStep onBack={() => go(1)} onNext={() => go(3)} />}
            {step === 3 && <CirclesStep onBack={() => go(2)} onDone={done} />}
          </motion.div>
        </AnimatePresence>
      </main>
    </div>
    </MotionConfig>
  );
}
