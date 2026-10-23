import 'package:dio/dio.dart';

import '../../../features/auth/data/models/auth_token_model.dart';
import '../../../shared/models/api_response.dart';
import '../../storage/secure_storage_service.dart';

class AuthInterceptor extends Interceptor {
  AuthInterceptor(this._secureStorage, this._baseUrl);

  final SecureStorageService _secureStorage;
  final String _baseUrl;
  Future<bool>? _refreshing;

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    final token = await _secureStorage.readToken();
    if (token != null && token.isNotEmpty) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    final tenantSlug = await _secureStorage.readTenantSlug();
    if (tenantSlug != null && tenantSlug.isNotEmpty) {
      options.headers['X-Tenant-Slug'] = tenantSlug;
    }
    handler.next(options);
  }

  @override
  Future<void> onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    final request = err.requestOptions;
    final isAuthEndpoint = request.path.endsWith('/api/auth/login') ||
        request.path.endsWith('/api/auth/refresh') ||
        request.path.endsWith('/api/auth/logout');
    if (err.response?.statusCode != 401 ||
        isAuthEndpoint ||
        request.extra['authRetry'] == true) {
      handler.next(err);
      return;
    }

    final refreshed = await (_refreshing ??= _refresh()).whenComplete(() {
      _refreshing = null;
    });
    if (!refreshed) {
      await _secureStorage.deleteToken();
      handler.next(err);
      return;
    }

    try {
      final token = await _secureStorage.readToken();
      final tenant = await _secureStorage.readTenantSlug();
      final retry = request.copyWith(
        headers: {
          ...request.headers,
          if (token != null) 'Authorization': 'Bearer $token',
          if (tenant != null && tenant.isNotEmpty) 'X-Tenant-Slug': tenant,
        },
        extra: {...request.extra, 'authRetry': true},
      );
      handler.resolve(await Dio().fetch(retry));
    } on DioException catch (retryError) {
      handler.next(retryError);
    }
  }

  Future<bool> _refresh() async {
    final refreshToken = await _secureStorage.readRefreshToken();
    final tenant = await _secureStorage.readTenantSlug();
    if (refreshToken == null ||
        refreshToken.isEmpty ||
        tenant == null ||
        tenant.isEmpty) {
      return false;
    }
    try {
      final response = await Dio(BaseOptions(baseUrl: _baseUrl))
          .post<Map<String, dynamic>>(
            '/api/auth/refresh',
            data: {'refreshToken': refreshToken},
            options: Options(headers: {'X-Tenant-Slug': tenant}),
          );
      final body = response.data;
      if (body == null) return false;
      final result = ApiResponse<AuthTokenModel>.fromJson(
        body,
        (value) => AuthTokenModel.fromJson(value as Map<String, dynamic>),
      );
      final token = result.data;
      if (!result.success ||
          token.accessToken.isEmpty ||
          token.refreshToken.isEmpty ||
          token.accessExpiresInMs <= 0) {
        return false;
      }
      await _secureStorage.saveSession(
        accessToken: token.accessToken,
        refreshToken: token.refreshToken,
        tenantSlug: tenant,
        accessExpiresInMs: token.accessExpiresInMs,
      );
      return true;
    } catch (_) {
      return false;
    }
  }
}
