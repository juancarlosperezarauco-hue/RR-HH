class ApiErrorResponse {
  const ApiErrorResponse({
    required this.success,
    required this.message,
    required this.errors,
    required this.timestamp,
  });

  factory ApiErrorResponse.fromJson(Map<String, dynamic> json) {
    return ApiErrorResponse(
      success: json['success'] as bool? ?? false,
      message: json['message'] as String? ?? 'Ocurrió un error.',
      errors: (json['errors'] as List<dynamic>? ?? const [])
          .whereType<String>()
          .toList(growable: false),
      timestamp: DateTime.parse(json['timestamp'] as String),
    );
  }

  final bool success;
  final String message;
  final List<String> errors;
  final DateTime timestamp;
}
