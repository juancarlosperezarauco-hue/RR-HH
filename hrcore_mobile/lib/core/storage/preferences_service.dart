import 'package:shared_preferences/shared_preferences.dart';

class PreferencesService {
  const PreferencesService(this._preferences);

  final SharedPreferences _preferences;

  String? getString(String key) => _preferences.getString(key);

  bool getBool(String key) => _preferences.getBool(key) ?? false;

  Future<bool> setBool(String key, bool value) =>
      _preferences.setBool(key, value);

  Future<bool> setString(String key, String value) =>
      _preferences.setString(key, value);

  Future<bool> remove(String key) => _preferences.remove(key);
}
