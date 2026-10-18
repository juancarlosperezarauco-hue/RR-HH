import 'package:dio/dio.dart';

import '../../storage/secure_storage_service.dart';

class AuthInterceptor extends Interceptor {
  AuthInterceptor(this._secureStorage);

  final SecureStorageService _secureStorage;

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
}
