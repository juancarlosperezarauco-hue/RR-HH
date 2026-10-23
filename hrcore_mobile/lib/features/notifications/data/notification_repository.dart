import '../../../core/network/dio_client.dart';
import '../../../core/errors/app_exception.dart';
import '../../../core/network/api_exceptions.dart';
import '../../../shared/models/api_response.dart';

class MobileNotification {
  const MobileNotification({
    required this.id,
    required this.title,
    required this.body,
    required this.status,
    required this.createdAt,
    this.actionUrl,
  });

  factory MobileNotification.fromJson(Map<String, dynamic> json) =>
      MobileNotification(
        id: json['id'] as String? ?? '',
        title: json['title'] as String? ?? 'Notificación',
        body: json['body'] as String? ?? '',
        status: json['status'] as String? ?? 'UNREAD',
        createdAt:
            DateTime.tryParse(json['createdAt'] as String? ?? '') ??
            DateTime.now(),
        actionUrl: json['actionUrl'] as String?,
      );

  final String id;
  final String title;
  final String body;
  final String status;
  final DateTime createdAt;
  final String? actionUrl;
}

class NotificationDevice {
  const NotificationDevice({
    required this.id,
    required this.deviceName,
    required this.platform,
    required this.status,
  });

  factory NotificationDevice.fromJson(Map<String, dynamic> json) =>
      NotificationDevice(
        id: json['id'] as String? ?? '',
        deviceName: json['deviceName'] as String? ?? 'Dispositivo móvil',
        platform: json['platform'] as String? ?? 'UNKNOWN',
        status: json['status'] as String? ?? 'INACTIVE',
      );

  final String id;
  final String deviceName;
  final String platform;
  final String status;
}

class NotificationRepository {
  const NotificationRepository(this._client);

  final DioClient _client;

  Future<List<MobileNotification>> list({int page = 0, int size = 50}) async {
    final response = await _client.dio.get<Map<String, dynamic>>(
      '/api/me/notifications',
      queryParameters: {'page': page, 'size': size},
    );
    final body = response.data;
    if (body == null) return const [];
    final result = ApiResponse<Map<String, dynamic>>.fromJson(
      body,
      (value) => value as Map<String, dynamic>,
    );
    final content = result.data['content'];
    if (!result.success || content is! List) return const [];
    return content
        .whereType<Map<String, dynamic>>()
        .map(MobileNotification.fromJson)
        .toList();
  }

  Future<int> unreadCount() async {
    final response = await _client.dio.get<Map<String, dynamic>>(
      '/api/me/notifications/unread-count',
    );
    final body = response.data;
    if (body == null) return 0;
    final result = ApiResponse<Map<String, dynamic>>.fromJson(
      body,
      (value) => value as Map<String, dynamic>,
    );
    return result.success
        ? (result.data['unreadCount'] as num?)?.toInt() ?? 0
        : 0;
  }

  Future<void> markAsRead(String id) async {
    await _client.dio.patch<void>('/api/me/notifications/$id/read', data: {});
  }

  Future<List<NotificationDevice>> listDevices() async {
    try {
      final response = await _client.dio.get<Map<String, dynamic>>(
        '/api/me/notification-devices',
      );
      final body = response.data;
      if (body == null) {
        throw const AppException(
          message: 'La respuesta de dispositivos está vacía.',
        );
      }
      final result = ApiResponse<Map<String, dynamic>>.fromJson(
        body,
        (value) => value is Map<String, dynamic> ? value : <String, dynamic>{},
      );
      if (!result.success) throw AppException(message: result.message);
      final content = result.data['content'];
      if (content is! List) {
        throw const AppException(
          message: 'La respuesta de dispositivos no es válida.',
        );
      }
      return content
          .whereType<Map<String, dynamic>>()
          .map(NotificationDevice.fromJson)
          .toList();
    } catch (error) {
      if (error is AppException) rethrow;
      throw mapDioException(error);
    }
  }

  Future<void> deactivateDevice(String id) async {
    await _client.dio.patch<void>(
      '/api/me/notification-devices/$id/deactivate',
      data: {},
    );
  }
}
