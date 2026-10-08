import { useEffect, useRef, useState } from 'react';
import { Camera, Type } from 'lucide-react';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { CharCount } from '@/components/ui/char-count';
import { ProfanityHint } from '@/components/ui/profanity-hint';
import { useCreateMoment } from '@/hooks/use-moments';
import type { MomentBackground } from '@/lib/api/schema';
import { IMAGE_ACCEPT, pickImage } from '@/lib/images';
import { useUiStore } from '@/stores/ui-store';
import { cn } from '@/lib/utils';

const BACKGROUNDS: MomentBackground[] = ['sunset', 'ocean', 'forest', 'dusk', 'ember', 'plum'];
const MAX_TEXT = 200;

export function MomentComposer() {
  const open = useUiStore((s) => s.momentComposerOpen);
  const setOpen = useUiStore((s) => s.setMomentComposerOpen);
  const create = useCreateMoment();
  const [mode, setMode] = useState<'text' | 'image'>('text');
  const [text, setText] = useState('');
  const [background, setBackground] = useState<MomentBackground>('sunset');
  const [photo, setPhoto] = useState<{ file: File; url: string } | null>(null);
  const [caption, setCaption] = useState('');
  const fileInput = useRef<HTMLInputElement>(null);

  useEffect(() => () => {
    if (photo) URL.revokeObjectURL(photo.url);
  }, [photo]);

  const reset = () => {
    setText('');
    setCaption('');
    setPhoto(null);
    setMode('text');
  };

  const canShare = mode === 'text' ? text.trim().length > 0 : !!photo;
  const share = () => {
    const payload =
      mode === 'text'
        ? ({ kind: 'text', text: text.trim(), background } as const)
        : ({ kind: 'image', image: photo!.file, caption: caption.trim() } as const);
    create.mutate(payload, {
      onSuccess: () => {
        reset();
        setOpen(false);
      },
    });
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !create.isPending && setOpen(o)}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Share a moment</DialogTitle>
          <DialogDescription>Visible to your circles for 24 hours, then it moves to your private Rewind.</DialogDescription>
        </DialogHeader>

        <div className="px-6">
          <div role="radiogroup" aria-label="Moment type" className="mb-4 inline-flex rounded-full border border-line bg-surface-2 p-1">
            {(
              [
                ['text', 'Text', Type],
                ['image', 'Photo', Camera],
              ] as const
            ).map(([value, label, Icon]) => (
              <button
                key={value}
                type="button"
                role="radio"
                aria-checked={mode === value}
                onClick={() => {
                  setMode(value);
                  if (value === 'image' && !photo) fileInput.current?.click();
                }}
                className={cn(
                  'inline-flex items-center gap-1.5 rounded-full px-4 py-1.5 text-sm transition-colors',
                  mode === value ? 'bg-surface text-ink shadow-card' : 'text-muted hover:text-ink',
                )}
              >
                <Icon className="size-4" aria-hidden /> {label}
              </button>
            ))}
          </div>

          {/* 9:16 live preview */}
          <div className="relative mx-auto aspect-[9/16] max-h-[52dvh] overflow-hidden rounded-2xl">
            {mode === 'text' ? (
              <div className={cn('grid size-full place-items-center p-6', `moment-bg-${background}`)}>
                <textarea
                  autoFocus
                  aria-label="Moment text"
                  aria-describedby="moment-count"
                  placeholder="Say something…"
                  maxLength={MAX_TEXT}
                  value={text}
                  onChange={(e) => setText(e.target.value)}
                  className="w-full resize-none bg-transparent text-center font-serif text-2xl font-semibold leading-snug text-white placeholder:text-white/60 focus:outline-none"
                  rows={5}
                />
              </div>
            ) : photo ? (
              <img src={photo.url} alt="Selected moment" className="size-full bg-[#121110] object-contain" />
            ) : (
              <button
                type="button"
                onClick={() => fileInput.current?.click()}
                className="grid size-full place-items-center bg-surface-2 text-muted"
              >
                <span className="flex flex-col items-center gap-2">
                  <Camera className="size-8" aria-hidden /> Choose a photo
                </span>
              </button>
            )}
          </div>

          {mode === 'text' ? (
            <div className="mt-4 flex items-center justify-between">
              <div role="radiogroup" aria-label="Background" className="flex gap-2">
                {BACKGROUNDS.map((b) => (
                  <button
                    key={b}
                    type="button"
                    role="radio"
                    aria-checked={background === b}
                    aria-label={b}
                    onClick={() => setBackground(b)}
                    className={cn('size-7 rounded-full transition-transform', `moment-bg-${b}`, background === b && 'scale-110 ring-2 ring-ink ring-offset-2 ring-offset-surface')}
                  />
                ))}
              </div>
              <CharCount id="moment-count" value={text} max={MAX_TEXT} />
            </div>
          ) : (
            photo && (
              <div className="mt-4 flex gap-2">
                <Input aria-label="Caption (optional)" placeholder="Add a caption (optional)" maxLength={200} value={caption} onChange={(e) => setCaption(e.target.value)} />
                <Button variant="outline" onClick={() => fileInput.current?.click()}>
                  Change
                </Button>
              </div>
            )
          )}
          <ProfanityHint text={mode === 'text' ? text : caption} className="mt-2 text-[13px] text-accent-ink" />
          <input
            ref={fileInput}
            type="file"
            accept={IMAGE_ACCEPT}
            className="sr-only"
            tabIndex={-1}
            aria-hidden
            onChange={(e) => {
              const file = e.target.files?.[0];
              const url = pickImage(file);
              if (file && url) {
                setPhoto({ file, url });
                setMode('image');
              }
              e.target.value = '';
            }}
          />
        </div>

        <DialogFooter className="mt-4">
          <Button variant="ghost" onClick={() => setOpen(false)}>
            Cancel
          </Button>
          <Button variant="accent" disabled={!canShare} loading={create.isPending} onClick={share}>
            Share moment
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
