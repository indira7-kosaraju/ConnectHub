import { useRef, useState } from 'react';
import { ImagePlus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { ImageCropper } from '@/components/ui/image-cropper';
import { toast } from '@/components/ui/toaster';
import { useUpdateCircle } from '@/hooks/use-circles';
import { ApiRequestError } from '@/lib/api/client';
import type { CircleDetail } from '@/lib/api/schema';
import { pickImage } from '@/lib/images';
import { CircleCover } from './circle-cover';
import { CircleForm, type CircleFormValues } from './circle-form';

export function EditCircleDialog({ circle, open, onOpenChange }: { circle: CircleDetail; open: boolean; onOpenChange: (o: boolean) => void }) {
  const { update, cover } = useUpdateCircle(circle.slug);
  const [serverErrors, setServerErrors] = useState<Partial<Record<keyof CircleFormValues, string>>>();
  const [picked, setPicked] = useState<string | null>(null);
  const fileInput = useRef<HTMLInputElement>(null);

  const submit = (v: CircleFormValues) =>
    update.mutate(v, {
      onSuccess: () => {
        onOpenChange(false);
        toast.success('Circle updated');
      },
      onError: (err) => {
        if (err instanceof ApiRequestError && Object.keys(err.fields).length) setServerErrors(err.fields);
        else toast.error(err instanceof ApiRequestError ? err.message : 'Could not save changes');
      },
    });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Edit circle</DialogTitle>
          <DialogDescription>The link (/c/{circle.slug}) stays the same.</DialogDescription>
        </DialogHeader>
        <div className="px-6 pb-4">
          <button
            type="button"
            onClick={() => fileInput.current?.click()}
            className="group relative block h-24 w-full overflow-hidden rounded-xl"
            aria-label="Change cover image"
          >
            <CircleCover seed={circle.slug} src={circle.coverUrl} />
            <span className="absolute inset-0 grid place-items-center bg-[#1c1917]/0 text-sm font-medium text-white opacity-0 transition group-hover:bg-[#1c1917]/40 group-hover:opacity-100 group-focus-visible:bg-[#1c1917]/40 group-focus-visible:opacity-100">
              <span className="inline-flex items-center gap-2">
                <ImagePlus className="size-4" aria-hidden /> Change cover
              </span>
            </span>
          </button>
          <input
            ref={fileInput}
            type="file"
            accept="image/jpeg,image/png,image/webp,image/gif"
            className="sr-only"
            tabIndex={-1}
            aria-hidden
            onChange={(e) => {
              const url = pickImage(e.target.files?.[0]);
              if (url) setPicked(url);
              e.target.value = '';
            }}
          />
        </div>
        {open && (
          <CircleForm
            id="edit-circle"
            minMembers={Math.max(2, circle.memberCount)}
            defaultValues={{
              name: circle.name,
              description: circle.description ?? '',
              categoryId: circle.category.id,
              privateCircle: circle.privateCircle,
              maxMembers: circle.maxMembers,
            }}
            onSubmit={submit}
            serverErrors={serverErrors}
          />
        )}
        <DialogFooter>
          <Button variant="ghost" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button type="submit" form="edit-circle" loading={update.isPending}>
            Save changes
          </Button>
        </DialogFooter>
        <ImageCropper
          src={picked}
          aspect={3}
          shape="rect"
          title="Adjust the cover"
          maxOutput={1500}
          busy={cover.isPending}
          onCancel={() => setPicked(null)}
          onCropped={(blob) =>
            cover.mutate(blob, {
              onSuccess: () => {
                setPicked(null);
                toast.success('Cover updated');
              },
            })
          }
        />
      </DialogContent>
    </Dialog>
  );
}
