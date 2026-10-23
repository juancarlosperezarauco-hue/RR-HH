import '../../../core/errors/app_exception.dart';
import '../../../core/network/api_exceptions.dart';
import '../../../core/network/dio_client.dart';

class HomeRepository {
  const HomeRepository(this._client);

  final DioClient _client;

  Future<HomeData> load() async {
    final results = await Future.wait([
      _get('/api/auth/profile'),
      _get('/api/auth/me', allowFailure: true),
      _capture('/api/me/employment', 'situación laboral'),
      _capture(
        '/api/me/attendance?from=${_today()}&to=${_today()}&size=1&sort=workDate&direction=desc',
        'asistencia',
      ),
      _capture(
        '/api/me/leaves/balances?year=${DateTime.now().year}&size=100',
        'saldos de vacaciones',
      ),
      _capture(
        '/api/me/leaves/requests?size=100&sort=createdAt,desc',
        'solicitudes',
      ),
      _capture(
        '/api/me/schedule/range?fromDate=${_today()}&toDate=${_afterDays(7)}',
        'turnos',
      ),
      _capture('/api/my-team/employees?size=100', 'equipo'),
    ]);
    final issues = <String>[];
    for (final result in results.skip(2)) {
      if (result is _LoadResult && result.issue != null)
        issues.add(result.issue!);
    }

    return HomeData(
      profile: {...?_map(results[0]), ...?_map(results[1])},
      employment: _map(_resultData(results[2])),
      attendance: _pageFirst(_resultData(results[3])),
      balances: _pageItems(_resultData(results[4])),
      requests: _pageItems(_resultData(results[5])),
      schedule: _nextSchedule(_resultData(results[6])),
      team: _pageItems(_resultData(results[7])),
      issues: issues,
    );
  }

  Future<Object?> _get(String path, {bool allowFailure = false}) async {
    try {
      final response = await _client.dio.get<Map<String, dynamic>>(path);
      final body = response.data;
      if (body == null) throw const AppException(message: 'Respuesta vacía.');
      if (body['success'] != true) {
        throw AppException(
          message:
              body['message']?.toString() ?? 'No se pudo cargar el resumen.',
        );
      }
      return body['data'];
    } catch (error) {
      if (allowFailure) return null;
      if (error is AppException) rethrow;
      throw mapDioException(error);
    }
  }

  Future<_LoadResult> _capture(String path, String label) async {
    try {
      return _LoadResult(await _get(path), null);
    } on AppException catch (error) {
      return _LoadResult(null, 'No se pudo cargar $label. ${error.message}');
    }
  }

  static Object? _resultData(Object? value) =>
      value is _LoadResult ? value.data : value;

  static Map<String, dynamic>? _map(Object? value) =>
      value is Map<String, dynamic> ? value : null;

  static List<Map<String, dynamic>> _pageItems(Object? value) {
    final content = value is Map<String, dynamic> ? value['content'] : null;
    if (content is! List) return const [];
    return content.whereType<Map<String, dynamic>>().toList();
  }

  static Map<String, dynamic>? _pageFirst(Object? value) {
    final items = _pageItems(value);
    return items.isEmpty ? null : items.first;
  }

  static Map<String, dynamic>? _nextSchedule(Object? value) {
    if (value is! Map<String, dynamic>) return null;
    final days = value['days'];
    if (days is! List) return null;
    for (final day in days.whereType<Map<String, dynamic>>()) {
      final intervals = day['intervals'];
      if (intervals is! List || intervals.isEmpty) continue;
      final now = DateTime.now();
      final hasUpcomingInterval = intervals
          .whereType<Map<String, dynamic>>()
          .any((interval) {
            final end = DateTime.tryParse(
              interval['end']?.toString() ?? '',
            )?.toLocal();
            return end == null || !end.isBefore(now);
          });
      if (hasUpcomingInterval) return day;
    }
    return null;
  }

  static String _today() {
    final now = DateTime.now();
    return '${now.year.toString().padLeft(4, '0')}-${now.month.toString().padLeft(2, '0')}-${now.day.toString().padLeft(2, '0')}';
  }

  static String _afterDays(int days) {
    final date = DateTime.now().add(Duration(days: days));
    return '${date.year.toString().padLeft(4, '0')}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
  }
}

class HomeData {
  const HomeData({
    required this.profile,
    required this.employment,
    required this.attendance,
    required this.balances,
    required this.requests,
    required this.schedule,
    required this.team,
    this.issues = const [],
  });

  final Map<String, dynamic>? profile;
  final Map<String, dynamic>? employment;
  final Map<String, dynamic>? attendance;
  final List<Map<String, dynamic>> balances;
  final List<Map<String, dynamic>> requests;
  final Map<String, dynamic>? schedule;
  final List<Map<String, dynamic>> team;
  final List<String> issues;
}

class _LoadResult {
  const _LoadResult(this.data, this.issue);
  final Object? data;
  final String? issue;
}
