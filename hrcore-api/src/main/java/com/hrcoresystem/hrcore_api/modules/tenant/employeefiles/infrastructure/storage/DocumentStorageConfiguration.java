package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.infrastructure.storage;

import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(DocumentStorageProperties.class)
public class DocumentStorageConfiguration {
  @Bean
  public S3Client documentS3Client(DocumentStorageProperties p) {
    if (p.getBucket() == null || p.getBucket().isBlank())
      throw new IllegalStateException("Document bucket must be configured");
    if (p.getRegion() == null || p.getRegion().isBlank())
      throw new IllegalStateException("Document region must be configured");
    if (p.getMaxBytes() < 1) throw new IllegalStateException("Document max bytes must be positive");
    var builder = S3Client.builder().region(Region.of(p.getRegion()));
    if (p.getEndpoint() != null && !p.getEndpoint().isBlank()) {
      if (p.getAccessKey() == null
          || p.getAccessKey().isBlank()
          || p.getSecretKey() == null
          || p.getSecretKey().isBlank())
        throw new IllegalStateException("Local document storage credentials are required");
      builder
          .endpointOverride(URI.create(p.getEndpoint()))
          .forcePathStyle(true)
          .credentialsProvider(
              StaticCredentialsProvider.create(
                  AwsBasicCredentials.create(p.getAccessKey(), p.getSecretKey())));
    }
    return builder.build();
  }
}
