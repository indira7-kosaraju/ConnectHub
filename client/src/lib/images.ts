import { toast } from '@/components/ui/toaster';

export const IMAGE_ACCEPT = 'image/jpeg,image/png,image/webp,image/gif';
const MAX_BYTES = 10 * 1024 * 1024;

/** Validates a picked file and returns an object URL for the cropper, or null (with a toast). */
export function pickImage(file: File | undefined): string | null {
  if (!file) return null;
  if (!IMAGE_ACCEPT.split(',').includes(file.type)) {
    toast.error('Choose a JPEG, PNG, WebP or GIF image');
    return null;
  }
  if (file.size > MAX_BYTES) {
    toast.error('Images must be 10 MB or smaller');
    return null;
  }
  return URL.createObjectURL(file);
}
