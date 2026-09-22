package com.bgremoval.CodeCafe.service;

import com.bgremoval.CodeCafe.domain.ImageRecord;
import com.bgremoval.CodeCafe.domain.ProcessingStatus;
import com.bgremoval.CodeCafe.exception.ZipPackagingException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Default implementation of {@link ZipPackagerService}.
 *
 * <p>Requirements: 7.2, 7.3, 7.4, 7.5
 */
@Service
public class ZipPackagerServiceImpl implements ZipPackagerService {

    private final ProcessingSessionStore sessionStore;

    public ZipPackagerServiceImpl(ProcessingSessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Override
    public void packageSession(String sessionId, OutputStream out) throws ZipPackagingException {
        List<ImageRecord> completed = sessionStore.findBySession(sessionId)
                .stream()
                .filter(r -> r.getStatus() == ProcessingStatus.COMPLETED)
                .collect(Collectors.toList());

        if (completed.isEmpty()) {
            throw new ZipPackagingException(
                    "No COMPLETED images found for session: " + sessionId);
        }

        // Track used entry names for deduplication
        Map<String, Integer> nameCount = new HashMap<>();

        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            for (ImageRecord record : completed) {
                String baseName   = FilenameUtil.toDownloadFilename(record.getOriginalFilename());
                String entryName  = deduplicate(baseName, nameCount);

                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                zos.write(record.getOutputBytes());
                zos.closeEntry();
            }
        } catch (IOException e) {
            throw new ZipPackagingException(
                    "I/O error while building ZIP for session: " + sessionId, e);
        }
    }

    /**
     * Returns a unique entry name by appending {@code -N} (N ≥ 2) before the
     * final {@code .png} if the base name has already been used.
     *
     * <p>Example: first "photo-bg-removed.png" stays as-is; second becomes
     * "photo-bg-removed-2.png"; third becomes "photo-bg-removed-3.png".
     */
    private static String deduplicate(String proposedName, Map<String, Integer> nameCount) {
        if (!nameCount.containsKey(proposedName)) {
            nameCount.put(proposedName, 1);
            return proposedName;
        }

        int count = nameCount.get(proposedName) + 1;
        nameCount.put(proposedName, count);

        // Insert the counter suffix before .png
        // e.g. "photo-bg-removed.png" → "photo-bg-removed-2.png"
        if (proposedName.endsWith(".png")) {
            String withoutExt = proposedName.substring(0, proposedName.length() - 4);
            return withoutExt + "-" + count + ".png";
        }
        return proposedName + "-" + count;
    }
}
