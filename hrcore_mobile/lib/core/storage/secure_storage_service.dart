import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class SecureStorageService {
  SecureStorageService([FlutterSecureStorage? storage])
    : _storage = storage ?? const FlutterSecureStorage();

  static const tokenKey = 'access_token';
  static const refreshTokenKey = 'refresh_token';
  static const accessExpiresAtKey = 'access_expires_at';
  static const tenantSlugKey = 'tenant_slug';
  static const sessionLockedKey = 'session_locked';
  final FlutterSecureStorage _storage;

  Future<String?> readToken() => _storage.read(key: tokenKey);

  Future<String?> readRefreshToken() => _storage.read(key: refreshTokenKey);

  Future<DateTime?> readAccessExpiresAt() async {
    final value = await _storage.read(key: accessExpiresAtKey);
    if (value == null || value.isEmpty) return null;
    return DateTime.tryParse(value);
  }

  Future<bool> hasValidSession() async {
    final token = await readToken();
    final expiresAt = await readAccessExpiresAt();
    return token != null && token.isNotEmpty && expiresAt != null && expiresAt.isAfter(DateTime.now());
  }

  Future<String?> readTenantSlug() => _storage.read(key: tenantSlugKey);

  Future<bool> isSessionLocked() async =>
      (await _storage.read(key: sessionLockedKey)) == 'true';

  Future<void> saveToken(String token) =>
      _storage.write(key: tokenKey, value: token);

  Future<void> deleteToken() async {
    await Future.wait([
      _storage.delete(key: tokenKey),
      _storage.delete(key: refreshTokenKey),
      _storage.delete(key: accessExpiresAtKey),
      _storage.delete(key: tenantSlugKey),
      _storage.delete(key: sessionLockedKey),
    ]);
  }

  Future<void> lockSessionForBiometric() async {
    await Future.wait([
      _storage.delete(key: tokenKey),
      _storage.delete(key: accessExpiresAtKey),
      _storage.write(key: sessionLockedKey, value: 'true'),
    ]);
  }

  Future<void> saveSession({
    required String accessToken,
    required String refreshToken,
    required String tenantSlug,
    required int accessExpiresInMs,
  }) async {
    await Future.wait([
      _storage.write(key: tokenKey, value: accessToken),
      _storage.write(key: refreshTokenKey, value: refreshToken),
      _storage.write(key: tenantSlugKey, value: tenantSlug),
      _storage.delete(key: sessionLockedKey),
      _storage.write(
        key: accessExpiresAtKey,
        value: DateTime.now()
            .add(Duration(milliseconds: accessExpiresInMs))
            .toIso8601String(),
      ),
    ]);
  }
}
