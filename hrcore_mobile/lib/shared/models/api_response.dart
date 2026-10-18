class ApiResponse<T> {
  const ApiResponse({
    required this.success,
    required this.message,
    required this.data,
    required this.timestamp,
  });

  factory ApiResponse.fromJson(
    Map<String, dynamic> json,
    T Function(Object? value) parseData,
  ) {
    return ApiResponse<T>(
      success: json['success'] as bool? ?? false,
      message: json['message'] as String? ?? '',
      data: parseData(json['data']),
      timestamp: DateTime.parse(json['timestamp'] as String),
    );
  }

  final bool success;
  final String message;
  final T data;
  final DateTime timestamp;
}
