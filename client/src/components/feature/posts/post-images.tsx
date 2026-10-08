import { useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Dialog, DialogContent, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import type { PostImage } from '@/lib/api/schema';
import { cn } from '@/lib/utils';
import { cdn, cdnSrcSet } from '@/lib/images-cdn';

/** 1 image: natural aspect (clamped); 2: side by side; 3: one tall + two; 4: 2×2. Opens a lightbox. */
export function PostImages({ images, alt }: { images: PostImage[]; alt: string }) {
  const [open, setOpen] = useState<number | null>(null);
  if (images.length === 0) return null;

  const single = images.length === 1;
  const first = images[0]!;
  // Reserve space up front (no layout shift): clamp very tall/wide images.
  const ratio = single ? Math.min(Math.max(first.width / first.height, 0.75), 1.9) : undefined;

  return (
    <>
      <div
        className={cn('mt-3 grid gap-1 overflow-hidden rounded-xl', !single && 'aspect-[4/3]', images.length >= 2 && 'grid-cols-2', images.length >= 3 && 'grid-rows-2')}
        style={ratio ? { aspectRatio: String(ratio) } : undefined}
      >
        {images.map((img, i) => (
          <button
            key={img.url}
            type="button"
            onClick={() => setOpen(i)}
            className={cn('relative overflow-hidden bg-surface-2', images.length === 3 && i === 0 && 'row-span-2')}
            aria-label={`Open image ${i + 1} of ${images.length}`}
          >
            <img
              src={cdn(img.url, single ? 1200 : 600)}
              srcSet={cdnSrcSet(img.url, single ? [480, 800, 1200] : [320, 600])}
              sizes={single ? '(min-width: 768px) 580px, 100vw' : '(min-width: 768px) 290px, 50vw'}
              alt=""
              width={img.width}
              height={img.height}
              loading="lazy"
              decoding="async"
              className="size-full object-cover transition-transform duration-300 hover:scale-[1.02]"
            />
          </button>
        ))}
      </div>
      <Dialog open={open !== null} onOpenChange={(o) => !o && setOpen(null)}>
        <DialogContent sheetOnMobile={false} className="max-w-4xl border-none bg-transparent shadow-none sm:max-w-4xl">
          <DialogTitle className="sr-only">{alt}</DialogTitle>
          {open !== null && (
            <div className="relative">
              <img src={images[open]!.url} alt={`${alt} — image ${open + 1}`} className="mx-auto max-h-[80dvh] w-auto rounded-xl" />
              {images.length > 1 && (
                <div className="mt-3 flex justify-center gap-2">
                  <Button size="icon" variant="outline" aria-label="Previous image" onClick={() => setOpen((open + images.length - 1) % images.length)}>
                    <ChevronLeft />
                  </Button>
                  <Button size="icon" variant="outline" aria-label="Next image" onClick={() => setOpen((open + 1) % images.length)}>
                    <ChevronRight />
                  </Button>
                </div>
              )}
            </div>
          )}
        </DialogContent>
      </Dialog>
    </>
  );
}
