package com.hrcoresystem.hrcore_api.bootstrap.platform;

public record DevTokenResponse(
        String tokenType,
        String accessToken,
        String refreshToken
) {
}