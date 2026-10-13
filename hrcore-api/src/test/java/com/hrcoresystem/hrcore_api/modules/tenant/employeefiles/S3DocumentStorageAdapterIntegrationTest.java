package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles;

import static org.junit.jupiter.api.Assertions.*;

import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.infrastructure.storage.*;
import java.net.URI;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import org.junit.jupiter.api.*;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

class S3DocumentStorageAdapterIntegrationTest {
  @Test
  void roundTripAgainstExplicitIsolatedMinio() throws Exception {
    String endpoint = System.getenv("HU15_TEST_MINIO_ENDPOINT"),
        access = System.getenv("HU15_TEST_MINIO_ACCESS_KEY"),
        secret = System.getenv("HU15_TEST_MINIO_SECRET_KEY"),
        bucket = System.getenv("HU15_TEST_MINIO_BUCKET");
    Assumptions.assumeTrue(
        endpoint != null && access != null && secret != null && bucket != null,
        "Explicit isolated MinIO credentials are required");
    var props = new DocumentStorageProperties();
    props.setBucket(bucket);
    props.setRegion("us-east-1");
    byte[] content = "%PDF-1.7\n".getBytes();
    String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    Path source = Files.createTempFile("hu15-source-", ".pdf"),
        target = Files.createTempFile("hu15-target-", ".pdf");
    Files.write(source, content);
    Files.delete(target);
    String key = "tenants/hu15-integration/" + UUID.randomUUID();
    try (S3Client client =
        S3Client.builder()
            .region(Region.US_EAST_1)
            .endpointOverride(URI.create(endpoint))
            .forcePathStyle(true)
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create(access, secret)))
            .build()) {
      var adapter = new S3DocumentStorageAdapter(client, props);
      try {
        adapter.put(key, source, "application/pdf", hash);
        adapter.get(key, target, 1024);
        assertArrayEquals(content, Files.readAllBytes(target));
        Files.delete(target);
        assertThrows(IllegalStateException.class, () -> adapter.get(key, target, 4));
        assertFalse(Files.exists(target));
      } finally {
        adapter.delete(key);
      }
    } finally {
      Files.deleteIfExists(source);
      Files.deleteIfExists(target);
    }
  }
}
