import 'package:dio/dio.dart';

import '../errors/app_exception.dart';

AppException mapDioException(Object error) {
  if (error is! DioException) {
    return AppException(message: error.toString());
  }

  final statusCode = error.response?.statusCode;
  final responseData = error.response?.data;
  final serverMessage = responseData is Map<String, dynamic>
      ? _serverMessage(responseData)
      : null;

  return AppException(
    message: serverMessage ?? _messageForType(error.type),
    statusCode: statusCode,
  );
}

String? _serverMessage(Map<String, dynamic> data) {
  final message = data['message'] as String?;
  final errors = data['errors'];
  if (errors is List) {
    final details = errors
        .whereType<String>()
        .where((value) => value.trim().isNotEmpty)
        .join(' ');
    if (details.isNotEmpty) {
      return message == null || message.trim().isEmpty
          ? details
          : '$message: $details';
    }
  }
  return message;
}

String _messageForType(DioExceptionType type) => switch (type) {
  DioExceptionType.connectionTimeout ||
  DioExceptionType.receiveTimeout ||
  DioExceptionType.sendTimeout => 'La solicitud tardó demasiado.',
  DioExceptionType.connectionError => 'No se pudo conectar con el servidor.',
  DioExceptionType.badResponse => 'El servidor rechazó la solicitud.',
  _ => 'Ocurrió un error de comunicación.',
};
