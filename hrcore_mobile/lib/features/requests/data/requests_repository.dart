import '../../../core/errors/app_exception.dart';
import '../../../core/network/api_exceptions.dart';
import '../../../core/network/dio_client.dart';

class RequestsRepository {
  const RequestsRepository(this._client);
  final DioClient _client;

  Future<RequestsData> load() async {
    final results = await Future.wait<_PageResult>([
      _capturePage(
        '/api/me/leaves/balances?year=${DateTime.now().year}&size=50',
        'saldos',
      ),
      _capturePage(
        '/api/me/leaves/requests?size=50&sort=createdAt,desc',
        'solicitudes',
      ),
      _capturePage(
        '/api/leaves/types?status=ACTIVE&size=50&sort=code,asc',
        'tipos de permiso',
      ),
    ]);
    final profile = await _object('/api/me/profile');
    final employeeId = profile?['employeeId']?.toString();
    final incidentResult = employeeId == null
        ? const _PageResult(
            <Map<String, dynamic>>[],
            'No se pudo identificar tu relación laboral para cargar incidencias.',
          )
        : await _capturePage(
            '/api/incidents?employeeId=$employeeId&size=25&sort=occurredAt,desc',
            'incidencias personales',
          );
    final issues = <String>[];
    for (final result in results) {
      if (result.issue != null) issues.add(result.issue!);
    }
    if (incidentResult.issue != null) issues.add(incidentResult.issue!);
    return RequestsData(
      balances: results[0].items,
      requests: results[1].items,
      incidents: incidentResult.items,
      leaveTypes: results[2].items,
      issues: issues,
    );
  }

  Future<List<Map<String, dynamic>>> leaveTypes() =>
      _page('/api/leaves/types?status=ACTIVE&size=50&sort=code,asc');

  Future<void> createAndSubmit({
    required String leaveTypeId,
    required DateTime startDate,
    required DateTime endDate,
    required String reason,
  }) async {
    final created = await _request(
      'POST',
      '/api/me/leaves/requests',
      data: {
        'leaveTypeId': leaveTypeId,
        'startDate': _date(startDate),
        'endDate': _date(endDate),
        'reason': reason.trim(),
      },
    );
    final id = created?['id']?.toString();
    if (id == null || id.isEmpty)
      throw const AppException(
        message: 'La solicitud no devolvió un identificador.',
      );
    try {
      await _request('PATCH', '/api/me/leaves/requests/$id/submit');
    } on AppException catch (error) {
      throw AppException(
        message:
            'El borrador fue creado, pero no pudo enviarse (ID: $id). ${error.message}',
        statusCode: error.statusCode,
      );
    }
  }

  Future<Map<String, dynamic>?> _request(
    String method,
    String path, {
    Map<String, dynamic>? data,
  }) async {
    try {
      final response = method == 'POST'
          ? await _client.dio.post<Map<String, dynamic>>(path, data: data)
          : await _client.dio.patch<Map<String, dynamic>>(path, data: data);
      final body = response.data;
      if (body == null || body['success'] != true)
        throw AppException(
          message:
              body?['message']?.toString() ?? 'No se pudo enviar la solicitud.',
        );
      return body['data'] is Map<String, dynamic>
          ? body['data'] as Map<String, dynamic>
          : null;
    } catch (error) {
      if (error is AppException) rethrow;
      throw mapDioException(error);
    }
  }

  Future<Map<String, dynamic>?> _object(String path) async {
    try {
      final response = await _client.dio.get<Map<String, dynamic>>(path);
      final body = response.data;
      if (body?['success'] != true) return null;
      return body?['data'] is Map<String, dynamic>
          ? body!['data'] as Map<String, dynamic>
          : null;
    } catch (_) {
      return null;
    }
  }

  Future<_PageResult> _capturePage(String path, String label) async {
    try {
      return _PageResult(await _page(path), null);
    } on AppException catch (error) {
      return _PageResult(
        const [],
        'No se pudieron cargar $label. ${error.message}',
      );
    }
  }

  Future<List<Map<String, dynamic>>> _page(String path) async {
    try {
      final response = await _client.dio.get<Map<String, dynamic>>(path);
      final body = response.data;
      if (body == null)
        throw const AppException(
          message: 'La respuesta del servidor está vacía.',
        );
      if (body['success'] != true)
        throw AppException(
          message:
              body['message']?.toString() ??
              'No se pudieron cargar las solicitudes.',
        );
      final data = body['data'];
      final content = data is Map<String, dynamic> ? data['content'] : null;
      return content is List
          ? content.whereType<Map<String, dynamic>>().toList()
          : const [];
    } catch (error) {
      if (error is AppException) rethrow;
      throw mapDioException(error);
    }
  }
}

String _date(DateTime value) =>
    '${value.year.toString().padLeft(4, '0')}-${value.month.toString().padLeft(2, '0')}-${value.day.toString().padLeft(2, '0')}';

class RequestsData {
  const RequestsData({
    required this.balances,
    required this.requests,
    required this.incidents,
    required this.leaveTypes,
    this.issues = const [],
  });
  final List<Map<String, dynamic>> balances;
  final List<Map<String, dynamic>> requests;
  final List<Map<String, dynamic>> incidents;
  final List<Map<String, dynamic>> leaveTypes;
  final List<String> issues;
}

class _PageResult {
  const _PageResult(this.items, this.issue);
  final List<Map<String, dynamic>> items;
  final String? issue;
}
