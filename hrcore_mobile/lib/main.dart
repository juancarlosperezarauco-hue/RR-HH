import 'package:flutter/widgets.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';

import 'app/app.dart';
import 'app/di/injection_container.dart';
import 'app/router/route_names.dart';
import 'core/storage/secure_storage_service.dart';
import 'core/auth/biometric_auth_service.dart';
import 'features/auth/data/auth_repository.dart';
import 'features/notifications/data/push_notification_service.dart';

@pragma('vm:entry-point')
Future<void> _firebaseBackgroundHandler(RemoteMessage message) async {
  await Firebase.initializeApp();
}

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await dotenv.load(fileName: '.env');
  try {
    await Firebase.initializeApp();
    FirebaseMessaging.onBackgroundMessage(_firebaseBackgroundHandler);
  } catch (_) {
    // Firebase is optional on platforms without a native Firebase config.
  }
  await configureDependencies();
  final storage = getIt<SecureStorageService>();
  final biometric = getIt<BiometricAuthService>();
  var hasValidSession = await storage.hasValidSession();
  if (!hasValidSession &&
      !await storage.isSessionLocked() &&
      await storage.readRefreshToken() != null) {
    hasValidSession = await getIt<AuthRepository>().restoreSession();
  }
  final initialLocation = hasValidSession && biometric.isEnabled
      ? RouteNames.biometricLockPath
      : hasValidSession
          ? RouteNames.homePath
          : RouteNames.loginPath;
  if (hasValidSession) {
    await getIt<PushNotificationService>().initialize();
  }
  runApp(MyApp(
    initialLocation: initialLocation,
  ));
}

/// Kept as the public application entry point for existing consumers/tests.
class MyApp extends App {
  const MyApp({super.key, super.initialLocation});
}
