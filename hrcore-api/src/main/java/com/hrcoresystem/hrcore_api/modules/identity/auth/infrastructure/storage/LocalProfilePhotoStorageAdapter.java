package com.hrcoresystem.hrcore_api.modules.identity.auth.infrastructure.storage;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.config.ProfilePhotoStorageProperties;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.port.ProfilePhotoStoragePort;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

@Component
public class LocalProfilePhotoStorageAdapter implements ProfilePhotoStoragePort {

    private static final String PUBLIC_URL_PREFIX = "/api/public/auth/profile/photo";
    private final ProfilePhotoStorageProperties properties;

    public LocalProfilePhotoStorageAdapter(ProfilePhotoStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public StoredProfilePhoto store(String tenantSlug, UUID userId, byte[] bytes, String contentType) {
        if (!properties.isConfigured()) {
            throw new BusinessException("Profile photo storage is not configured");
        }
        if (!StringUtils.hasText(tenantSlug) || userId == null) {
            throw new BusinessException("Tenant slug and user id are required");
        }
        if (bytes == null || bytes.length == 0) {
            throw new BusinessException("Photo bytes are required");
        }

        Path userDir = resolveUserDir(tenantSlug, userId);
        try {
            deleteDirectoryContents(userDir);
            Files.createDirectories(userDir);
            Files.write(userDir.resolve(photoFilename(contentType)), bytes);
        } catch (IOException exception) {
            throw new BusinessException("No se pudo guardar la foto de perfil");
        }

        return new StoredProfilePhoto(publicUrl(tenantSlug, userId, contentType));
    }

    @Override
    public PhotoFile read(String tenantSlug, UUID userId, String contentType) {
        if (!properties.isConfigured()) {
            throw new BusinessException("Profile photo storage is not configured");
        }
        Path file = resolveUserDir(tenantSlug, userId).resolve(photoFilename(contentType));
        if (!Files.exists(file)) {
            throw new BusinessException("Profile photo not found");
        }

        try {
            return new PhotoFile(Files.readAllBytes(file), normalizedContentType(contentType), photoFilename(contentType));
        } catch (IOException exception) {
            throw new BusinessException("No se pudo leer la foto de perfil");
        }
    }

    @Override
    public PhotoFile readPublic(String tenantSlug, UUID userId) {
        if (!properties.isConfigured()) {
            throw new BusinessException("Profile photo storage is not configured");
        }

        Path userDir = resolveUserDir(tenantSlug, userId);
        if (!Files.exists(userDir)) {
            throw new BusinessException("Profile photo not found");
        }

        try (Stream<Path> stream = Files.list(userDir)) {
            Path file = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("profile."))
                    .min(Comparator.comparing(Path::toString))
                    .orElseThrow(() -> new BusinessException("Profile photo not found"));

            String filename = file.getFileName().toString();
            return new PhotoFile(
                    Files.readAllBytes(file),
                    contentTypeFromFilename(filename),
                    filename
            );
        } catch (IOException exception) {
            throw new BusinessException("No se pudo leer la foto de perfil");
        }
    }

    @Override
    public void delete(String tenantSlug, UUID userId) {
        if (!properties.isConfigured()) {
            return;
        }
        Path userDir = resolveUserDir(tenantSlug, userId);
        try {
            deleteDirectoryContents(userDir);
        } catch (IOException exception) {
            throw new BusinessException("No se pudo eliminar la foto de perfil");
        }
    }

    private Path resolveUserDir(String tenantSlug, UUID userId) {
        return Paths.get(properties.getRootDir(), safeSegment(tenantSlug), userId.toString());
    }

    private void deleteDirectoryContents(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var stream = Files.walk(directory)) {
            stream.sorted((left, right) -> right.compareTo(left))
                    .filter(path -> !path.equals(directory))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                    });
        } catch (RuntimeException exception) {
            if (exception.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw exception;
        }
    }

    private String publicUrl(String tenantSlug, UUID userId, String contentType) {
        return PUBLIC_URL_PREFIX + "/" + safeSegment(tenantSlug) + "/" + userId + extensionFor(normalizedContentType(contentType));
    }

    private String photoFilename(String contentType) {
        String normalized = normalizedContentType(contentType);
        return switch (normalized) {
            case "image/jpeg" -> "profile.jpg";
            case "image/png" -> "profile.png";
            case "image/webp" -> "profile.webp";
            case "image/gif" -> "profile.gif";
            default -> "profile.bin";
        };
    }

    private String normalizedContentType(String contentType) {
        return StringUtils.hasText(contentType) ? contentType.split(";")[0].trim().toLowerCase() : "application/octet-stream";
    }

    private String safeSegment(String value) {
        return value == null ? "unknown" : value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> "";
        };
    }

    private String contentTypeFromFilename(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        return "application/octet-stream";
    }
}
