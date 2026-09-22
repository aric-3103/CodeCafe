package com.bgremoval.CodeCafe.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.bgremoval.CodeCafe.exception.ModelException;
import com.bgremoval.CodeCafe.exception.ModelLoadException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Spring singleton that loads the U2Net ONNX model once at startup.
 *
 * Model resolution order:
 *   1. Classpath: /models/u2net.onnx  (bundled in the JAR, if present)
 *   2. Local cache: ~/.codecafe/models/u2net.onnx  (downloaded on first run)
 *
 * On first run the model is downloaded automatically from Hugging Face
 * (~176 MB, one-time) and cached locally so subsequent startups are fast.
 */
@Component
public class U2NetModelServiceImpl implements U2NetModelService {

    private static final int MODEL_SIZE  = 320;
    private static final int PIXEL_COUNT = MODEL_SIZE * MODEL_SIZE;
    private static final int TENSOR_SIZE = 3 * PIXEL_COUNT;

    private static final float[] MEAN = {0.485f, 0.456f, 0.406f};
    private static final float[] STD  = {0.229f, 0.224f, 0.225f};

    /** Classpath location (bundled JAR). */
    private static final String CLASSPATH_MODEL = "/models/u2net.onnx";

    /** Hugging Face direct download URL for the U2Net ONNX model (public repo, no login). */
    private static final String MODEL_DOWNLOAD_URL =
            "https://huggingface.co/Heliosoph/u2net-onnx/resolve/main/u2net.onnx";

    /** Fallback download URL (rembg project release). */
    private static final String MODEL_DOWNLOAD_URL_FALLBACK =
            "https://github.com/danielgatis/rembg/releases/download/v0.0.0/u2net.onnx";

    /** Local cache — persists across restarts so download only happens once. */
    private static final Path LOCAL_CACHE_PATH =
            Paths.get(System.getProperty("user.home"), ".codecafe", "models", "u2net.onnx");

    private OrtEnvironment env;
    private OrtSession     session;

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @PostConstruct
    public void loadModel() {
        try {
            byte[] modelBytes = resolveModelBytes();
            env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            opts.setIntraOpNumThreads(Runtime.getRuntime().availableProcessors());
            session = env.createSession(modelBytes, opts);
            System.out.println("[U2NetModelServiceImpl] Model loaded successfully ("
                    + modelBytes.length / (1024 * 1024) + " MB).");
        } catch (ModelLoadException e) {
            // Log clearly but DON'T crash the application context.
            // predict() will return a user-friendly FAILED status for each image.
            System.err.println(e.getMessage());
            System.err.println("[U2NetModelServiceImpl] Application will start, but background removal will be unavailable until the model is installed.");
        } catch (OrtException e) {
            System.err.println("[U2NetModelServiceImpl] ORT session creation failed: " + e.getMessage());
        }
    }

    /**
     * Returns raw model bytes. Tries classpath first, then local cache,
     * then downloads from Hugging Face.
     */
    private byte[] resolveModelBytes() {
        // 1. Try classpath (bundled resources)
        try (InputStream is = getClass().getResourceAsStream(CLASSPATH_MODEL)) {
            if (is != null) {
                System.out.println("[U2NetModelServiceImpl] Loading model from classpath.");
                return is.readAllBytes();
            }
        } catch (IOException e) {
            System.err.println("[U2NetModelServiceImpl] Classpath read failed: " + e.getMessage());
        }

        // 2. Try local cache
        if (Files.exists(LOCAL_CACHE_PATH)) {
            try {
                System.out.println("[U2NetModelServiceImpl] Loading model from cache: " + LOCAL_CACHE_PATH);
                return Files.readAllBytes(LOCAL_CACHE_PATH);
            } catch (IOException e) {
                System.err.println("[U2NetModelServiceImpl] Cache read failed: " + e.getMessage());
            }
        }

        // 3. Download and cache
        return downloadModel();
    }

    private byte[] downloadModel() {
        System.out.println("[U2NetModelServiceImpl] Model not found locally. Attempting download...");

        // Try each URL in order
        String[] urls = { MODEL_DOWNLOAD_URL, MODEL_DOWNLOAD_URL_FALLBACK };
        IOException lastError = null;

        for (String url : urls) {
            System.out.println("[U2NetModelServiceImpl] Trying: " + url);
            try {
                Files.createDirectories(LOCAL_CACHE_PATH.getParent());

                try (InputStream in  = URI.create(url).toURL().openStream();
                     OutputStream out = Files.newOutputStream(LOCAL_CACHE_PATH,
                             StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

                    byte[] buffer = new byte[8192];
                    long totalBytes = 0;
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        totalBytes += read;
                        if (totalBytes % (10L * 1024 * 1024) == 0) {
                            System.out.printf("[U2NetModelServiceImpl] Downloaded %d MB...%n",
                                    totalBytes / (1024 * 1024));
                        }
                    }
                }

                // Verify the file is non-empty (not an HTML error page)
                long fileSize = Files.size(LOCAL_CACHE_PATH);
                if (fileSize < 1_000_000) {
                    Files.deleteIfExists(LOCAL_CACHE_PATH);
                    throw new IOException("Downloaded file too small (" + fileSize + " bytes) — likely an error page, not the model.");
                }

                System.out.println("[U2NetModelServiceImpl] Download complete (" +
                        fileSize / (1024 * 1024) + " MB). Cached at: " + LOCAL_CACHE_PATH);
                return Files.readAllBytes(LOCAL_CACHE_PATH);

            } catch (IOException e) {
                lastError = e;
                System.err.println("[U2NetModelServiceImpl] Failed: " + e.getMessage());
                try { Files.deleteIfExists(LOCAL_CACHE_PATH); } catch (IOException ignored) { }
            }
        }

        // All URLs failed — give clear manual instructions
        String manualInstructions =
            "\n\n========== ACTION REQUIRED ==========\n" +
            "Automatic model download failed. Please download the model manually:\n\n" +
            "  1. Open this URL in your browser:\n" +
            "     https://huggingface.co/Heliosoph/u2net-onnx/resolve/main/u2net.onnx\n" +
            "     (or search: huggingface.co/Heliosoph/u2net-onnx)\n\n" +
            "  2. Save the file as:  u2net.onnx\n\n" +
            "  3. Move it to:  " + LOCAL_CACHE_PATH + "\n" +
            "     (create the folder if it doesn't exist)\n\n" +
            "  4. Restart the application.\n" +
            "=====================================\n";

        System.err.println(manualInstructions);
        throw new ModelLoadException("U2Net model download failed. " + manualInstructions, lastError);
    }

    @PreDestroy
    public void closeModel() {
        if (session != null) {
            try { session.close(); } catch (OrtException e) {
                System.err.println("[U2NetModelServiceImpl] Warning closing ORT session: " + e.getMessage());
            }
        }
        if (env != null) {
            env.close();
        }
    }

    // -----------------------------------------------------------------------
    // Inference
    // -----------------------------------------------------------------------

    @Override
    public float[][] predict(BufferedImage image) throws ModelException {
        if (session == null || env == null) {
            throw new ModelLoadException("U2Net model has not been loaded.");
        }

        BufferedImage resized = resizeTo320(image);
        float[] flat = toNCHWFloat(resized);
        long[] shape = {1L, 3L, MODEL_SIZE, MODEL_SIZE};

        try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(flat), shape);
             OrtSession.Result result = session.run(
                     java.util.Collections.singletonMap(
                             session.getInputNames().iterator().next(), inputTensor))) {

            float[][][][] raw = (float[][][][]) result.get(0).getValue();
            return raw[0][0];

        } catch (OrtException e) {
            throw new ModelException("U2Net inference failed: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static BufferedImage resizeTo320(BufferedImage src) {
        BufferedImage dst = new BufferedImage(MODEL_SIZE, MODEL_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dst.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(src, 0, 0, MODEL_SIZE, MODEL_SIZE, null);
        } finally {
            g2.dispose();
        }
        return dst;
    }

    private static float[] toNCHWFloat(BufferedImage image) {
        float[] flat = new float[TENSOR_SIZE];
        int rOffset = 0;
        int gOffset = PIXEL_COUNT;
        int bOffset = 2 * PIXEL_COUNT;

        for (int y = 0; y < MODEL_SIZE; y++) {
            for (int x = 0; x < MODEL_SIZE; x++) {
                int pixel = image.getRGB(x, y);
                int idx   = y * MODEL_SIZE + x;

                float r = ((pixel >> 16) & 0xFF) / 255.0f;
                float g = ((pixel >>  8) & 0xFF) / 255.0f;
                float b = ( pixel        & 0xFF) / 255.0f;

                flat[rOffset + idx] = (r - MEAN[0]) / STD[0];
                flat[gOffset + idx] = (g - MEAN[1]) / STD[1];
                flat[bOffset + idx] = (b - MEAN[2]) / STD[2];
            }
        }
        return flat;
    }
}
