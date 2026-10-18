import '../../../core/errors/app_exception.dart';
import '../../../core/network/api_exceptions.dart';
import '../../../core/network/dio_client.dart';
import '../../../shared/models/api_response.dart';
import 'models/attendance_punch_model.dart';

class AttendanceRepository {
  const AttendanceRepository(this._client);

  final DioClient _client;

  Future<AttendancePunchModel> punch({required String punchType}) async {
    if (punchType != 'IN' && punchType != 'OUT') {
      throw const AppException(message: 'El tipo de marcación no es válido.');
    }

    try {
      final response = await _client.dio.post<Map<String, dynamic>>(
        '/api/me/attendance/punches',
        data: {'punchType': punchType},
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
}
