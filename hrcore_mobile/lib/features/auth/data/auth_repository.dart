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
}
