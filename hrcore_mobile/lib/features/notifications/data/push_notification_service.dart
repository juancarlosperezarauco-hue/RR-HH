import 'dart:io';

import 'package:device_info_plus/device_info_plus.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:package_info_plus/package_info_plus.dart';

import '../../../core/network/dio_client.dart';
import 'notification_repository.dart';

class PushNotificationService {
  PushNotificationService(this._client);

  final DioClient _client;
  final FlutterLocalNotificationsPlugin _local =
      FlutterLocalNotificationsPlugin();

  FirebaseMessaging get _messaging => FirebaseMessaging.instance;

  Future<void> initialize() async {
    try {
      if (Firebase.apps.isEmpty) return;
      await _messaging.requestPermission(alert: true, badge: true, sound: true);
      const android = AndroidInitializationSettings('@mipmap/ic_launcher');
      const ios = DarwinInitializationSettings();
      await _local.initialize(
        settings: const InitializationSettings(android: android, iOS: ios),
      );
      await _local
          .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin
          >()
          ?.requestNotificationsPermission();
      await registerCurrentDevice();
      _messaging.onTokenRefresh.listen((token) async {
        await _register(token);
      });
      FirebaseMessaging.onMessage.listen(_showForegroundNotification);
    } catch (_) {
      // Push configuration must never prevent authentication or app startup.
    }
  }

  Future<NotificationDevice?> registerCurrentDevice() async {
    try {
      return await _register(await _messaging.getToken());
    } catch (_) {
      return null;
    }
  }

  Future<NotificationDevice?> _register(String? token) async {
    if (token == null || token.isEmpty) return null;
    try {
      final package = await PackageInfo.fromPlatform();
      final device = DeviceInfoPlugin();
      String deviceId = 'unknown';
      String deviceName = 'Dispositivo móvil';
      String osVersion = Platform.operatingSystemVersion;
      String platform = Platform.isIOS ? 'IOS' : 'ANDROID';
      if (Platform.isAndroid) {
        final info = await device.androidInfo;
        deviceId = info.id;
        deviceName = '${info.manufacturer} ${info.model}';
        osVersion = info.version.release;
      } else if (Platform.isIOS) {
        final info = await device.iosInfo;
        deviceId = info.identifierForVendor ?? info.name;
        deviceName = info.name;
        osVersion = info.systemVersion;
      }
      final response = await _client.dio.post<Map<String, dynamic>>(
        '/api/me/notification-devices',
        data: {
          'deviceId': deviceId,
          'fcmToken': token,
          'platform': platform,
          'deviceName': deviceName,
          'appVersion': package.version,
          'osVersion': osVersion,
        },
      );
      final data = response.data?['data'];
      return data is Map<String, dynamic>
          ? NotificationDevice.fromJson(data)
          : null;
    } catch (_) {
      // Push registration must never block login or app usage.
      return null;
    }
  }

  Future<void> _showForegroundNotification(RemoteMessage message) async {
    final notification = message.notification;
    if (notification == null) return;
    const details = NotificationDetails(
      android: AndroidNotificationDetails(
        'hrcore_notifications',
        'HRCore',
        channelDescription: 'Notificaciones de HRCore',
        importance: Importance.high,
        priority: Priority.high,
      ),
      iOS: DarwinNotificationDetails(),
    );
    await _local.show(
      id: message.hashCode,
      title: notification.title,
      body: notification.body,
      notificationDetails: details,
      payload: message.data['actionUrl']?.toString(),
    );
  }
}
