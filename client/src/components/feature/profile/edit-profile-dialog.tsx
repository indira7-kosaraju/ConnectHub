import { useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Camera, ImagePlus, Trash2 } from 'lucide-react';
import { Avatar } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { FormField } from '@/components/ui/form-field';
import { ImageCropper } from '@/components/ui/image-cropper';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { CharCount } from '@/components/ui/char-count';
import { toast } from '@/components/ui/toaster';
import { CircleCover } from '@/components/feature/circles/circle-cover';
import { useProfileMutations } from '@/hooks/use-profile';
import { ApiRequestError } from '@/lib/api/client';
import { IMAGE_ACCEPT, pickImage } from '@/lib/images';
import { useAuthStore } from '@/stores/auth-store';

const schema = z.object({
  displayName: z.string().trim().min(1, 'Your name can’t be empty').max(60),
  bio: z.string().max(160, '160 characters max'),
  location: z.string().max(80),
  website: z.string().max(255),
});
type Values = z.infer<typeof schema>;

type Cropping = { kind: 'avatar' | 'cover'; url: string } | null;

export function EditProfileDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (o: boolean) => void }) {
  const user = useAuthStore((s) => s.user);
  const { update, avatar, cover, removeCover } = useProfileMutations();
  const [cropping, setCropping] = useState<Cropping>(null);
  const avatarInput = useRef<HTMLInputElement>(null);
  const coverInput = useRef<HTMLInputElement>(null);
  const { register, handleSubmit, watch, setError, formState: { errors } } = useForm<Values>({
    resolver: zodResolver(schema),
    values: {
      displayName: user?.displayName ?? '',
      bio: user?.bio ?? '',
      location: user?.location ?? '',
      website: user?.website ?? '',
    },
  });

  if (!user) return null;

  const submit = handleSubmit((v) =>
    update.mutate(v, {
      onSuccess: () => {
        onOpenChange(false);
        toast.success('Profile updated');
      },
      onError: (err) => {
        if (err instanceof ApiRequestError) {
          const fields = Object.entries(err.fields);
          if (fields.length) fields.forEach(([f, m]) => setError(f as keyof Values, { message: m }));
          else if (/website/i.test(err.message)) setError('website', { message: err.message });
          else toast.error(err.message);
        } else toast.error('Could not save your profile');
      },
    }),
  );

  const onCropped = (blob: Blob) => {
    const m = cropping?.kind === 'avatar' ? avatar : cover;
    m.mutate(blob, {
      onSuccess: () => {
        setCropping(null);
        toast.success(cropping?.kind === 'avatar' ? 'Photo updated' : 'Cover updated');
      },
      onError: (err) => toast.error(err instanceof ApiRequestError ? err.message : 'Upload failed'),
    });
  };

  const fileInput = (kind: 'avatar' | 'cover', ref: React.RefObject<HTMLInputElement>) => (
    <input
      ref={ref}
      type="file"
      accept={IMAGE_ACCEPT}
      className="sr-only"
      tabIndex={-1}
      aria-hidden
      onChange={(e) => {
        const url = pickImage(e.target.files?.[0]);
        if (url) setCropping({ kind, url });
        e.target.value = '';
      }}
    />
  );

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>Edit profile</DialogTitle>
          <DialogDescription>No follower counts here — just you.</DialogDescription>
        </DialogHeader>

        <div className="px-6">
          <div className="relative h-28 overflow-hidden rounded-xl sm:h-36">
            <CircleCover seed={user.username ?? user.id} src={user.coverUrl} />
            <div className="absolute right-2 top-2 flex gap-1.5">
              <Button size="sm" variant="outline" className="bg-surface/90" onClick={() => coverInput.current?.click()}>
                <ImagePlus aria-hidden /> Cover
              </Button>
              {user.coverUrl && (
                <Button size="icon-sm" variant="outline" className="bg-surface/90" aria-label="Remove cover" onClick={() => removeCover.mutate()}>
                  <Trash2 />
                </Button>
              )}
            </div>
          </div>
          <button
            type="button"
            onClick={() => avatarInput.current?.click()}
            className="group relative -mt-10 ml-4 block rounded-full border-4 border-surface"
            aria-label="Change profile photo"
          >
            <Avatar size="xl" src={user.avatarUrl} name={user.displayName ?? user.username} className="size-20" />
            <span className="absolute inset-0 grid place-items-center rounded-full bg-[#1c1917]/0 text-white opacity-0 transition group-hover:bg-[#1c1917]/40 group-hover:opacity-100">
              <Camera className="size-5" aria-hidden />
            </span>
          </button>
          {fileInput('cover', coverInput)}
          {fileInput('avatar', avatarInput)}
        </div>

        <form id="edit-profile" onSubmit={submit} noValidate className="space-y-4 px-6 py-4">
          <FormField label="Name" error={errors.displayName?.message}>
            <Input maxLength={60} autoComplete="name" {...register('displayName')} />
          </FormField>
          <div className="flex flex-col gap-1.5">
            <div className="flex items-center justify-between">
              <Label htmlFor="profile-bio">Bio</Label>
              <CharCount id="profile-bio-count" value={watch('bio')} max={160} />
            </div>
            <Textarea id="profile-bio" maxLength={160} rows={3} aria-describedby="profile-bio-count" placeholder="A line or two about you" {...register('bio')} />
            {errors.bio && <p className="text-[13px] text-danger">{errors.bio.message}</p>}
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <FormField label="Location" error={errors.location?.message}>
              <Input maxLength={80} placeholder="City, country" {...register('location')} />
            </FormField>
            <FormField label="Website" error={errors.website?.message}>
              <Input maxLength={255} inputMode="url" placeholder="yoursite.com" {...register('website')} />
            </FormField>
          </div>
        </form>

        <DialogFooter>
          <Button variant="ghost" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button type="submit" form="edit-profile" loading={update.isPending}>
            Save
          </Button>
        </DialogFooter>

        <ImageCropper
          src={cropping?.url ?? null}
          aspect={cropping?.kind === 'cover' ? 3 : 1}
          shape={cropping?.kind === 'cover' ? 'rect' : 'round'}
          title={cropping?.kind === 'cover' ? 'Adjust your cover' : 'Adjust your photo'}
          maxOutput={cropping?.kind === 'cover' ? 1500 : 1000}
          busy={avatar.isPending || cover.isPending}
          onCancel={() => setCropping(null)}
          onCropped={onCropped}
        />
      </DialogContent>
    </Dialog>
  );
}
