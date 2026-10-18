import 'package:flutter_dotenv/flutter_dotenv.dart';

class ApiConfig {
  const ApiConfig({
    required this.baseUrl,
    this.tenantSlug,
    required this.connectTimeout,
    required this.receiveTimeout,
  });

  factory ApiConfig.fromEnvironment() {
    final baseUrl = dotenv.env['API_BASE_URL']?.trim();
    if (baseUrl == null || baseUrl.isEmpty) {
      throw StateError('API_BASE_URL no está definida en el archivo .env');
    }
    final tenantSlug = dotenv.env['API_TENANT_SLUG']?.trim();

    return ApiConfig(
      baseUrl: baseUrl,
      tenantSlug: tenantSlug,
      connectTimeout: _durationFromEnv('API_CONNECT_TIMEOUT_MS', 15000),
      receiveTimeout: _durationFromEnv('API_RECEIVE_TIMEOUT_MS', 30000),
    );
  }

  final String baseUrl;
  final String? tenantSlug;
  final Duration connectTimeout;
  final Duration receiveTimeout;

  static Duration _durationFromEnv(String key, int fallbackMs) {
    final value = int.tryParse(dotenv.env[key] ?? '');
    return Duration(milliseconds: value ?? fallbackMs);
  }
}
