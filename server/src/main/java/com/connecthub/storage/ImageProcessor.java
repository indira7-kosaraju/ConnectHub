package com.connecthub.storage;

import com.connecthub.common.exception.BadRequestException;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * Validates and normalises uploads: allow-listed types only, size and pixel limits (guards
 * against decompression bombs), resized with Thumbnailator and re-encoded as JPEG — which also
 * strips EXIF metadata such as GPS location.
 */
@Component
public class ImageProcessor {

    public static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final long MAX_PIXELS = 40_000_000L;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> ALLOWED_FORMATS = Set.of("jpeg", "jpg", "png", "webp", "gif");

    public ProcessedImage process(MultipartFile file, ImagePreset preset) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No image provided");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("Images must be 10 MB or smaller");
        }
        if (file.getContentType() == null || !ALLOWED_TYPES.contains(file.getContentType().toLowerCase())) {
            throw new BadRequestException("Only JPEG, PNG, WebP or GIF images are allowed");
        }
        try {
            byte[] bytes = file.getBytes();
            BufferedImage source = decode(bytes);
            BufferedImage rgb = flattenToRgb(source);

            var builder = Thumbnails.of(rgb);
            if (preset.crop) {
                // Fill the preset box and crop the centre. If the source is smaller than the box,
                // shrink the box (keeping its aspect ratio) so we never upscale.
                double fit = Math.min(1.0, Math.min((double) rgb.getWidth() / preset.width, (double) rgb.getHeight() / preset.height));
                int boxW = Math.max(1, (int) Math.round(preset.width * fit));
                int boxH = Math.max(1, (int) Math.round(preset.height * fit));
                builder.size(boxW, boxH).crop(Positions.CENTER);
            } else {
                builder.size(Math.min(preset.width, rgb.getWidth()), Math.min(preset.height, rgb.getHeight()))
                        .keepAspectRatio(true);
            }
            BufferedImage result = builder.asBufferedImage();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(result).scale(1.0).outputFormat("jpg").outputQuality(0.85).toOutputStream(out);
            return new ProcessedImage(out.toByteArray(), result.getWidth(), result.getHeight());
        } catch (IOException e) {
            throw new BadRequestException("That file couldn't be read as an image");
        }
    }

    private BufferedImage decode(byte[] bytes) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new BadRequestException("That file isn't a supported image");
            }
            ImageReader reader = readers.next();
            try {
                if (!ALLOWED_FORMATS.contains(reader.getFormatName().toLowerCase())) {
                    throw new BadRequestException("Only JPEG, PNG, WebP or GIF images are allowed");
                }
                reader.setInput(in, true, true);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > MAX_PIXELS) {
                    throw new BadRequestException("Image dimensions are too large");
                }
                return reader.read(0); // first frame only for GIFs
            } finally {
                reader.dispose();
            }
        }
    }

    /** JPEG has no alpha: paint transparent images onto white instead of black. */
    private static BufferedImage flattenToRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) {
            return src;
        }
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, src.getWidth(), src.getHeight());
            g.drawImage(src, 0, 0, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }
}
