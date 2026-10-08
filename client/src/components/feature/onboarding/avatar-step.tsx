import { useEffect, useRef, useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Camera } from 'lucide-react';
import { Avatar } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { ImageCropper } from '@/components/ui/image-cropper';
import { toast } from '@/components/ui/toaster';
import { meApi } from '@/lib/api/auth';
import { ApiRequestError } from '@/lib/api/client';
import { useAuthStore } from '@/stores/auth-store';
import { StepFrame } from './step-frame';

const MAX_BYTES = 10 * 1024 * 1024;
const ACCEPT = 'image/jpeg,image/png,image/webp,image/gif';

export function AvatarStep({ onNext, onBack }: { onNext: () => void; onBack: () => void }) {
  const user = useAuthStore((s) => s.user);
  const setUser = useAuthStore((s) => s.setUser);
  const fileInput = useRef<HTMLInputElement>(null);
  const [pickedUrl, setPickedUrl] = useState<string | null>(null);

  useEffect(() => () => {
    if (pickedUrl) URL.revokeObjectURL(pickedUrl);
  }, [pickedUrl]);

  const upload = useMutation({
    mutationFn: (blob: Blob) => meApi.uploadAvatar(blob),
    onSuccess: (me) => {
      setUser(me);
      setPickedUrl(null);
      toast.success('Looking good!');
    },
    onError: (err) => toast.error(err instanceof ApiRequestError ? err.message : 'Upload failed'),
  });

  const onPick = (file: File | undefined) => {
    if (!file) return;
    if (!ACCEPT.split(',').includes(file.type)) {
      toast.error('Choose a JPEG, PNG, WebP or GIF image');
      return;
    }
    if (file.size > MAX_BYTES) {
      toast.error('Images must be 10 MB or smaller');
      return;
    }
    setPickedUrl(URL.createObjectURL(file));
  };

  const hasAvatar = !!user?.avatarUrl;

  return (
    <StepFrame
      eyebrow="Step 2 · Your face"
      title="Add a photo"
      description="Circles feel warmer when people can put a face to a name. Totally optional."
      footer={
        <>
          <Button variant="ghost" onClick={onBack}>
            Back
          </Button>
          <Button size="lg" variant={hasAvatar ? 'primary' : 'outline'} onClick={onNext}>
            {hasAvatar ? 'Continue' : 'Skip for now'}
          </Button>
        </>
      }
    >
      <div className="flex flex-col items-center gap-6 py-4">
        <button
          type="button"
          onClick={() => fileInput.current?.click()}
          className="group relative rounded-full focus-visible:outline-offset-4"
          aria-label={hasAvatar ? 'Change photo' : 'Choose a photo'}
        >
          <Avatar size="xl" src={user?.avatarUrl} name={user?.displayName ?? user?.username} className="size-36 text-4xl" />
          <span className="absolute bottom-1 right-1 grid size-10 place-items-center rounded-full border-4 border-bg bg-accent text-[#1c1917] transition-transform group-hover:scale-105">
            <Camera className="size-4" aria-hidden />
          </span>
        </button>
        <Button variant="outline" onClick={() => fileInput.current?.click()}>
          {hasAvatar ? 'Choose a different photo' : 'Choose a photo'}
        </Button>
        <input
          ref={fileInput}
          type="file"
          accept={ACCEPT}
          className="sr-only"
          tabIndex={-1}
          aria-hidden
          onChange={(e) => {
            onPick(e.target.files?.[0]);
            e.target.value = '';
          }}
        />
      </div>
      <ImageCropper
        src={pickedUrl}
        busy={upload.isPending}
        onCancel={() => setPickedUrl(null)}
        onCropped={(blob) => upload.mutate(blob)}
      />
    </StepFrame>
  );
}
