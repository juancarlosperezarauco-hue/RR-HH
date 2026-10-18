class AttendancePunchModel {
  const AttendancePunchModel({
    required this.punchType,
    required this.punchedAt,
  });

  factory AttendancePunchModel.fromJson(Map<String, dynamic> json) {
    return AttendancePunchModel(
      punchType: json['punchType'] as String? ?? '',
      punchedAt: DateTime.parse(json['punchedAt'] as String),
    );
  }

  final String punchType;
  final DateTime punchedAt;
}
