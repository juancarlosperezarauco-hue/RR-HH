import 'package:get_it/get_it.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../core/config/api_config.dart';
import '../../core/network/dio_client.dart';
import '../../core/storage/preferences_service.dart';
import '../../core/storage/secure_storage_service.dart';
import '../../features/auth/data/auth_repository.dart';
import '../../features/attendance/data/attendance_repository.dart';

final GetIt getIt = GetIt.instance;

Future<void> configureDependencies() async {
  if (getIt.isRegistered<ApiConfig>()) return;

  final preferences = await SharedPreferences.getInstance();
  final secureStorage = SecureStorageService();

  getIt
    ..registerSingleton<ApiConfig>(ApiConfig.fromEnvironment())
    ..registerSingleton<PreferencesService>(PreferencesService(preferences))
    ..registerSingleton<SecureStorageService>(secureStorage)
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
    );
}
