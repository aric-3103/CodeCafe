package com.bgremoval.CodeCafe.service;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Upsamples the U2Net 320×320 float mask to the original image dimensions
 * using bilinear interpolation, then applies it as the alpha channel of an
 * ARGB PNG, preserving the original RGB channels exactly.
 *
 * <p>Requirements: 1.2
 */
@Component
public class AlphaMaskApplicator {

    /**
     * Applies the given float mask to the source image as an alpha channel.
     *
     * @param original   source image at its native resolution
     * @param mask       float[320][320] saliency mask from U2Net (values in [0,1])
     * @return PNG-encoded bytes of the ARGB result image
     * @throws IOException if PNG encoding fails
     */
    public byte[] apply(BufferedImage original, float[][] mask) throws IOException {
        int w = original.getWidth();
        int h = original.getHeight();

        // --- 1. Upsample the 320×320 mask to original dimensions ----------------
        // Build a greyscale BufferedImage from the mask floats
        BufferedImage maskImg = new BufferedImage(320, 320, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < 320; y++) {
            for (int x = 0; x < 320; x++) {
                int alpha = Math.min(255, Math.max(0, Math.round(mask[y][x] * 255f)));
                // For TYPE_BYTE_GRAY the single component is stored in the red slot of getRGB
                int grey = (alpha << 16) | (alpha << 8) | alpha;
                maskImg.setRGB(x, y, 0xFF000000 | grey);
            }
        }

        // Scale mask image to original dimensions using bilinear interpolation
        BufferedImage upsampledMask = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        if (w != 320 || h != 320) {
            double scaleX = (double) w / 320.0;
            double scaleY = (double) h / 320.0;
            AffineTransform at = AffineTransform.getScaleInstance(scaleX, scaleY);
            AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
            op.filter(maskImg, upsampledMask);
        } else {
            upsampledMask = maskImg;
        }

        // --- 2. Build ARGB result image ------------------------------------------
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb   = original.getRGB(x, y);
                // Extract grey value from the upsampled mask (red channel of the greyscale pixel)
                int maskPixel = upsampledMask.getRGB(x, y);
                int alpha = (maskPixel >> 16) & 0xFF;
                // Preserve original RGB, replace alpha
                result.setRGB(x, y, (alpha << 24) | (rgb & 0x00FFFFFF));
            }
        }

        // --- 3. Encode as PNG bytes -----------------------------------------------
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (!ImageIO.write(result, "PNG", baos)) {
            throw new IOException("ImageIO could not find a PNG writer");
        }
        return baos.toByteArray();
    }
}
