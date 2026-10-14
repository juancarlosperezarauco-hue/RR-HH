package com.hrcoresystem.hrcore_api.common.security.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class SessionTokenHasher {
  private SessionTokenHasher() {
  }

  public static String hash(String token) {
    try {
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public static boolean matches(String token, String expected) {
    return MessageDigest.isEqual(hash(token).getBytes(StandardCharsets.US_ASCII),
        expected.getBytes(StandardCharsets.US_ASCII));
  }
}
