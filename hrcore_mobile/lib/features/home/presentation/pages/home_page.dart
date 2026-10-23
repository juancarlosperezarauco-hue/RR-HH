import 'package:flutter/material.dart';
import 'package:get_it/get_it.dart';
import 'package:intl/intl.dart';

import '../../../../core/errors/app_exception.dart';
import '../../data/home_repository.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key, this.onOpenSection, this.onOpenTeam});
  final ValueChanged<int>? onOpenSection;
  final ValueChanged<List<Map<String, dynamic>>>? onOpenTeam;
  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  HomeData? _data;
  String? _error;
  bool _loading = true;
  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final data = await GetIt.I<HomeRepository>().load();
      if (mounted)
        setState(() {
          _data = data;
          _loading = false;
        });
    } on AppException catch (error) {
      if (mounted)
        setState(() {
          _error = error.message;
          _loading = false;
        });
    } catch (_) {
      if (mounted)
        setState(() {
          _error = 'No se pudo cargar tu resumen laboral.';
          _loading = false;
        });
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null && _data == null)
      return RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          children: [
            const SizedBox(height: 180),
            Icon(Icons.cloud_off_outlined, size: 44),
            const SizedBox(height: 12),
            Center(child: Text(_error!, textAlign: TextAlign.center)),
            Center(
              child: TextButton.icon(
                onPressed: _load,
                icon: const Icon(Icons.refresh),
                label: const Text('Reintentar'),
              ),
            ),
          ],
        ),
      );
    final data = _data!;
    final profile = data.profile ?? const <String, dynamic>{};
    final employment = data.employment ?? const <String, dynamic>{};
    final name = _firstNonBlank(
      profile['firstName'],
      profile['fullName'],
      'tu jornada',
    );
    final status = _label(employment['employmentStatus'] ?? profile['status']);
    final balancesWithDays = data.balances
        .where((item) => _number(item['availableDays']) > 0)
        .length;
    final pending = data.requests
        .where((item) => item['status']?.toString().toUpperCase() == 'PENDING')
        .length;
    final teamAccess = _hasTeamAccess(profile);
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(20, 16, 20, 32),
        children: [
          _WelcomeHeader(name: name, date: DateTime.now()),
          const SizedBox(height: 20),
          _StatusStrip(status: status),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: _MetricCard(
                  icon: Icons.fact_check_outlined,
                  label: 'Asistencia de hoy',
                  value: _attendanceLabel(data.attendance),
                  color: theme.colorScheme.primary,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _MetricCard(
                  icon: Icons.beach_access_outlined,
                  label: 'Tipos con saldo',
                  value: '$balancesWithDays',
                  color: Colors.teal.shade700,
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          _Heading(
            title: 'Hoy',
            action: 'Ver jornada',
            onPressed: () => widget.onOpenSection?.call(1),
          ),
          const SizedBox(height: 10),
          _ScheduleCard(schedule: data.schedule, attendance: data.attendance),
          const SizedBox(height: 20),
          _Heading(
            title: 'Pendientes',
            action: 'Ver solicitudes',
            onPressed: () => widget.onOpenSection?.call(2),
          ),
          const SizedBox(height: 10),
          _PendingCard(
            count: pending,
            onPressed: () => widget.onOpenSection?.call(2),
          ),
          if (teamAccess) ...[
            const SizedBox(height: 20),
            _TeamCard(
              count: data.team.length,
              onPressed: () => widget.onOpenTeam?.call(data.team),
            ),
          ],
          const SizedBox(height: 20),
          _QuickLinks(onOpenSection: widget.onOpenSection),
          if (data.issues.isNotEmpty) ...[
            const SizedBox(height: 14),
            _LoadIssues(items: data.issues),
          ],
          if (_error != null) ...[
            const SizedBox(height: 14),
            Text(_error!, style: TextStyle(color: theme.colorScheme.error)),
          ],
        ],
      ),
    );
  }
}

class _WelcomeHeader extends StatelessWidget {
  const _WelcomeHeader({required this.name, required this.date});
  final String name;
  final DateTime date;
  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Text(
        'Hola, $name',
        style: Theme.of(
          context,
        ).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800),
      ),
      const SizedBox(height: 3),
      Text(
        DateFormat('dd/MM/yyyy').format(date),
        style: Theme.of(context).textTheme.bodyMedium?.copyWith(
          color: Theme.of(context).colorScheme.onSurfaceVariant,
        ),
      ),
    ],
  );
}

class _StatusStrip extends StatelessWidget {
  const _StatusStrip({required this.status});
  final String status;
  @override
  Widget build(BuildContext context) {
    final inactive = {'Inactiva', 'Suspendida', 'Finalizada'}.contains(status);
    final color = inactive
        ? Theme.of(context).colorScheme.error
        : const Color(0xFF237A3B);
    final background = inactive
        ? Theme.of(context).colorScheme.errorContainer
        : const Color(0xFFEAF7EE);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        children: [
          Icon(
            inactive ? Icons.warning_amber_outlined : Icons.verified_outlined,
            color: color,
            size: 19,
          ),
          const SizedBox(width: 9),
          const Expanded(
            child: Text(
              'Situación laboral',
              style: TextStyle(fontWeight: FontWeight.w600),
            ),
          ),
          Text(
            status.isEmpty ? 'Sin información' : status,
            style: TextStyle(color: color, fontWeight: FontWeight.w800),
          ),
        ],
      ),
    );
  }
}

class _MetricCard extends StatelessWidget {
  const _MetricCard({
    required this.icon,
    required this.label,
    required this.value,
    required this.color,
  });
  final IconData icon;
  final String label;
  final String value;
  final Color color;
  @override
  Widget build(BuildContext context) => Card(
    margin: EdgeInsets.zero,
    elevation: 0,
    child: Padding(
      padding: const EdgeInsets.all(15),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: color),
          const SizedBox(height: 12),
          Text(label, style: Theme.of(context).textTheme.bodySmall),
          const SizedBox(height: 4),
          Text(
            value,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: Theme.of(
              context,
            ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w800),
          ),
        ],
      ),
    ),
  );
}

class _Heading extends StatelessWidget {
  const _Heading({
    required this.title,
    required this.action,
    required this.onPressed,
  });
  final String title;
  final String action;
  final VoidCallback onPressed;
  @override
  Widget build(BuildContext context) => Row(
    children: [
      Text(
        title,
        style: Theme.of(
          context,
        ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800),
      ),
      const Spacer(),
      TextButton(onPressed: onPressed, child: Text(action)),
    ],
  );
}

class _ScheduleCard extends StatelessWidget {
  const _ScheduleCard({required this.schedule, required this.attendance});
  final Map<String, dynamic>? schedule;
  final Map<String, dynamic>? attendance;
  @override
  Widget build(BuildContext context) {
    final intervals = schedule?['intervals'] as List?;
    Map? interval;
    if (intervals != null) {
      for (final candidate in intervals.whereType<Map>()) {
        final end = DateTime.tryParse(
          candidate['end']?.toString() ?? '',
        )?.toLocal();
        if (end == null || !end.isBefore(DateTime.now())) {
          interval = candidate;
          break;
        }
      }
    }
    final start =
        interval?['start']?.toString() ??
        attendance?['expectedStartAt']?.toString();
    final end =
        interval?['end']?.toString() ??
        attendance?['expectedEndAt']?.toString();
    final status = _label(attendance?['status']);
    final scheduleDate = schedule?['date']?.toString();
    final today = DateFormat('yyyy-MM-dd').format(DateTime.now());
    final title = schedule == null
        ? 'Próximo turno no disponible'
        : scheduleDate == today
        ? 'Horario de hoy'
        : 'Próximo turno';
    return Card(
      margin: EdgeInsets.zero,
      elevation: 0,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Row(
              children: [
                Container(
                  width: 42,
                  height: 42,
                  decoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.primaryContainer,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Icon(
                    Icons.schedule_outlined,
                    color: Theme.of(context).colorScheme.primary,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        title,
                        style: const TextStyle(fontWeight: FontWeight.w700),
                      ),
                      const SizedBox(height: 3),
                      Text(
                        status.isEmpty
                            ? (scheduleDate == null
                                  ? 'No hay turnos programados'
                                  : 'Fecha: $scheduleDate')
                            : 'Asistencia: $status',
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const Divider(height: 24),
            Row(
              children: [
                Expanded(
                  child: _TimeItem(label: 'Entrada', value: _time(start)),
                ),
                Expanded(
                  child: _TimeItem(label: 'Salida', value: _time(end)),
                ),
                Expanded(
                  child: _TimeItem(
                    label: 'Marcación',
                    value: _time(attendance?['actualFirstInAt']?.toString()),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _TimeItem extends StatelessWidget {
  const _TimeItem({required this.label, required this.value});
  final String label;
  final String value;
  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Text(label, style: Theme.of(context).textTheme.bodySmall),
      const SizedBox(height: 4),
      Text(value, style: const TextStyle(fontWeight: FontWeight.w800)),
    ],
  );
}

class _PendingCard extends StatelessWidget {
  const _PendingCard({required this.count, required this.onPressed});
  final int count;
  final VoidCallback onPressed;
  @override
  Widget build(BuildContext context) => Card(
    margin: EdgeInsets.zero,
    elevation: 0,
    child: ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      leading: Icon(
        count == 0
            ? Icons.check_circle_outline
            : Icons.pending_actions_outlined,
        color: count == 0
            ? Colors.green.shade700
            : Theme.of(context).colorScheme.primary,
      ),
      title: Text(
        count == 0
            ? 'No tienes solicitudes pendientes'
            : '$count solicitud${count == 1 ? '' : 'es'} pendiente${count == 1 ? '' : 's'}',
      ),
      subtitle: const Text('Vacaciones, permisos y justificativos'),
      trailing: const Icon(Icons.chevron_right),
      onTap: onPressed,
    ),
  );
}

class _TeamCard extends StatelessWidget {
  const _TeamCard({required this.count, required this.onPressed});
  final int count;
  final VoidCallback onPressed;
  @override
  Widget build(BuildContext context) => Card(
    margin: EdgeInsets.zero,
    elevation: 0,
    child: ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 5),
      leading: Icon(
        Icons.groups_outlined,
        color: Theme.of(context).colorScheme.primary,
      ),
      title: const Text(
        'Mi equipo',
        style: TextStyle(fontWeight: FontWeight.w700),
      ),
      subtitle: Text('$count persona${count == 1 ? '' : 's'} a tu cargo'),
      trailing: const Icon(Icons.chevron_right),
      onTap: onPressed,
    ),
  );
}

class _QuickLinks extends StatelessWidget {
  const _QuickLinks({required this.onOpenSection});
  final ValueChanged<int>? onOpenSection;
  @override
  Widget build(BuildContext context) => Row(
    children: [
      Expanded(
        child: OutlinedButton.icon(
          onPressed: () => onOpenSection?.call(2),
          icon: const Icon(Icons.assignment_outlined),
          label: const Text('Solicitudes'),
        ),
      ),
      const SizedBox(width: 10),
      Expanded(
        child: OutlinedButton.icon(
          onPressed: () => onOpenSection?.call(3),
          icon: const Icon(Icons.folder_open_outlined),
          label: const Text('Archivos'),
        ),
      ),
    ],
  );
}

class _LoadIssues extends StatelessWidget {
  const _LoadIssues({required this.items});
  final List<String> items;
  @override
  Widget build(BuildContext context) => Card(
    color: Theme.of(context).colorScheme.errorContainer,
    elevation: 0,
    margin: EdgeInsets.zero,
    child: Padding(
      padding: const EdgeInsets.all(14),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(
            Icons.info_outline,
            color: Theme.of(context).colorScheme.onErrorContainer,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              items.join('\n'),
              style: TextStyle(
                color: Theme.of(context).colorScheme.onErrorContainer,
              ),
            ),
          ),
        ],
      ),
    ),
  );
}

String _firstNonBlank(Object? first, Object? second, String fallback) {
  for (final value in [first, second]) {
    final text = value?.toString().trim() ?? '';
    if (text.isNotEmpty) return text.split(' ').first;
  }
  return fallback;
}

String _label(Object? value) {
  final text = value?.toString() ?? '';
  return switch (text.toUpperCase()) {
    'ACTIVE' => 'Activa',
    'ON_LEAVE' => 'En ausencia',
    'SUSPENDED' => 'Suspendida',
    'INACTIVE' => 'Inactiva',
    'TERMINATED' => 'Finalizada',
    'PRESENT' => 'Presente',
    'LATE' => 'Atraso',
    'ABSENT' => 'Ausente',
    'INCOMPLETE' => 'Incompleta',
    'JUSTIFIED' => 'Justificada',
    'DAY_OFF' => 'Día libre',
    _ => text.isEmpty ? '' : text,
  };
}

String _attendanceLabel(Map<String, dynamic>? value) {
  if (value == null) return 'Sin registro';
  final status = _label(value['status']);
  return status.isEmpty ? 'Registrada' : status;
}

double _number(Object? value) => double.tryParse(value?.toString() ?? '') ?? 0;
String _time(String? value) {
  if (value == null || value.isEmpty) return '—';
  final parsed = DateTime.tryParse(value);
  return parsed == null
      ? (value.length >= 5 ? value.substring(0, 5) : value)
      : DateFormat.Hm().format(parsed.toLocal());
}

bool _hasTeamAccess(Map<String, dynamic> profile) {
  final permissions = profile['permissions'];
  return permissions is List &&
      permissions.map((e) => e.toString()).contains('employees.team.read');
}
