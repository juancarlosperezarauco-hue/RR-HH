package com.hrcoresystem.hrcore_api.common.security.authorization.domain;

import java.util.regex.Pattern;

public final class PermissionCodeValidator {
  private static final Pattern PATTERN = Pattern.compile("^[a-z][a-z0-9_-]*(?:\\.[a-z0-9_-]+)*$");

  private PermissionCodeValidator() {
  }

  public static boolean isValid(String code) {
    return code != null && code.length() <= 100 && PATTERN.matcher(code).matches();
  }

  public static void requireValid(String code) {
    if (!isValid(code))
      throw new IllegalStateException("Invalid permission code: " + code);
  }
}
