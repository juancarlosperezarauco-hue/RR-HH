import 'package:dio/dio.dart';

import '../config/api_config.dart';
import '../storage/secure_storage_service.dart';
import 'interceptors/auth_interceptor.dart';

class DioClient {
  DioClient({
    required ApiConfig config,
    required SecureStorageService secureStorage,
  }) : dio = Dio(
         BaseOptions(
           baseUrl: config.baseUrl,
           connectTimeout: config.connectTimeout,
           receiveTimeout: config.receiveTimeout,
           headers: {
             'Accept': 'application/json',
             if (config.tenantSlug != null && config.tenantSlug!.isNotEmpty)
               'X-Tenant-Slug': config.tenantSlug,
           },
         ),
       ) {
    dio.interceptors.add(AuthInterceptor(secureStorage, config.baseUrl));
  }

  final Dio dio;
}
