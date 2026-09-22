package com.bgremoval.CodeCafe.service;

import com.bgremoval.CodeCafe.domain.ImageRecord;
import com.bgremoval.CodeCafe.domain.ProcessingStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.UUID;

/**
 * Default implementation of {@link BackgroundRemovalService}.
 *
 * <p>Annotated with {@code @Async} so each call executes in the configured
 * {@code bgRemovalTaskExecutor} thread pool (see {@code AsyncConfig}).
 * All exceptions are caught and written into the record as {@code FAILED} —
 * nothing propagates out of the method.
 *
 * <p>Requirements: 1.2, 1.3, 1.4, 8.1, 8.2, 8.3
 */
@Service
public class BackgroundRemovalServiceImpl implements BackgroundRemovalService {

    private final ProcessingSessionStore sessionStore;
    private final U2NetModelService      modelService;
    private final AlphaMaskApplicator    maskApplicator;

    public BackgroundRemovalServiceImpl(ProcessingSessionStore sessionStore,
                                        U2NetModelService modelService,
                                        AlphaMaskApplicator maskApplicator) {
        this.sessionStore   = sessionStore;
        this.modelService   = modelService;
        this.maskApplicator = maskApplicator;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Runs in the {@code bgRemovalTaskExecutor} thread pool.
     */
    @Override
    @Async("bgRemovalTaskExecutor")
    public void process(UUID recordId) {
        ImageRecord record = null;
        try {
            // 1. Fetch record — if missing, log and bail (nothing to update)
            record = sessionStore.findById(recordId).orElse(null);
            if (record == null) {
                System.err.println("[BackgroundRemovalService] Record not found: " + recordId);
                return;
            }

            // 2. Transition to PROCESSING
            record.setStatus(ProcessingStatus.PROCESSING);
            sessionStore.updateRecord(record);

            // 3. Decode input bytes
            BufferedImage sourceImage;
            try (ByteArrayInputStream bais = new ByteArrayInputStream(record.getInputBytes())) {
                sourceImage = ImageIO.read(bais);
            }
            if (sourceImage == null) {
                throw new IllegalStateException(
                        "ImageIO could not decode the file: " + record.getOriginalFilename());
            }

            // 4. Run U2Net inference
            float[][] mask = modelService.predict(sourceImage);

            // 5. Apply alpha mask and encode to PNG
            byte[] outputBytes = maskApplicator.apply(sourceImage, mask);

            // 6. Transition to COMPLETED
            record.setOutputBytes(outputBytes);
            record.setStatus(ProcessingStatus.COMPLETED);
            record.setCompletedAt(Instant.now());
            sessionStore.updateRecord(record);

        } catch (Exception ex) {
            // Top-level catch: always transition the record to FAILED
            if (record != null) {
                String msg = buildErrorMessage(record.getOriginalFilename(), ex);
                record.setStatus(ProcessingStatus.FAILED);
                record.setErrorMessage(msg);
                record.setCompletedAt(Instant.now());
                sessionStore.updateRecord(record);
            }
        }
    }

    private String buildErrorMessage(String filename, Exception ex) {
        String cause = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        if (ex instanceof javax.imageio.IIOException || ex instanceof IllegalStateException) {
            return "File could not be decoded: " + filename + " — " + cause;
        }
        return "Background removal failed for " + filename + ": " + cause;
    }
}
