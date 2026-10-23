import 'package:get_it/get_it.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../core/config/api_config.dart';
import '../../core/auth/biometric_auth_service.dart';
import '../../core/network/dio_client.dart';
import '../../core/storage/preferences_service.dart';
import '../../core/storage/secure_storage_service.dart';
import '../../features/auth/data/auth_repository.dart';
import '../../features/attendance/data/attendance_repository.dart';
import '../../features/home/data/home_repository.dart';
import '../../features/notifications/data/notification_repository.dart';
import '../../features/notifications/data/push_notification_service.dart';
import '../../features/profile/data/profile_repository.dart';
import '../../features/requests/data/requests_repository.dart';
import '../../features/files/data/files_repository.dart';

final GetIt getIt = GetIt.instance;

Future<void> configureDependencies() async {
  if (getIt.isRegistered<ApiConfig>()) return;

  final preferences = await SharedPreferences.getInstance();
  final secureStorage = SecureStorageService();

  getIt
    ..registerSingleton<ApiConfig>(ApiConfig.fromEnvironment())
    ..registerSingleton<PreferencesService>(PreferencesService(preferences))
    ..registerSingleton<SecureStorageService>(secureStorage)
    ..registerLazySingleton<BiometricAuthService>(
      () => BiometricAuthService(getIt<PreferencesService>()),
    )
    ..registerLazySingleton<DioClient>(
      () => DioClient(
        config: getIt<ApiConfig>(),
        secureStorage: getIt<SecureStorageService>(),
      ),
    )
    ..registerLazySingleton<AuthRepository>(
      () => AuthRepository(getIt<DioClient>(), getIt<SecureStorageService>()),
    )
    ..registerLazySingleton<AttendanceRepository>(
      () => AttendanceRepository(getIt<DioClient>()),
    )
    ..registerLazySingleton<HomeRepository>(
      () => HomeRepository(getIt<DioClient>()),
    )
    ..registerLazySingleton<ProfileRepository>(
      () => ProfileRepository(getIt<DioClient>()),
    )
    ..registerLazySingleton<RequestsRepository>(
      () => RequestsRepository(getIt<DioClient>()),
    )
    ..registerLazySingleton<FilesRepository>(
      () => FilesRepository(getIt<DioClient>()),
    )
    ..registerLazySingleton<NotificationRepository>(
      () => NotificationRepository(getIt<DioClient>()),
    )
    ..registerLazySingleton<PushNotificationService>(
      () => PushNotificationService(getIt<DioClient>()),
    );
}
