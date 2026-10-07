package com.hrcoresystem.hrcore_api.modules.identity.auth.application.port;

public interface FaceRecognitionPort {

    DetectedFace detectFace(byte[] imageBytes, String filename, String contentType);

    FaceComparisonResult compareFaceTokens(String faceToken1, String faceToken2);

    boolean isConfigured();

    record DetectedFace(String faceToken, String faceId) {
    }

    record FaceComparisonResult(double confidence) {
    }
}
