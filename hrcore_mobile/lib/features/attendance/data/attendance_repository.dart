import '../../../core/errors/app_exception.dart';
import '../../../core/network/api_exceptions.dart';
import '../../../core/network/dio_client.dart';
import '../../../shared/models/api_response.dart';
import 'package:geolocator/geolocator.dart';
import 'models/attendance_punch_model.dart';

class AttendanceRepository {
  AttendanceRepository(this._client);

  final DioClient _client;
  String? _locationWarning;
  bool _locationNeedsAppSettings = false;
  final Map<String, String> _queryErrors = {};

  String? takeLocationWarning() {
    final warning = _locationWarning;
    _locationWarning = null;
    return warning;
  }

  Future<bool> openLocationSettings() {
    return _locationNeedsAppSettings
        ? Geolocator.openAppSettings()
        : Geolocator.openLocationSettings();
  }

  Future<AttendancePunchModel> punch({required String punchType}) async {
    if (!const {'IN', 'OUT', 'BREAK_START', 'BREAK_END'}.contains(punchType)) {
      throw const AppException(message: 'El tipo de marcación no es válido.');
    }

    try {
      final coordinates = await _currentCoordinates();
      final response = await _client.dio.post<Map<String, dynamic>>(
        '/api/me/attendance/punches',
        data: {
          'punchType': punchType,
          if (coordinates != null) ...coordinates,
        },
      );
      final body = response.data;
      if (body == null) {
        throw const AppException(
          message: 'La respuesta del servidor está vacía.',
        );
      }

      final result = ApiResponse<AttendancePunchModel>.fromJson(
        body,
        (value) => AttendancePunchModel.fromJson(value as Map<String, dynamic>),
      );
      if (!result.success) throw AppException(message: result.message);
      return result.data;
    } catch (error) {
      if (error is AppException) rethrow;
      throw mapDioException(error);
    }
  }

  /// Obtiene la ubicación actual sin convertir un permiso opcional en un
  /// bloqueo local. Si el tenant la exige, el backend rechazará la solicitud
  /// con su mensaje contractual; si no la exige, la marcación puede continuar.
  Future<Map<String, double>?> _currentCoordinates() async {
    _locationWarning = null;
    _locationNeedsAppSettings = false;
    try {
      if (!await Geolocator.isLocationServiceEnabled()) {
        _locationWarning =
            'La ubicación está desactivada. Actívala para validar tu marcación.';
        return null;
      }

      var permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }
      if (permission == LocationPermission.deniedForever) {
        _locationNeedsAppSettings = true;
        _locationWarning =
            'El permiso de ubicación está bloqueado. Habilítalo en los ajustes de la aplicación.';
        return null;
      }
      if (permission == LocationPermission.denied) {
        _locationNeedsAppSettings = true;
        _locationWarning =
            'El permiso de ubicación fue rechazado. Puedes habilitarlo en los ajustes de la aplicación.';
        return null;
      }

      final position = await Geolocator.getCurrentPosition(
        locationSettings: const LocationSettings(
          accuracy: LocationAccuracy.high,
          timeLimit: Duration(seconds: 10),
        ),
      );
      return {
        'latitude': position.latitude,
        'longitude': position.longitude,
      };
    } catch (_) {
      _locationWarning =
          'No se pudo obtener tu ubicación. Puedes revisar los permisos en los ajustes de la aplicación.';
      _locationNeedsAppSettings = true;
      return null;
    }
  }

  Future<List<Map<String, dynamic>>> records({
    String? from,
    String? to,
  }) async => _safePage('records',
    '/api/me/attendance?from=${from ?? ''}&to=${to ?? ''}&size=50&sort=workDate&direction=desc',
  );

  Future<List<Map<String, dynamic>>> punches({
    String? from,
    String? to,
  }) async => _safePage(
    'punches',
    '/api/me/attendance/punches?from=${Uri.encodeQueryComponent(from ?? '')}&to=${Uri.encodeQueryComponent(to ?? '')}&size=50',
  );

  Future<List<Map<String, dynamic>>> adjustments() async =>
      _safePage('adjustments', '/api/me/attendance/adjustments?size=25');

  Future<List<Map<String, dynamic>>> overtime() async =>
      _safePage('overtime', '/api/me/attendance/overtime?size=25');

  Future<Map<String, dynamic>?> scheduleRange({
    required String from,
    required String to,
  }) async => _safeGet('/api/me/schedule/range?fromDate=$from&toDate=$to');

  String? takeQueryError(String key) => _queryErrors.remove(key);

  Future<List<Map<String, dynamic>>> _safePage(String key, String path) async {
    _queryErrors.remove(key);
    try {
      return await _page(path);
    } catch (error) {
      _queryErrors[key] = error is AppException
          ? error.message
          : 'No se pudo consultar esta sección.';
      return const [];
    }
  }

  Future<Map<String, dynamic>?> _safeGet(String path) async {
    try {
      return await _get(path);
    } catch (_) {
      return null;
    }
  }

  Future<List<Map<String, dynamic>>> _page(String path) async {
    final data = await _get(path);
    final content = data?['content'];
    return content is List
        ? content.whereType<Map<String, dynamic>>().toList()
        : const [];
  }

  Future<Map<String, dynamic>?> _get(String path) async {
    try {
      final response = await _client.dio.get<Map<String, dynamic>>(path);
      final body = response.data;
      if (body == null) {
        throw const AppException(
          message: 'La respuesta del servidor está vacía.',
        );
      }
      if (body['success'] != true) {
        throw AppException(
          message: body['message']?.toString() ?? 'No se pudo cargar Jornada.',
        );
      }
      return body['data'] is Map<String, dynamic>
          ? body['data'] as Map<String, dynamic>
          : null;
    } catch (error) {
      if (error is AppException) rethrow;
      throw mapDioException(error);
    }
  }
}
