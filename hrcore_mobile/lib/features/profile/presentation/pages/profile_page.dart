import 'package:flutter/material.dart';
import 'package:get_it/get_it.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../../app/router/route_names.dart';
import '../../../../core/auth/biometric_auth_service.dart';
import '../../../../core/errors/app_exception.dart';
import '../../../auth/data/auth_repository.dart';
import '../../../notifications/data/notification_repository.dart';
import '../../../notifications/data/push_notification_service.dart';
import '../../data/profile_repository.dart';

class ProfilePage extends StatefulWidget {
  const ProfilePage({super.key});
  @override
  State<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends State<ProfilePage> {
  bool _biometricEnabled = false;
  bool _busy = false;
  bool _loading = true;
  bool _devicesLoading = true;
  String? _errorMessage;
  String? _devicesError;
  ProfileData? _profile;
  List<NotificationDevice> _devices = const [];

  @override
  void initState() {
    super.initState();
    _biometricEnabled = GetIt.I<BiometricAuthService>().isEnabled;
    _load();
  }

  Future<void> _load() async {
    if (mounted) {
      setState(() {
        _loading = true;
        _errorMessage = null;
        _profile = null;
      });
    }
    try {
      final profile = await GetIt.I<ProfileRepository>().load();
      if (mounted) {
        setState(() {
          _profile = profile;
          _loading = false;
        });
      }
    } catch (error) {
      if (mounted) {
        setState(() {
          _loading = false;
          _errorMessage = error is AppException
              ? error.message
              : 'No se pudo cargar tu perfil.';
        });
      }
    }
    await _loadDevices();
  }

  Future<List<NotificationDevice>?> _loadDevices() async {
    if (mounted) {
      setState(() {
        _devicesLoading = true;
        _devicesError = null;
      });
    }
    try {
      final devices = await GetIt.I<NotificationRepository>().listDevices();
      if (mounted) {
        setState(() {
          _devices = devices;
          _devicesLoading = false;
        });
      }
      return devices;
    } catch (error) {
      if (mounted) {
        setState(() {
          _devicesLoading = false;
          _devicesError = error is AppException
              ? error.message
              : 'No se pudieron cargar los dispositivos.';
        });
      }
      return null;
    }
  }

  Future<void> _registerDevice() async {
    if (mounted) setState(() => _busy = true);
    try {
      final registeredDevice = await GetIt.I<PushNotificationService>()
          .registerCurrentDevice();
      final devices = await _loadDevices();
      if (mounted) {
        final confirmed =
            registeredDevice != null ||
            devices?.any((device) => device.status == 'ACTIVE') == true;
        final confirmedDevice =
            registeredDevice ??
            (devices == null || devices.isEmpty
                ? null
                : devices.firstWhere(
                    (device) => device.status == 'ACTIVE',
                    orElse: () => devices.first,
                  ));
        if (registeredDevice != null &&
            (devices == null ||
                !devices.any((device) => device.id == registeredDevice.id))) {
          setState(() {
            _devices = [
              registeredDevice,
              ..._devices.where((device) => device.id != registeredDevice.id),
            ];
          });
        }
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              confirmed
                  ? 'Dispositivo registrado · ID ${_shortId(confirmedDevice?.id ?? '')}'
                  : 'No se pudo confirmar el registro del dispositivo.',
            ),
          ),
        );
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('No se pudo registrar este dispositivo.'),
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _deactivateDevice(String id) async {
    if (mounted) setState(() => _busy = true);
    try {
      await GetIt.I<NotificationRepository>().deactivateDevice(id);
      await _loadDevices();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('No se pudo desactivar el dispositivo.'),
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _logout() async {
    if (mounted) setState(() => _busy = true);
    try {
      final biometric = GetIt.I<BiometricAuthService>();
      await GetIt.I<AuthRepository>().logout(
        preserveBiometricSession: biometric.isEnabled,
      );
      if (mounted) context.go(RouteNames.loginPath);
    } catch (error) {
      if (mounted) {
        setState(() => _busy = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              error is AppException
                  ? error.message
                  : 'No se pudo cerrar la sesión.',
            ),
          ),
        );
      }
    }
  }

  Future<void> _toggleBiometric(bool enabled) async {
    final biometric = GetIt.I<BiometricAuthService>();
    if (enabled && !await biometric.isAvailable()) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Este dispositivo no tiene biometría disponible.'),
          ),
        );
      }
      return;
    }
    if (enabled && !await biometric.authenticate()) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              'No se pudo validar la biometría${biometric.lastError == null ? '.' : ': ${biometric.lastError}'}',
            ),
          ),
        );
      }
      return;
    }
    await biometric.setEnabled(enabled);
    if (mounted) setState(() => _biometricEnabled = enabled);
  }

  Future<void> _changePassword() async {
    final current = TextEditingController();
    final next = TextEditingController();
    final formKey = GlobalKey<FormState>();
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Cambiar contraseña'),
        content: Form(
          key: formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextFormField(
                controller: current,
                obscureText: true,
                decoration: const InputDecoration(
                  labelText: 'Contraseña actual',
                ),
                validator: (v) => v == null || v.isEmpty
                    ? 'Ingresa tu contraseña actual'
                    : null,
              ),
              TextFormField(
                controller: next,
                obscureText: true,
                decoration: const InputDecoration(
                  labelText: 'Nueva contraseña',
                ),
                validator: (v) => v == null || v.length < 8
                    ? 'Usa al menos 8 caracteres'
                    : null,
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () {
              if (formKey.currentState!.validate()) {
                Navigator.pop(dialogContext, true);
              }
            },
            child: const Text('Guardar'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) {
      current.dispose();
      next.dispose();
      return;
    }
    try {
      await GetIt.I<AuthRepository>().changePassword(
        currentPassword: current.text,
        newPassword: next.text,
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Contraseña actualizada.')),
        );
        // The backend increments the security version when changing the
        // password, invalidating the current session and its refresh token.
        // Clear local credentials and require a fresh login instead of
        // leaving the user in a screen that will fail on the next request.
        await GetIt.I<BiometricAuthService>().setEnabled(false);
        await GetIt.I<AuthRepository>().logout();
        if (mounted) context.go(RouteNames.loginPath);
      }
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              error is AppException
                  ? error.message
                  : 'No se pudo cambiar la contraseña.',
            ),
          ),
        );
      }
    }
    current.dispose();
    next.dispose();
  }

  Future<void> _revokeOtherSessions() async {
    if (_busy) return;
    if (mounted) setState(() => _busy = true);
    try {
      await GetIt.I<AuthRepository>().revokeOtherSessions();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Las otras sesiones fueron cerradas.')),
        );
      }
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              error is AppException
                  ? error.message
                  : 'No se pudieron cerrar las otras sesiones.',
            ),
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final data = _profile;
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 28),
        children: [
          if (_loading) const LinearProgressIndicator(minHeight: 2),
          if (_errorMessage != null)
            _ErrorBanner(message: _errorMessage!, onRetry: _load),
          if (data != null) ...[
            _ProfileHeader(data: data),
            const SizedBox(height: 12),
            _AccountSummary(data: data),
            _PersonalSection(data: data),
            _EmploymentSection(data: data),
          ],
          const SizedBox(height: 8),
          _SecuritySection(
            biometricEnabled: _biometricEnabled,
            busy: _busy,
            onBiometricChanged: _toggleBiometric,
            onChangePassword: _changePassword,
            onRevokeSessions: _revokeOtherSessions,
          ),
          _DevicesSection(
            devices: _devices,
            busy: _busy,
            loading: _devicesLoading,
            errorMessage: _devicesError,
            onRegister: _registerDevice,
            onDeactivate: _deactivateDevice,
          ),
          const SizedBox(height: 16),
          FilledButton.tonalIcon(
            onPressed: _busy ? null : _logout,
            icon: const Icon(Icons.logout),
            label: const Text('Cerrar sesión'),
          ),
        ],
      ),
    );
  }
}

class _ProfileHeader extends StatelessWidget {
  const _ProfileHeader({required this.data});
  final ProfileData data;
  @override
  Widget build(BuildContext context) {
    final account = data.account;
    final name = account.fullName.isEmpty ? account.email : account.fullName;
    final initials = name
        .trim()
        .split(RegExp(r'\s+'))
        .where((part) => part.isNotEmpty)
        .take(2)
        .map((part) => part[0])
        .join()
        .toUpperCase();
    final colors = Theme.of(context).colorScheme;
    return Card(
      margin: EdgeInsets.zero,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            CircleAvatar(
              radius: 30,
              backgroundColor: colors.primaryContainer,
              child: Text(
                initials.isEmpty ? '?' : initials,
                style: TextStyle(
                  color: colors.primary,
                  fontSize: 20,
                  fontWeight: FontWeight.w800,
                ),
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    name,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.titleLarge?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: 3),
                  Text(
                    account.email,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: colors.onSurfaceVariant,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 6,
                    runSpacing: 4,
                    children: [
                      _StatusChip(
                        label: _statusLabel(account.status),
                        active: account.active,
                      ),
                      if (data.access.owner)
                        const _StatusChip(label: 'Propietario', active: true),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _AccountSummary extends StatelessWidget {
  const _AccountSummary({required this.data});
  final ProfileData data;
  @override
  Widget build(BuildContext context) => _SectionCard(
    title: 'Cuenta y acceso',
    icon: Icons.account_circle_outlined,
    children: [
      _InfoRow(label: 'Organización', value: _value(data.account.tenantSlug)),
      _InfoRow(
        label: 'Roles',
        value: data.access.roles.isEmpty
            ? 'Sin roles asignados'
            : data.access.roles.join(', '),
      ),
      _InfoRow(label: 'Cuenta creada', value: _date(data.account.createdAt)),
    ],
  );
}

class _PersonalSection extends StatelessWidget {
  const _PersonalSection({required this.data});
  final ProfileData data;
  @override
  Widget build(BuildContext context) {
    final employee = data.employee;
    return _SectionCard(
      title: 'Información personal',
      icon: Icons.badge_outlined,
      initiallyExpanded: false,
      children: [
        _InfoRow(
          label: 'Nombre completo',
          value: _value(employee?.fullName, fallback: data.account.fullName),
        ),
        _InfoRow(
          label: 'Correo laboral',
          value: _value(employee?.workEmail, fallback: data.account.email),
        ),
        _InfoRow(
          label: 'Correo personal',
          value: _value(employee?.personalEmail),
        ),
        _InfoRow(
          label: 'Teléfono',
          value: _value(employee?.mobilePhone, fallback: employee?.phone),
        ),
        _InfoRow(
          label: 'Documento',
          value: _join(employee?.documentType, employee?.documentNumber),
        ),
        _InfoRow(label: 'Dirección', value: _value(employee?.address)),
      ],
    );
  }
}

class _EmploymentSection extends StatelessWidget {
  const _EmploymentSection({required this.data});
  final ProfileData data;
  @override
  Widget build(BuildContext context) {
    final employee = data.employee;
    final employment = data.employment;
    return _SectionCard(
      title: 'Situación laboral',
      icon: Icons.work_outline,
      initiallyExpanded: false,
      children: [
        _InfoRow(
          label: 'Número de empleado',
          value: _value(employee?.employeeNumber),
        ),
        _InfoRow(
          label: 'Estado',
          value: _statusLabel(
            employment?.status.isNotEmpty == true
                ? employment!.status
                : employee?.status,
          ),
        ),
        _InfoRow(
          label: 'Fecha de ingreso',
          value: _date(employment?.hireDate ?? employee?.latestHireDate),
        ),
        if (employment?.terminationDate != null)
          _InfoRow(
            label: 'Fecha de baja',
            value: _date(employment!.terminationDate),
          ),
        if (employment?.currentPeriod != null)
          _InfoRow(
            label: 'Periodo actual',
            value: _period(employment!.currentPeriod!),
          ),
      ],
    );
  }
}

class _SecuritySection extends StatelessWidget {
  const _SecuritySection({
    required this.biometricEnabled,
    required this.busy,
    required this.onBiometricChanged,
    required this.onChangePassword,
    required this.onRevokeSessions,
  });
  final bool biometricEnabled;
  final bool busy;
  final ValueChanged<bool> onBiometricChanged;
  final VoidCallback onChangePassword;
  final VoidCallback onRevokeSessions;
  @override
  Widget build(BuildContext context) => _SectionCard(
    title: 'Seguridad',
    icon: Icons.security_outlined,
    initiallyExpanded: false,
    children: [
      SwitchListTile.adaptive(
        contentPadding: EdgeInsets.zero,
        value: biometricEnabled,
        onChanged: busy ? null : onBiometricChanged,
        secondary: const Icon(Icons.fingerprint),
        title: const Text('Acceso biométrico'),
        subtitle: const Text(
          'Usa la huella o Face ID para desbloquear la sesión.',
        ),
      ),
      ListTile(
        contentPadding: EdgeInsets.zero,
        leading: const Icon(Icons.password_outlined),
        title: const Text('Cambiar contraseña'),
        onTap: busy ? null : onChangePassword,
      ),
      ListTile(
        contentPadding: EdgeInsets.zero,
        leading: const Icon(Icons.devices_outlined),
        title: const Text('Cerrar otras sesiones'),
        onTap: busy ? null : onRevokeSessions,
      ),
    ],
  );
}

class _DevicesSection extends StatelessWidget {
  const _DevicesSection({
    required this.devices,
    required this.busy,
    required this.loading,
    required this.errorMessage,
    required this.onRegister,
    required this.onDeactivate,
  });
  final List<NotificationDevice> devices;
  final bool busy;
  final bool loading;
  final String? errorMessage;
  final VoidCallback onRegister;
  final ValueChanged<String> onDeactivate;
  @override
  Widget build(BuildContext context) => _SectionCard(
    title: 'Notificaciones push',
    icon: Icons.notifications_outlined,
    initiallyExpanded: false,
    children: [
      if (loading)
        const Padding(
          padding: EdgeInsets.only(bottom: 8),
          child: LinearProgressIndicator(minHeight: 2),
        )
      else if (errorMessage != null)
        Padding(
          padding: const EdgeInsets.only(bottom: 8),
          child: Text(
            errorMessage!,
            style: TextStyle(color: Theme.of(context).colorScheme.error),
          ),
        )
      else if (devices.isEmpty)
        const Padding(
          padding: EdgeInsets.only(bottom: 8),
          child: Text(
            'No hay dispositivos registrados para recibir alertas push.',
          ),
        )
      else
        for (final device in devices)
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: Icon(
              device.platform == 'IOS'
                  ? Icons.phone_iphone
                  : Icons.phone_android,
            ),
            title: Text(device.deviceName),
            subtitle: Text(
              '${device.platform} · ${_statusLabel(device.status)} · ID ${_shortId(device.id)}',
            ),
            trailing: device.status == 'ACTIVE'
                ? IconButton(
                    icon: const Icon(Icons.notifications_off_outlined),
                    tooltip: 'Desactivar',
                    onPressed: busy ? null : () => onDeactivate(device.id),
                  )
                : const Icon(Icons.notifications_off_outlined),
          ),
      OutlinedButton.icon(
        onPressed: busy ? null : onRegister,
        icon: const Icon(Icons.add_alert_outlined),
        label: const Text('Registrar este dispositivo'),
      ),
    ],
  );
}

class _SectionCard extends StatelessWidget {
  const _SectionCard({
    required this.title,
    required this.icon,
    required this.children,
    this.initiallyExpanded = true,
  });
  final String title;
  final IconData icon;
  final List<Widget> children;
  final bool initiallyExpanded;
  @override
  Widget build(BuildContext context) => Card(
    margin: const EdgeInsets.only(bottom: 8),
    child: ExpansionTile(
      initiallyExpanded: initiallyExpanded,
      tilePadding: const EdgeInsets.symmetric(horizontal: 16),
      childrenPadding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
      leading: Icon(icon, color: Theme.of(context).colorScheme.primary),
      title: Text(title, style: const TextStyle(fontWeight: FontWeight.w700)),
      children: children,
    ),
  );
}

class _InfoRow extends StatelessWidget {
  const _InfoRow({required this.label, required this.value});
  final String label;
  final String value;
  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 7),
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: Text(
            label,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: Theme.of(context).colorScheme.onSurfaceVariant,
            ),
          ),
        ),
        const SizedBox(width: 12),
        Flexible(
          child: Text(
            value,
            textAlign: TextAlign.end,
            style: const TextStyle(fontWeight: FontWeight.w600),
          ),
        ),
      ],
    ),
  );
}

class _StatusChip extends StatelessWidget {
  const _StatusChip({required this.label, required this.active});
  final String label;
  final bool active;
  @override
  Widget build(BuildContext context) => Chip(
    label: Text(label),
    visualDensity: VisualDensity.compact,
    avatar: Icon(
      active ? Icons.check_circle_outline : Icons.info_outline,
      size: 16,
    ),
  );
}

class _ErrorBanner extends StatelessWidget {
  const _ErrorBanner({required this.message, required this.onRetry});
  final String message;
  final VoidCallback onRetry;
  @override
  Widget build(BuildContext context) => Card(
    color: Theme.of(context).colorScheme.errorContainer,
    child: ListTile(
      leading: Icon(
        Icons.error_outline,
        color: Theme.of(context).colorScheme.onErrorContainer,
      ),
      title: Text(message),
      trailing: TextButton(onPressed: onRetry, child: const Text('Reintentar')),
    ),
  );
}

String _value(String? value, {String? fallback}) {
  final result = value?.trim();
  if (result != null && result.isNotEmpty) return result;
  final backup = fallback?.trim();
  return backup == null || backup.isEmpty ? 'No disponible' : backup;
}

String _join(String? first, String? second) => _value(
  [
    first,
    second,
  ].where((part) => part != null && part.trim().isNotEmpty).join(' · '),
);
String _date(DateTime? value) => value == null
    ? 'No disponible'
    : DateFormat('dd/MM/yyyy').format(value.toLocal());
String _period(EmploymentPeriod period) =>
    '${_date(period.startDate)} — ${period.endDate == null ? 'Vigente' : _date(period.endDate)}';
String _shortId(String id) => id.length <= 12 ? id : '${id.substring(0, 8)}…';

String _statusLabel(String? status) => switch (status?.toUpperCase()) {
  'ACTIVE' => 'Activo',
  'INACTIVE' => 'Inactivo',
  'PENDING' => 'Pendiente',
  'SUSPENDED' => 'Suspendido',
  'ON_LEAVE' => 'Con licencia',
  'TERMINATED' => 'Finalizado',
  _ => _value(status),
};
