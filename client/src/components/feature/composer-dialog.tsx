import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ImagePlus, X } from 'lucide-react';
import { AnimatePresence, motion } from 'framer-motion';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Avatar } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { Select } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { CharCount } from '@/components/ui/char-count';
import { ProfanityHint } from '@/components/ui/profanity-hint';
import { toast } from '@/components/ui/toaster';
import { useMyCircles } from '@/hooks/use-circles';
import { useCreatePost } from '@/hooks/use-posts';
import { ApiRequestError } from '@/lib/api/client';
import { IMAGE_ACCEPT } from '@/lib/images';
import { useAuthStore } from '@/stores/auth-store';
import { useUiStore } from '@/stores/ui-store';
import { cn } from '@/lib/utils';

const MAX_CHARS = 500;
const MAX_IMAGES = 4;
const MAX_BYTES = 10 * 1024 * 1024;

interface Picked {
  file: File;
  url: string;
}

export function ComposerDialog() {
  const navigate = useNavigate();
  const open = useUiStore((s) => s.composerOpen);
  const preset = useUiStore((s) => s.composerCircleId);
  const close = useUiStore((s) => s.closeComposer);
  const user = useAuthStore((s) => s.user);
  const circles = useMyCircles();
  const create = useCreatePost();
  const fileInput = useRef<HTMLInputElement>(null);

  const [text, setText] = useState('');
  const [circleId, setCircleId] = useState('');
  const [images, setImages] = useState<Picked[]>([]);

  // Pick the preset circle (or remember the last one) each time the composer opens.
  useEffect(() => {
    if (!open) return;
    const ids = circles.data?.map((c) => c.id) ?? [];
    setCircleId((current) => (preset && ids.includes(preset) ? preset : ids.includes(current) ? current : (ids[0] ?? '')));
  }, [open, preset, circles.data]);

  useEffect(() => () => images.forEach((i) => URL.revokeObjectURL(i.url)), [images]);

  const reset = () => {
    setText('');
    setImages([]);
  };

  const addFiles = (files: FileList | null) => {
    if (!files) return;
    const next = [...images];
    for (const file of Array.from(files)) {
      if (next.length >= MAX_IMAGES) {
        toast(`Up to ${MAX_IMAGES} photos per post`);
        break;
      }
      if (!IMAGE_ACCEPT.split(',').includes(file.type)) {
        toast.error(`${file.name}: choose a JPEG, PNG, WebP or GIF`);
        continue;
      }
      if (file.size > MAX_BYTES) {
        toast.error(`${file.name} is over 10 MB`);
        continue;
      }
      next.push({ file, url: URL.createObjectURL(file) });
    }
    setImages(next);
  };

  const canPost = !!circleId && (text.trim().length > 0 || images.length > 0) && text.length <= MAX_CHARS;
  const circleName = useMemo(() => circles.data?.find((c) => c.id === circleId)?.name, [circles.data, circleId]);

  const submit = () =>
    create.mutate(
      { circleId, content: text.trim(), images: images.map((i) => i.file) },
      {
        onSuccess: (post) => {
          toast.success(`Shared with ${post.circle.name}`, {
            action: { label: 'View', onClick: () => navigate(`/p/${post.id}`) },
          });
          reset();
          close();
        },
        onError: (err) => toast.error(err instanceof ApiRequestError ? err.message : 'Could not share your post'),
      },
    );

  const noCircles = circles.isSuccess && circles.data.length === 0;

  return (
    <Dialog open={open} onOpenChange={(o) => !o && !create.isPending && close()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Share with a circle</DialogTitle>
          <DialogDescription>Posts go to one circle — never to a follower count.</DialogDescription>
        </DialogHeader>

        {noCircles ? (
          <div className="px-6 pb-6 text-center">
            <p className="text-muted">Join a circle first — posts always belong to one.</p>
            <Button asChild className="mt-4" variant="accent" onClick={close}>
              <Link to="/discover">Discover circles</Link>
            </Button>
          </div>
        ) : (
          <>
            <div className="flex gap-3 px-6">
              <Avatar src={user?.avatarUrl} name={user?.displayName ?? user?.username} />
              <div className="min-w-0 flex-1">
                <label htmlFor="composer-circle" className="sr-only">
                  Circle
                </label>
                <Select
                  id="composer-circle"
                  value={circleId}
                  onChange={(e) => setCircleId(e.target.value)}
                  className="h-9 w-auto rounded-full border-line bg-surface-2 py-0 pl-3 text-[13px] font-medium"
                  required
                >
                  {circles.data?.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.category.emoji} {c.name}
                    </option>
                  ))}
                </Select>
                <Textarea
                  autoFocus
                  aria-label="Post text"
                  aria-describedby="composer-count"
                  placeholder={circleName ? `Share something with ${circleName}…` : 'What’s on your mind?'}
                  maxLength={MAX_CHARS}
                  value={text}
                  onChange={(e) => setText(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' && (e.metaKey || e.ctrlKey) && canPost) submit();
                  }}
                  className="mt-2 min-h-32 border-none bg-transparent px-0 text-[17px] focus-visible:ring-0"
                />
                <ProfanityHint text={text} />
              </div>
            </div>

            <AnimatePresence>
              {images.length > 0 && (
                <motion.ul
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  className="grid grid-cols-4 gap-2 px-6 pb-3"
                  aria-label="Attached photos"
                >
                  {images.map((img, i) => (
                    <li key={img.url} className="relative aspect-square overflow-hidden rounded-lg bg-surface-2">
                      <img src={img.url} alt={`Attachment ${i + 1}`} className="size-full object-cover" />
                      <button
                        type="button"
                        onClick={() => setImages(images.filter((x) => x !== img))}
                        className="absolute right-1 top-1 grid size-6 place-items-center rounded-full bg-[#1c1917]/70 text-white"
                        aria-label={`Remove attachment ${i + 1}`}
                      >
                        <X className="size-3.5" aria-hidden />
                      </button>
                    </li>
                  ))}
                </motion.ul>
              )}
            </AnimatePresence>

            <DialogFooter className="justify-between">
              <div className="flex items-center gap-2">
                <Button
                  variant="ghost"
                  size="icon"
                  aria-label="Add photos"
                  disabled={images.length >= MAX_IMAGES}
                  onClick={() => fileInput.current?.click()}
                >
                  <ImagePlus />
                </Button>
                <span className={cn('text-xs text-muted', images.length >= MAX_IMAGES && 'text-accent-ink')}>
                  {images.length}/{MAX_IMAGES}
                </span>
                <input
                  ref={fileInput}
                  type="file"
                  multiple
                  accept={IMAGE_ACCEPT}
                  className="sr-only"
                  tabIndex={-1}
                  aria-hidden
                  onChange={(e) => {
                    addFiles(e.target.files);
                    e.target.value = '';
                  }}
                />
              </div>
              <div className="flex items-center gap-3">
                <CharCount id="composer-count" value={text} max={MAX_CHARS} />
                <Button variant="accent" disabled={!canPost} loading={create.isPending} onClick={submit}>
                  Post
                </Button>
              </div>
            </DialogFooter>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
