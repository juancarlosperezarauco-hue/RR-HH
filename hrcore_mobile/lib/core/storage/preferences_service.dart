import 'package:shared_preferences/shared_preferences.dart';

class PreferencesService {
  const PreferencesService(this._preferences);

  final SharedPreferences _preferences;

  String? getString(String key) => _preferences.getString(key);

  Future<bool> setString(String key, String value) =>
      _preferences.setString(key, value);

  Future<bool> remove(String key) => _preferences.remove(key);
}
