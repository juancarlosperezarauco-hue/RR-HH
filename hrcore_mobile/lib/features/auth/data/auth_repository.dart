import 'package:dio/dio.dart';

import '../../../core/errors/app_exception.dart';
import '../../../core/network/api_exceptions.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/storage/secure_storage_service.dart';
import '../../../shared/models/api_response.dart';
import 'models/auth_token_model.dart';

class AuthRepository {
  const AuthRepository(this._client, this._secureStorage);

  final DioClient _client;
  final SecureStorageService _secureStorage;

  Future<void> login({
    required String tenantSlug,
    required String email,
    required String password,
  }) async {
    try {
      final response = await _client.dio.post<Map<String, dynamic>>(
        '/api/auth/login',
        data: {'email': email.trim(), 'password': password},
        options: Options(headers: {'X-Tenant-Slug': tenantSlug.trim()}),
      );

      final body = response.data;
      if (body == null) {
        throw const AppException(
          message: 'La respuesta del servidor está vacía.',
        );
      }

      final result = ApiResponse<AuthTokenModel>.fromJson(
        body,
        (value) => AuthTokenModel.fromJson(value as Map<String, dynamic>),
      );
      if (!result.success) {
        throw AppException(message: result.message);
      }

      final token = result.data;
      if (token.tokenType.toLowerCase() != 'bearer' ||
          token.accessToken.isEmpty ||
          token.refreshToken.isEmpty ||
          token.accessExpiresInMs <= 0) {
        throw const AppException(
          message: 'La respuesta de autenticación no es válida.',
        );
      }

      await _secureStorage.saveSession(
        accessToken: token.accessToken,
        refreshToken: token.refreshToken,
        tenantSlug: tenantSlug.trim(),
        accessExpiresInMs: token.accessExpiresInMs,
      );
    } catch (error) {
      if (error is AppException) rethrow;
      throw mapDioException(error);
    }
  }

  Future<void> logout({bool preserveBiometricSession = false}) async {
    if (preserveBiometricSession) {
      await _secureStorage.lockSessionForBiometric();
      return;
    }
    try {
      final token = await _secureStorage.readToken();
      if (token != null && token.isNotEmpty) {
        await _client.dio.post<void>('/api/auth/logout');
      }
    } catch (_) {
      // Logout is local-first: a network failure must not keep the session.
    } finally {
      await _secureStorage.deleteToken();
    }
  }

  Future<bool> restoreSession() async {
    final refreshToken = await _secureStorage.readRefreshToken();
    final tenantSlug = await _secureStorage.readTenantSlug();
    if (refreshToken == null || refreshToken.isEmpty ||
        tenantSlug == null || tenantSlug.isEmpty) {
      return false;
    }
    try {
      final response = await _client.dio.post<Map<String, dynamic>>(
        '/api/auth/refresh',
        data: {'refreshToken': refreshToken},
        options: Options(headers: {'X-Tenant-Slug': tenantSlug}),
      );
      final body = response.data;
      if (body == null) {
        await _secureStorage.deleteToken();
        return false;
      }
      final result = ApiResponse<AuthTokenModel>.fromJson(
        body,
        (value) => AuthTokenModel.fromJson(value as Map<String, dynamic>),
      );
      final token = result.data;
      if (!result.success || token.accessToken.isEmpty ||
          token.refreshToken.isEmpty || token.accessExpiresInMs <= 0) {
        await _secureStorage.deleteToken();
        return false;
      }
      await _secureStorage.saveSession(
        accessToken: token.accessToken,
        refreshToken: token.refreshToken,
        tenantSlug: tenantSlug,
        accessExpiresInMs: token.accessExpiresInMs,
      );
      return true;
    } catch (_) {
      await _secureStorage.deleteToken();
      return false;
    }
  }

  Future<void> changePassword({
    required String currentPassword,
    required String newPassword,
  }) async {
    try {
      final response = await _client.dio.post<Map<String, dynamic>>(
        '/api/auth/change-password',
        data: {
          'currentPassword': currentPassword,
          'newPassword': newPassword,
        },
      );
      final body = response.data;
      if (body == null || body['success'] != true) {
        throw AppException(
          message: body?['message'] as String? ?? 'No se pudo cambiar la contraseña.',
        );
      }
    } catch (error) {
      if (error is AppException) rethrow;
      throw mapDioException(error);
    }
  }

  Future<void> revokeOtherSessions() async {
    try {
      await _client.dio.post<void>('/api/auth/sessions/revoke-others');
    } catch (error) {
      throw mapDioException(error);
    }
  }
}
