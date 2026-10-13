package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.repository;

import java.nio.file.Path;

public interface DocumentStoragePort {
  void put(String key, Path source, String mimeType, String checksumSha256);

  void get(String key, Path destination, long maxBytes);

  void delete(String key);
}
