package com.bgremoval.CodeCafe.controller;

import com.bgremoval.CodeCafe.domain.ImageRecord;
import com.bgremoval.CodeCafe.domain.ProcessingStatus;
import com.bgremoval.CodeCafe.dto.ImageRecordDto;
import com.bgremoval.CodeCafe.dto.SessionStatusResponse;
import com.bgremoval.CodeCafe.dto.UploadResponse;
import com.bgremoval.CodeCafe.exception.RecordNotFoundException;
import com.bgremoval.CodeCafe.exception.ZipPackagingException;
import com.bgremoval.CodeCafe.service.FilenameUtil;
import com.bgremoval.CodeCafe.service.ProcessingSessionStore;
import com.bgremoval.CodeCafe.service.UploadService;
import com.bgremoval.CodeCafe.service.ZipPackagerService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST API surface for the background removal tool.
 *
 * <p>All routes are under {@code /api}.
 * Requirements: 1.1, 2.1–2.4, 4.1, 4.2, 5.1, 5.6, 6.1–6.4, 7.1–7.5, 9.2
 */
@RestController
@RequestMapping("/api")
public class ImageController {

    private final UploadService          uploadService;
    private final ProcessingSessionStore sessionStore;
    private final ZipPackagerService     zipPackagerService;

    public ImageController(UploadService uploadService,
                           ProcessingSessionStore sessionStore,
                           ZipPackagerService zipPackagerService) {
        this.uploadService      = uploadService;
        this.sessionStore       = sessionStore;
        this.zipPackagerService = zipPackagerService;
    }

    // -----------------------------------------------------------------------
    // POST /api/images/upload
    // -----------------------------------------------------------------------

    /**
     * Accepts multipart file uploads and enqueues them for processing.
     *
     * @param sessionId browser-generated UUID for the current session
     * @param files     1–50 image files
     * @return 200 with {@link UploadResponse}; 400 if all files are rejected
     */
    @PostMapping("/images/upload")
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("files") List<MultipartFile> files) {
        UploadResponse response = uploadService.upload(sessionId, files);
        return ResponseEntity.ok(response);
    }

    // -----------------------------------------------------------------------
    // GET /api/session/{sessionId}/status
    // -----------------------------------------------------------------------

    /**
     * Returns a snapshot of all records in the session for polling.
     *
     * <p>{@code allTerminal} is {@code true} when every record has reached a
     * terminal status (COMPLETED or FAILED) — the browser should stop polling.
     */
    @GetMapping("/session/{sessionId}/status")
    public ResponseEntity<SessionStatusResponse> sessionStatus(
            @PathVariable String sessionId) {
        List<ImageRecord> records = sessionStore.findBySession(sessionId);
        List<ImageRecordDto> dtos = records.stream().map(this::toDto).collect(Collectors.toList());

        int total     = dtos.size();
        int completed = (int) dtos.stream().filter(d -> d.status() == ProcessingStatus.COMPLETED).count();
        int failed    = (int) dtos.stream().filter(d -> d.status() == ProcessingStatus.FAILED).count();
        int skipped   = 0; // validator-rejected files are never stored as records
        boolean allTerminal = total > 0 && dtos.stream()
                .allMatch(d -> d.status() == ProcessingStatus.COMPLETED
                            || d.status() == ProcessingStatus.FAILED);

        SessionStatusResponse response = new SessionStatusResponse(
                sessionId, dtos, total, completed, failed, skipped, allTerminal);
        return ResponseEntity.ok(response);
    }

    // -----------------------------------------------------------------------
    // GET /api/images/{recordId}/status
    // -----------------------------------------------------------------------

    @GetMapping("/images/{recordId}/status")
    public ResponseEntity<ImageRecordDto> imageStatus(@PathVariable UUID recordId) {
        ImageRecord record = sessionStore.findById(recordId)
                .orElseThrow(() -> new RecordNotFoundException("Record not found: " + recordId));
        return ResponseEntity.ok(toDto(record));
    }

    // -----------------------------------------------------------------------
    // GET /api/images/{recordId}/download
    // -----------------------------------------------------------------------

    @GetMapping("/images/{recordId}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID recordId) {
        ImageRecord record = sessionStore.findById(recordId)
                .orElseThrow(() -> new RecordNotFoundException("Record not found: " + recordId));

        if (record.getStatus() != ProcessingStatus.COMPLETED) {
            throw new RecordNotFoundException(
                    "Record " + recordId + " is not yet COMPLETED (status: " + record.getStatus() + ")");
        }

        byte[] bytes = record.getOutputBytes();
        if (bytes == null || bytes.length == 0) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        String downloadName = FilenameUtil.toDownloadFilename(record.getOriginalFilename());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + downloadName + "\"")
                .body(bytes);
    }

    // -----------------------------------------------------------------------
    // GET /api/session/{sessionId}/download-all
    // -----------------------------------------------------------------------

    @GetMapping("/session/{sessionId}/download-all")
    public void downloadAll(@PathVariable String sessionId,
                            HttpServletResponse response) throws IOException {
        // Check for COMPLETED records first
        List<ImageRecord> completed = sessionStore.findBySession(sessionId)
                .stream()
                .filter(r -> r.getStatus() == ProcessingStatus.COMPLETED)
                .collect(Collectors.toList());

        if (completed.isEmpty()) {
            response.sendError(HttpStatus.NOT_FOUND.value(),
                    "No COMPLETED images found for session: " + sessionId);
            return;
        }

        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"processed-images.zip\"");

        try {
            zipPackagerService.packageSession(sessionId, response.getOutputStream());
        } catch (ZipPackagingException ex) {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // GET /api/images/{recordId}/preview
    // -----------------------------------------------------------------------

    @GetMapping("/images/{recordId}/preview")
    public ResponseEntity<byte[]> preview(@PathVariable UUID recordId,
                                          @RequestParam(defaultValue = "result") String type) {
        ImageRecord record = sessionStore.findById(recordId)
                .orElseThrow(() -> new RecordNotFoundException("Record not found: " + recordId));

        if ("original".equalsIgnoreCase(type)) {
            byte[] original = record.getInputBytes();
            if (original == null) {
                throw new RecordNotFoundException("Original bytes not available for record: " + recordId);
            }
            String mime = record.getContentType() != null ? record.getContentType() : "image/jpeg";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mime))
                    .body(original);
        }

        // Default: result PNG
        if (record.getStatus() != ProcessingStatus.COMPLETED || record.getOutputBytes() == null) {
            throw new RecordNotFoundException("Result not yet available for record: " + recordId);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(record.getOutputBytes());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private ImageRecordDto toDto(ImageRecord r) {
        return new ImageRecordDto(r.getId(), r.getOriginalFilename(), r.getStatus(), r.getErrorMessage());
    }
}
