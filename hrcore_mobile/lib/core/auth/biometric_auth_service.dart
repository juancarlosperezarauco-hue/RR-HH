import 'package:flutter/services.dart';
import 'package:local_auth/local_auth.dart';

import '../storage/preferences_service.dart';

class BiometricAuthService {
  BiometricAuthService(this._preferences);

  static const biometricEnabledKey = 'biometric_login_enabled';

  final LocalAuthentication _localAuthentication = LocalAuthentication();
  final PreferencesService _preferences;
  String? lastError;

  bool get isEnabled => _preferences.getBool(biometricEnabledKey);

  Future<bool> isAvailable() async {
    try {
      final supported = await _localAuthentication.isDeviceSupported();
      final enrolled = await _localAuthentication.canCheckBiometrics;
      return supported && enrolled;
    } on PlatformException catch (error) {
      lastError = error.message ?? error.code;
      return false;
    } catch (error) {
      lastError = error.toString();
      return false;
    }
  }

  Future<bool> authenticate() async {
    try {
      lastError = null;
      return await _localAuthentication.authenticate(
        localizedReason: 'Confirma tu identidad para abrir HRCore',
        options: const AuthenticationOptions(
          biometricOnly: true,
          stickyAuth: true,
          useErrorDialogs: true,
        ),
      );
    } on PlatformException catch (error) {
      lastError = error.message ?? error.code;
      return false;
    } catch (error) {
      lastError = error.toString();
      return false;
    }
  }

  Future<void> setEnabled(bool enabled) =>
      _preferences.setBool(biometricEnabledKey, enabled);
}
