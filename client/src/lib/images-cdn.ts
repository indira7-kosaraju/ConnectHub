/**
 * Cloudinary delivers any stored image resized and re-encoded on the fly. For Cloudinary URLs we
 * request an automatic format (AVIF/WebP) and quality at the width actually needed; other URLs
 * (local dev uploads, seed placeholders) pass through unchanged.
 */
const CLOUDINARY = /^(https:\/\/res\.cloudinary\.com\/[^/]+\/image\/upload\/)(.*)$/;

export function cdn(url: string, width: number): string {
  const m = CLOUDINARY.exec(url);
  return m ? `${m[1]}f_auto,q_auto,c_limit,w_${Math.round(width)}/${m[2]}` : url;
}

/** srcset for responsive images; empty for non-Cloudinary URLs. */
export function cdnSrcSet(url: string, widths: number[]): string | undefined {
  return CLOUDINARY.test(url) ? widths.map((w) => `${cdn(url, w)} ${w}w`).join(', ') : undefined;
}
