package com.hrcoresystem.hrcore_api.common.security.session;

public record IssuedTokens(String accessToken, String refreshToken, long accessExpiresInMs) {
}
