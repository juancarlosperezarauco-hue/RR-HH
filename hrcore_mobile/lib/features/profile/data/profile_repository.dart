import '../../../core/errors/app_exception.dart';
import '../../../core/network/api_exceptions.dart';
import '../../../core/network/dio_client.dart';
import '../../../shared/models/api_response.dart';

class ProfileRepository {
  const ProfileRepository(this._client);

  final DioClient _client;

  Future<ProfileData> load() async {
    try {
      final responses = await Future.wait([
        _get('/api/auth/profile'),
        _get('/api/auth/me'),
        _get('/api/me/profile', allowFailure: true),
        _get('/api/me/employment', allowFailure: true),
      ]);
      return ProfileData(
        account: AccountProfile.fromJson(responses[0]),
        access: AccessProfile.fromJson(responses[1]),
        employee: responses[2] == null
            ? null
            : EmployeeProfile.fromJson(responses[2]! as Map<String, dynamic>),
        employment: responses[3] == null
            ? null
            : EmploymentSummary.fromJson(responses[3]! as Map<String, dynamic>),
      );
    } catch (error) {
      if (error is AppException) rethrow;
      throw mapDioException(error);
    }
  }

  Future<Object?> _get(String path, {bool allowFailure = false}) async {
    try {
      final response = await _client.dio.get<Map<String, dynamic>>(path);
      final body = response.data;
      if (body == null) {
        throw const AppException(
          message: 'La respuesta del servidor está vacía.',
        );
      }
      final result = ApiResponse<Object?>.fromJson(body, (value) => value);
      if (!result.success) throw AppException(message: result.message);
      return result.data;
    } catch (error) {
      if (allowFailure) return null;
      rethrow;
    }
  }
}

class ProfileData {
  const ProfileData({
    required this.account,
    required this.access,
    this.employee,
    this.employment,
  });

  final AccountProfile account;
  final AccessProfile access;
  final EmployeeProfile? employee;
  final EmploymentSummary? employment;
}

class AccountProfile {
  const AccountProfile({
    required this.id,
    required this.email,
    required this.firstName,
    required this.lastName,
    required this.active,
    required this.status,
    required this.tenantSlug,
    required this.createdAt,
  });

  factory AccountProfile.fromJson(Object? value) {
    final json = value! as Map<String, dynamic>;
    return AccountProfile(
      id: json['id']?.toString() ?? '',
      email: json['email']?.toString() ?? '',
      firstName: json['firstName']?.toString() ?? '',
      lastName: json['lastName']?.toString() ?? '',
      active: json['active'] as bool? ?? false,
      status: json['status']?.toString() ?? 'UNKNOWN',
      tenantSlug: json['tenantSlug']?.toString() ?? '',
      createdAt: DateTime.tryParse(json['createdAt']?.toString() ?? ''),
    );
  }

  final String id;
  final String email;
  final String firstName;
  final String lastName;
  final bool active;
  final String status;
  final String tenantSlug;
  final DateTime? createdAt;

  String get fullName => [
    firstName,
    lastName,
  ].where((part) => part.trim().isNotEmpty).join(' ').trim();
}

class AccessProfile {
  const AccessProfile({
    required this.owner,
    required this.roles,
    required this.permissions,
  });

  factory AccessProfile.fromJson(Object? value) {
    final json = value! as Map<String, dynamic>;
    return AccessProfile(
      owner: json['owner'] as bool? ?? false,
      roles: _strings(json['roles']),
      permissions: _strings(json['permissions']),
    );
  }

  final bool owner;
  final List<String> roles;
  final List<String> permissions;
}

class EmployeeProfile {
  const EmployeeProfile({
    required this.employeeNumber,
    required this.fullName,
    required this.documentType,
    required this.documentNumber,
    required this.birthDate,
    required this.personalEmail,
    required this.workEmail,
    required this.phone,
    required this.mobilePhone,
    required this.address,
    required this.status,
    required this.latestHireDate,
  });

  factory EmployeeProfile.fromJson(Map<String, dynamic> json) =>
      EmployeeProfile(
        employeeNumber: _text(json['employeeNumber']),
        fullName: _text(json['fullName']),
        documentType: _text(json['documentType']),
        documentNumber: _text(json['documentNumber']),
        birthDate: _date(json['birthDate']),
        personalEmail: _text(json['personalEmail']),
        workEmail: _text(json['workEmail']),
        phone: _text(json['phone']),
        mobilePhone: _text(json['mobilePhone']),
        address: _text(json['address']),
        status: _text(json['employmentStatus']),
        latestHireDate: _date(json['latestHireDate']),
      );

  final String employeeNumber;
  final String fullName;
  final String documentType;
  final String documentNumber;
  final DateTime? birthDate;
  final String personalEmail;
  final String workEmail;
  final String phone;
  final String mobilePhone;
  final String address;
  final String status;
  final DateTime? latestHireDate;
}

class EmploymentSummary {
  const EmploymentSummary({
    required this.status,
    required this.hireDate,
    required this.terminationDate,
    required this.currentPeriod,
  });

  factory EmploymentSummary.fromJson(Map<String, dynamic> json) =>
      EmploymentSummary(
        status: _text(json['employmentStatus']),
        hireDate: _date(json['hireDate']),
        terminationDate: _date(json['terminationDate']),
        currentPeriod: json['currentEmploymentPeriod'] is Map<String, dynamic>
            ? EmploymentPeriod.fromJson(
                json['currentEmploymentPeriod'] as Map<String, dynamic>,
              )
            : null,
      );

  final String status;
  final DateTime? hireDate;
  final DateTime? terminationDate;
  final EmploymentPeriod? currentPeriod;
}

class EmploymentPeriod {
  const EmploymentPeriod({
    required this.startDate,
    required this.endDate,
    required this.status,
  });

  factory EmploymentPeriod.fromJson(Map<String, dynamic> json) =>
      EmploymentPeriod(
        startDate: _date(json['startDate']),
        endDate: _date(json['endDate']),
        status: _text(json['status']),
      );

  final DateTime? startDate;
  final DateTime? endDate;
  final String status;
}

String _text(Object? value) => value?.toString().trim() ?? '';
DateTime? _date(Object? value) => DateTime.tryParse(_text(value));
List<String> _strings(Object? value) =>
    value is List ? value.whereType<String>().toList() : const [];
