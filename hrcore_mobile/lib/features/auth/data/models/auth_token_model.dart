class AuthTokenModel {
  const AuthTokenModel({
    required this.tokenType,
    required this.accessToken,
    required this.refreshToken,
    required this.accessExpiresInMs,
  });

  factory AuthTokenModel.fromJson(Map<String, dynamic> json) {
    return AuthTokenModel(
      tokenType: json['tokenType'] as String? ?? '',
      accessToken: json['accessToken'] as String? ?? '',
      refreshToken: json['refreshToken'] as String? ?? '',
      accessExpiresInMs: (json['accessExpiresInMs'] as num?)?.toInt() ?? 0,
    );
  }

  final String tokenType;
  final String accessToken;
  final String refreshToken;
  final int accessExpiresInMs;
}
