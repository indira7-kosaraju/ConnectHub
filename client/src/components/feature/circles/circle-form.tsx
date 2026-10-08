import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useQuery } from '@tanstack/react-query';
import { z } from 'zod';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Select } from '@/components/ui/select';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';
import { FormField } from '@/components/ui/form-field';
import { CharCount } from '@/components/ui/char-count';
import { Skeleton } from '@/components/ui/skeleton';
import { interestsApi } from '@/lib/api/auth';
import type { Interest } from '@/lib/api/schema';
import { qk } from '@/lib/query-keys';

export const circleSchema = z.object({
  name: z.string().trim().min(3, 'At least 3 characters').max(50, '50 characters max'),
  description: z.string().trim().max(300, '300 characters max'),
  categoryId: z.string().min(1, 'Choose a category'),
  privateCircle: z.boolean(),
  maxMembers: z.coerce.number<number>().int().min(2, 'At least 2').max(150, 'Circles stay small — 150 max'),
});
export type CircleFormValues = z.infer<typeof circleSchema>;

interface CircleFormProps {
  id: string;
  defaultValues: CircleFormValues;
  minMembers?: number;
  onSubmit: (values: CircleFormValues) => void;
  serverErrors?: Partial<Record<keyof CircleFormValues, string>>;
}

/**
 * Shared by create and edit dialogs; the dialog owns the submit button (form={id}).
 * Waits for categories so the native <select> mounts with its options and keeps the default value.
 */
export function CircleForm(props: CircleFormProps) {
  const interests = useQuery({ queryKey: qk.interests, queryFn: interestsApi.list, staleTime: Infinity });
  if (!interests.data) {
    return (
      <div className="space-y-4 px-6 pb-4" aria-busy>
        <Skeleton className="h-11 w-full rounded-xl" />
        <Skeleton className="h-24 w-full rounded-xl" />
        <Skeleton className="h-11 w-full rounded-xl" />
      </div>
    );
  }
  return <CircleFormFields {...props} categories={interests.data} />;
}

function CircleFormFields({ id, defaultValues, minMembers = 2, onSubmit, serverErrors, categories }: CircleFormProps & { categories: Interest[] }) {
  const { register, handleSubmit, watch, setValue, setError, formState: { errors } } = useForm<CircleFormValues>({
    resolver: zodResolver(circleSchema),
    defaultValues,
  });

  useEffect(() => {
    for (const [field, message] of Object.entries(serverErrors ?? {})) {
      if (message) setError(field as keyof CircleFormValues, { type: 'server', message });
    }
  }, [serverErrors, setError]);

  const description = watch('description');
  const privateCircle = watch('privateCircle');

  return (
    <form id={id} onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4 px-6 pb-4">
      <FormField label="Name" error={errors.name?.message}>
        <Input maxLength={50} placeholder="e.g. Golden Hour Club" {...register('name')} />
      </FormField>
      <div className="flex flex-col gap-1.5">
        <div className="flex items-center justify-between">
          <Label htmlFor={`${id}-description`}>Description</Label>
          <CharCount id={`${id}-description-count`} value={description} max={300} />
        </div>
        <Textarea
          id={`${id}-description`}
          maxLength={300}
          aria-describedby={`${id}-description-count`}
          placeholder="What’s this circle about? Who should join?"
          {...register('description')}
        />
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Category" error={errors.categoryId?.message}>
          <Select {...register('categoryId')}>
            <option value="">Choose…</option>
            {categories.map((i) => (
              <option key={i.id} value={i.id}>
                {i.emoji} {i.name}
              </option>
            ))}
          </Select>
        </FormField>
        <FormField label="Member limit" error={errors.maxMembers?.message} hint={`${minMembers}–150 people`}>
          <Input type="number" inputMode="numeric" min={minMembers} max={150} {...register('maxMembers')} />
        </FormField>
      </div>
      <div className="flex items-center justify-between gap-4 rounded-xl border border-line p-4">
        <div>
          <Label htmlFor={`${id}-private`}>Private circle</Label>
          <p className="text-[13px] text-muted">
            {privateCircle ? 'People request to join; admins approve. Posts are members-only.' : 'Anyone can join and read posts.'}
          </p>
        </div>
        <Switch id={`${id}-private`} checked={privateCircle} onCheckedChange={(v) => setValue('privateCircle', v, { shouldDirty: true })} />
      </div>
    </form>
  );
}
