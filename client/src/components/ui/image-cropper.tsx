import { useCallback, useState } from 'react';
import Cropper, { type Area } from 'react-easy-crop';
import { Minus, Plus } from 'lucide-react';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from './dialog';
import { Button } from './button';

interface ImageCropperProps {
  /** Object URL of the picked image; the dialog is open while this is set. */
  src: string | null;
  aspect?: number;
  shape?: 'round' | 'rect';
  title?: string;
  /** Longest output edge in px. */
  maxOutput?: number;
  busy?: boolean;
  onCancel: () => void;
  onCropped: (blob: Blob) => void;
}

async function cropToBlob(src: string, area: Area, maxOutput: number): Promise<Blob> {
  const image = await new Promise<HTMLImageElement>((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error('Could not load image'));
    img.src = src;
  });
  const scale = Math.min(1, maxOutput / Math.max(area.width, area.height));
  const canvas = document.createElement('canvas');
  canvas.width = Math.round(area.width * scale);
  canvas.height = Math.round(area.height * scale);
  const ctx = canvas.getContext('2d');
  if (!ctx) throw new Error('Canvas unsupported');
  ctx.imageSmoothingQuality = 'high';
  ctx.drawImage(image, area.x, area.y, area.width, area.height, 0, 0, canvas.width, canvas.height);
  return new Promise((resolve, reject) =>
    canvas.toBlob((b) => (b ? resolve(b) : reject(new Error('Crop failed'))), 'image/jpeg', 0.92),
  );
}

export function ImageCropper({
  src,
  aspect = 1,
  shape = 'round',
  title = 'Adjust your photo',
  maxOutput = 1000,
  busy = false,
  onCancel,
  onCropped,
}: ImageCropperProps) {
  const [crop, setCrop] = useState({ x: 0, y: 0 });
  const [zoom, setZoom] = useState(1);
  const [area, setArea] = useState<Area | null>(null);
  const onCropComplete = useCallback((_: Area, pixels: Area) => setArea(pixels), []);

  const confirm = async () => {
    if (!src || !area) return;
    onCropped(await cropToBlob(src, area, maxOutput));
  };

  return (
    <Dialog open={!!src} onOpenChange={(open) => !open && !busy && onCancel()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>Drag to reposition. Use the slider or scroll to zoom.</DialogDescription>
        </DialogHeader>
        <div className="relative mx-6 h-72 overflow-hidden rounded-xl bg-surface-2">
          {src && (
            <Cropper
              image={src}
              crop={crop}
              zoom={zoom}
              aspect={aspect}
              cropShape={shape}
              showGrid={false}
              onCropChange={setCrop}
              onZoomChange={setZoom}
              onCropComplete={onCropComplete}
            />
          )}
        </div>
        <div className="flex items-center gap-3 px-6 py-4">
          <Minus className="size-4 text-muted" aria-hidden />
          <input
            type="range"
            min={1}
            max={3}
            step={0.01}
            value={zoom}
            onChange={(e) => setZoom(Number(e.target.value))}
            aria-label="Zoom"
            className="h-1 flex-1 cursor-pointer accent-[var(--accent)]"
          />
          <Plus className="size-4 text-muted" aria-hidden />
        </div>
        <DialogFooter>
          <Button variant="ghost" onClick={onCancel} disabled={busy}>
            Cancel
          </Button>
          <Button variant="primary" onClick={confirm} loading={busy} disabled={!area}>
            Save photo
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
