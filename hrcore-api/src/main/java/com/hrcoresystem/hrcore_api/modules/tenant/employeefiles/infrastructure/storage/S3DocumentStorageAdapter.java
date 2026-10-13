package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.infrastructure.storage;

import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.repository.DocumentStoragePort;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Component
public class S3DocumentStorageAdapter implements DocumentStoragePort {
  private final S3Client s3;
  private final DocumentStorageProperties properties;

  public S3DocumentStorageAdapter(S3Client s3, DocumentStorageProperties properties) {
    this.s3 = s3;
    this.properties = properties;
  }

  public void put(String key, Path source, String mimeType, String checksum) {
    try {
      s3.putObject(
          PutObjectRequest.builder()
              .bucket(properties.getBucket())
              .key(key)
              .contentType(mimeType)
              .metadata(Map.of("sha256", checksum))
              .ifNoneMatch("*")
              .build(),
          RequestBody.fromFile(source));
    } catch (RuntimeException ex) {
      throw new IllegalStateException("Document storage upload failed", ex);
    }
  }

  public void get(String key, Path destination, long maxBytes) {
    try (var input =
        s3.getObject(GetObjectRequest.builder().bucket(properties.getBucket()).key(key).build())) {
      Long declaredLength = input.response().contentLength();
      if (declaredLength != null && declaredLength > maxBytes)
        throw new IllegalStateException("Stored document exceeds size limit");
      try (OutputStream output =
          Files.newOutputStream(destination, StandardOpenOption.CREATE_NEW)) {
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
          if (read > maxBytes - total)
            throw new IllegalStateException("Stored document exceeds size limit");
          output.write(buffer, 0, read);
          total += read;
        }
      }
    } catch (IOException | RuntimeException ex) {
      throw new IllegalStateException("Document object unavailable or exceeds size limit", ex);
    }
  }

  public void delete(String key) {
    s3.deleteObject(DeleteObjectRequest.builder().bucket(properties.getBucket()).key(key).build());
  }
}
