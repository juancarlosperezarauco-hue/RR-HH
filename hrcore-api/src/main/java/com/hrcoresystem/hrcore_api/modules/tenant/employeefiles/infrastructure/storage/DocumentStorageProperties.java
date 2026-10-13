package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage.documents")
public class DocumentStorageProperties {
  private String bucket;
  private String region = "us-east-1";
  private String endpoint;
  private String accessKey;
  private String secretKey;
  private long maxBytes = 10485760L;

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String x) {
    bucket = x;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String x) {
    region = x;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String x) {
    endpoint = x;
  }

  public String getAccessKey() {
    return accessKey;
  }

  public void setAccessKey(String x) {
    accessKey = x;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String x) {
    secretKey = x;
  }

  public long getMaxBytes() {
    return maxBytes;
  }

  public void setMaxBytes(long x) {
    maxBytes = x;
  }
}
