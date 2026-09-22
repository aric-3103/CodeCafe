package com.bgremoval.CodeCafe.service;

import com.bgremoval.CodeCafe.domain.ImageRecord;
import com.bgremoval.CodeCafe.domain.ProcessingStatus;
import com.bgremoval.CodeCafe.dto.ImageRecordDto;
import com.bgremoval.CodeCafe.dto.UploadResponse;
import com.bgremoval.CodeCafe.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Default implementation of {@link UploadService}.
 *
 * <p>Requirements: 1.1, 1.7, 2.1, 2.2, 3.3
 */
@Service
public class UploadServiceImpl implements UploadService {

    private final FileValidatorService fileValidator;
    private final ProcessingSessionStore sessionStore;
    private final BackgroundRemovalService backgroundRemovalService;

    public UploadServiceImpl(FileValidatorService fileValidator,
                             ProcessingSessionStore sessionStore,
                             BackgroundRemovalService backgroundRemovalService) {
        this.fileValidator = fileValidator;
        this.sessionStore = sessionStore;
        this.backgroundRemovalService = backgroundRemovalService;
    }

    @Override
    public UploadResponse upload(String sessionId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new ValidationException(
                    "No files were submitted. Accepted formats: jpg, jpeg, png, webp. Max size: 25 MB.");
        }

        List<ImageRecordDto> acceptedDtos = new ArrayList<>();
        List<String> rejectedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            String filename = file.getOriginalFilename() != null
                    ? file.getOriginalFilename() : "(unknown)";
            try {
                // Validate — throws ValidationException on failure
                fileValidator.validate(file);

                // Build and store record
                ImageRecord record = new ImageRecord();
                record.setId(UUID.randomUUID());
                record.setSessionId(sessionId);
                record.setOriginalFilename(filename);
                record.setContentType(file.getContentType());
                record.setInputBytes(file.getBytes());
                record.setStatus(ProcessingStatus.IDLE);
                record.setUploadedAt(Instant.now());

                sessionStore.addRecord(sessionId, record);

                // Submit async processing task
                backgroundRemovalService.process(record.getId());

                acceptedDtos.add(toDto(record));

            } catch (ValidationException ex) {
                rejectedFiles.add(filename);
            } catch (IOException ex) {
                // Can't read bytes — treat as rejected
                rejectedFiles.add(filename);
            }
        }

        // Reject the entire request if no valid files remain
        if (acceptedDtos.isEmpty()) {
            throw new ValidationException(
                    "No valid files were submitted. Accepted formats: jpg, jpeg, png, webp. Max size: 25 MB.");
        }

        return new UploadResponse(sessionId, acceptedDtos, rejectedFiles);
    }

    private ImageRecordDto toDto(ImageRecord r) {
        return new ImageRecordDto(r.getId(), r.getOriginalFilename(), r.getStatus(), r.getErrorMessage());
    }
}
