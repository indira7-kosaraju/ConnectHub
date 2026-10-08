import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { toast } from '@/components/ui/toaster';
import { useCreateCircle } from '@/hooks/use-circles';
import { ApiRequestError } from '@/lib/api/client';
import { CircleForm, type CircleFormValues } from './circle-form';

export function CreateCircleDialog() {
  const [open, setOpen] = useState(false);
  const [serverErrors, setServerErrors] = useState<Partial<Record<keyof CircleFormValues, string>>>();
  const navigate = useNavigate();
  const create = useCreateCircle();

  const submit = (v: CircleFormValues) =>
    create.mutate(
      { ...v, description: v.description || undefined },
      {
        onSuccess: (circle) => {
          setOpen(false);
          toast.success(`${circle.name} is live. Invite your people!`);
          navigate(`/c/${circle.slug}`);
        },
        onError: (err) => {
          if (err instanceof ApiRequestError && Object.keys(err.fields).length) setServerErrors(err.fields);
          else toast.error(err instanceof ApiRequestError ? err.message : 'Could not create circle');
        },
      },
    );

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="accent">
          <Plus aria-hidden /> New circle
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Start a circle</DialogTitle>
          <DialogDescription>Small by design — up to 150 people around one shared interest.</DialogDescription>
        </DialogHeader>
        {open && (
          <CircleForm
            id="create-circle"
            defaultValues={{ name: '', description: '', categoryId: '', privateCircle: false, maxMembers: 150 }}
            onSubmit={submit}
            serverErrors={serverErrors}
          />
        )}
        <DialogFooter>
          <Button variant="ghost" onClick={() => setOpen(false)}>
            Cancel
          </Button>
          <Button type="submit" form="create-circle" loading={create.isPending}>
            Create circle
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
