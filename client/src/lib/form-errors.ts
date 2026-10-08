import type { FieldValues, Path, UseFormSetError } from 'react-hook-form';
import { ApiRequestError } from '@/lib/api/client';
import { toast } from '@/components/ui/toaster';

/**
 * Maps server validation errors onto form fields; anything else becomes a toast.
 * Returns true if at least one field error was applied.
 */
export function applyServerErrors<T extends FieldValues>(error: unknown, setError: UseFormSetError<T>, fields: Path<T>[]): boolean {
  if (error instanceof ApiRequestError) {
    let applied = false;
    for (const [field, message] of Object.entries(error.fields)) {
      if ((fields as string[]).includes(field)) {
        setError(field as Path<T>, { type: 'server', message });
        applied = true;
      }
    }
    if (!applied) toast.error(error.message);
    return applied;
  }
  toast.error('Something went wrong');
  return false;
}
