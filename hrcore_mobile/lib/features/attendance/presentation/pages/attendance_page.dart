import 'package:flutter/material.dart';
import 'package:get_it/get_it.dart';
import 'package:intl/intl.dart';

import '../../../../core/errors/app_exception.dart';
import '../../data/attendance_repository.dart';
import '../../data/models/attendance_punch_model.dart';

class AttendancePage extends StatefulWidget {
  const AttendancePage({super.key});
  @override
  State<AttendancePage> createState() => _AttendancePageState();
}

class _AttendancePageState extends State<AttendancePage> {
  final _repository = GetIt.I<AttendanceRepository>();
  List<Map<String, dynamic>> _records = const [],
      _punches = const [],
      _adjustments = const [],
      _overtime = const [];
  Map<String, dynamic>? _schedule;
  bool _loading = true, _punching = false;
  String? _error;
  String? _locationStatus;

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
    final now = DateTime.now();
    final todayStart = DateTime(now.year, now.month, now.day);
    final today = _date(todayStart);
    final historyFrom = _date(todayStart.subtract(const Duration(days: 30)));
    final todayTo = now.toUtc().toIso8601String();
    // Include the previous shift window so an overnight IN is considered
    // before allowing another IN/OUT sequence today.
    final todayFrom = todayStart
        .subtract(const Duration(hours: 36))
        .toUtc()
        .toIso8601String();
    try {
      final result = await Future.wait([
        _repository.records(from: historyFrom, to: today),
        _repository.punches(from: todayFrom, to: todayTo),
        _repository.adjustments(),
        _repository.overtime(),
        _repository.scheduleRange(
          from: today,
          to: _date(DateTime.now().add(const Duration(days: 14))),
        ),
      ]);
      if (!mounted) return;
      setState(() {
        _records = result[0] as List<Map<String, dynamic>>;
        _punches = result[1] as List<Map<String, dynamic>>;
        _adjustments = result[2] as List<Map<String, dynamic>>;
        _overtime = result[3] as List<Map<String, dynamic>>;
        _schedule = _nextSchedule(result[4] as Map<String, dynamic>?, now: now);
        final queryErrors = <String>[];
        for (final key in ['records', 'punches', 'adjustments', 'overtime']) {
          final message = _repository.takeQueryError(key);
          if (message != null && message.isNotEmpty) queryErrors.add(message);
        }
        _error = queryErrors.isEmpty ? null : queryErrors.join(' ');
        _loading = false;
      });
    } catch (error) {
      if (mounted) {
        setState(() {
          _loading = false;
          _error = error is AppException
              ? error.message
              : 'No se pudo cargar tu jornada.';
        });
      }
    }
  }

  Future<void> _registerPunch(String type) async {
    if (_punching) return;
    setState(() {
      _punching = true;
      _error = null;
    });
    try {
      final punch = await _repository.punch(punchType: type);
      final locationWarning = _repository.takeLocationWarning();
      if (locationWarning != null && mounted) {
        setState(() => _locationStatus = locationWarning);
      }
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(_successMessage(punch))));
      if (locationWarning != null && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(locationWarning),
            action: SnackBarAction(
              label: 'Abrir ajustes',
              onPressed: () {
                _repository.openLocationSettings();
              },
            ),
          ),
        );
      }
      await _load();
    } on AppException catch (error) {
      final locationWarning = _repository.takeLocationWarning();
      if (mounted) {
        setState(() => _error = error.message);
        if (locationWarning != null) _locationStatus = locationWarning;
        if (locationWarning != null) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(locationWarning),
              action: SnackBarAction(
                label: 'Abrir ajustes',
                onPressed: () {
                  _repository.openLocationSettings();
                },
              ),
            ),
          );
        }
      }
    } finally {
      if (mounted) setState(() => _punching = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    final theme = Theme.of(context);
    final today = _date(DateTime.now());
    final record = _records.cast<Map<String, dynamic>?>().firstWhere(
      (value) => value?['workDate']?.toString() == today,
      orElse: () => null,
    );
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(20, 20, 20, 32),
        children: [
          Text(
            'Tu jornada',
            style: theme.textTheme.headlineSmall?.copyWith(
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            'Registra tu jornada y consulta tus movimientos laborales.',
            style: theme.textTheme.bodyMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 20),
          _PunchCard(
            record: record,
            punches: _punches,
            loading: _punching,
            onPunch: _registerPunch,
          ),
          if (_locationStatus != null) ...[
            const SizedBox(height: 10),
            _LocationStatus(message: _locationStatus!),
          ],
          if (_error != null) ...[
            const SizedBox(height: 12),
            _ErrorBox(message: _error!),
          ],
          const SizedBox(height: 22),
          _SectionTitle(title: 'Próximo turno'),
          const SizedBox(height: 10),
          _ScheduleCard(schedule: _schedule),
          const SizedBox(height: 22),
          _SectionTitle(title: 'Historial reciente'),
          const SizedBox(height: 10),
          _HistoryCard(records: _records, punches: _punches),
          const SizedBox(height: 22),
          _SectionTitle(title: 'Solicitudes de jornada'),
          const SizedBox(height: 10),
          _RequestSummary(adjustments: _adjustments, overtime: _overtime),
        ],
      ),
    );
  }

  String _successMessage(AttendancePunchModel punch) {
    final label = switch (punch.punchType.toUpperCase()) {
      'IN' => 'Entrada',
      'OUT' => 'Salida',
      'BREAK_START' => 'Inicio de pausa',
      'BREAK_END' => 'Fin de pausa',
      _ => 'Marcación',
    };
    return '$label registrada a las ${DateFormat.Hm().format(punch.punchedAt.toLocal())}';
  }
}

class _PunchCard extends StatelessWidget {
  const _PunchCard({
    required this.record,
    required this.punches,
    required this.loading,
    required this.onPunch,
  });
  final Map<String, dynamic>? record;
  final List<Map<String, dynamic>> punches;
  final bool loading;
  final ValueChanged<String> onPunch;
  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final status = _label(record?['status']);
    final first = record?['actualFirstInAt']?.toString();
    final last = record?['actualLastOutAt']?.toString();
    final latestPunch = punches.isEmpty
        ? null
        : punches.first['punchType']?.toString().toUpperCase();
    final latestPunchAt = punches.isEmpty
        ? null
        : DateTime.tryParse(punches.first['punchedAt']?.toString() ?? '')?.toLocal();
    final today = _date(DateTime.now());
    final todayPunchCount = punches.where((punch) {
      final at = DateTime.tryParse(
        punch['punchedAt']?.toString() ?? '',
      )?.toLocal();
      return at != null && _date(at) == today;
    }).length;
    final canCheckIn = latestPunch == null || latestPunch == 'OUT';
    final canCheckOut = latestPunch == 'IN' || latestPunch == 'BREAK_END';
    final canStartBreak = latestPunch == 'IN';
    final canEndBreak = latestPunch == 'BREAK_START';
    return Card(
      margin: EdgeInsets.zero,
      elevation: 0,
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(
                  Icons.access_time_filled,
                  color: theme.colorScheme.primary,
                ),
                const SizedBox(width: 10),
                Text(
                  'Marcación de hoy',
                  style: theme.textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w700,
                  ),
                ),
                const Spacer(),
                _StatusChip(label: status.isEmpty ? 'Sin registro' : status),
              ],
            ),
            const SizedBox(height: 18),
            Row(
              children: [
                Expanded(
                  child: _TimeCell(label: 'Entrada', value: _time(first)),
                ),
                Expanded(
                  child: _TimeCell(label: 'Salida', value: _time(last)),
                ),
                Expanded(
                  child: _TimeCell(
                    label: 'Marcaciones',
                    value: '$todayPunchCount',
                  ),
                ),
              ],
            ),
            const SizedBox(height: 20),
            if (canCheckIn)
              SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: loading ? null : () => onPunch('IN'),
                  icon: const Icon(Icons.login),
                  label: const Text('Registrar entrada'),
                ),
              ),
            if (canCheckOut || canStartBreak || canEndBreak) ...[
              if (canCheckIn) const SizedBox(height: 10),
              Row(
                children: [
                  if (canStartBreak)
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: loading ? null : () => onPunch('BREAK_START'),
                        icon: const Icon(Icons.free_breakfast_outlined),
                        label: const Text('Iniciar pausa'),
                      ),
                    ),
                  if (canEndBreak)
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: loading ? null : () => onPunch('BREAK_END'),
                        icon: const Icon(Icons.play_arrow_outlined),
                        label: const Text('Terminar pausa'),
                      ),
                    ),
                  if (canStartBreak || canEndBreak) const SizedBox(width: 10),
                  if (canCheckOut)
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: loading ? null : () => onPunch('OUT'),
                        icon: const Icon(Icons.logout),
                        label: const Text('Registrar salida'),
                      ),
                    ),
                ],
              ),
            ],
            if (latestPunch != null) ...[
              const SizedBox(height: 8),
              Text(
                latestPunch == 'IN'
                    ? 'Tienes una entrada abierta${latestPunchAt == null ? '' : ' desde las ${_time(latestPunchAt.toIso8601String())}'}. Puedes iniciar una pausa o registrar tu salida.'
                    : latestPunch == 'BREAK_START'
                    ? 'Tienes una pausa abierta. Debes terminarla antes de registrar la salida.'
                    : latestPunch == 'BREAK_END'
                    ? 'La pausa terminó. Puedes registrar tu salida.'
                    : 'La última marcación fue una salida.',
                style: theme.textTheme.bodySmall?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ],
            if (loading) ...[
              const SizedBox(height: 14),
              const LinearProgressIndicator(),
            ],
          ],
        ),
      ),
    );
  }
}

class _ScheduleCard extends StatelessWidget {
  const _ScheduleCard({required this.schedule});
  final Map<String, dynamic>? schedule;
  @override
  Widget build(BuildContext context) {
    final intervals = schedule?['intervals'] as List?;
    final interval =
        intervals != null && intervals.isNotEmpty && intervals.first is Map
        ? intervals.first as Map
        : null;
    return Card(
      margin: EdgeInsets.zero,
      elevation: 0,
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
        leading: const Icon(Icons.calendar_month_outlined),
        title: Text(
          schedule == null
              ? 'No hay un turno disponible'
              : 'Turno del ${schedule?['date'] ?? 'próximo día'}',
        ),
        subtitle: Text(
          schedule == null
              ? 'El horario aún no está configurado.'
              : '${_time(interval?['start']?.toString())} – ${_time(interval?['end']?.toString())}',
        ),
        trailing: const Icon(Icons.chevron_right),
      ),
    );
  }
}

class _HistoryCard extends StatelessWidget {
  const _HistoryCard({required this.records, required this.punches});
  final List<Map<String, dynamic>> records;
  final List<Map<String, dynamic>> punches;
  @override
  Widget build(BuildContext context) {
    if (records.isEmpty && punches.isEmpty) {
      return const _EmptyCard(message: 'Todavía no hay movimientos recientes.');
    }
    return Card(
      margin: EdgeInsets.zero,
      elevation: 0,
      child: Column(
        children: [
          for (final record in records.take(5))
            ListTile(
              leading: const Icon(Icons.event_note_outlined),
              title: Text(_dateLabel(record['workDate'])),
              subtitle: Text(
                'Estado: ${_label(record['status'])} · Trabajado: ${record['workedMinutes'] ?? 0} min',
              ),
              trailing: Text('${record['lateMinutes'] ?? 0} min tarde'),
            ),
          if (records.isEmpty)
            for (final punch in punches.take(5))
              ListTile(
                leading: Icon(
                  punch['punchType'] == 'IN' ? Icons.login : Icons.logout,
                ),
                title: Text(punch['punchType'] == 'IN' ? 'Entrada' : 'Salida'),
                subtitle: Text(_dateTime(punch['punchedAt'])),
              ),
        ],
      ),
    );
  }
}

class _RequestSummary extends StatelessWidget {
  const _RequestSummary({required this.adjustments, required this.overtime});
  final List<Map<String, dynamic>> adjustments;
  final List<Map<String, dynamic>> overtime;
  @override
  Widget build(BuildContext context) => Card(
    margin: EdgeInsets.zero,
    elevation: 0,
    child: Column(
      children: [
        _RequestTile(
          icon: Icons.edit_calendar_outlined,
          title: 'Ajustes de marcación',
          count: adjustments.length,
        ),
        const Divider(height: 1),
        _RequestTile(
          icon: Icons.more_time_outlined,
          title: 'Horas extra',
          count: overtime.length,
        ),
      ],
    ),
  );
}

class _RequestTile extends StatelessWidget {
  const _RequestTile({
    required this.icon,
    required this.title,
    required this.count,
  });
  final IconData icon;
  final String title;
  final int count;
  @override
  Widget build(BuildContext context) => ListTile(
    leading: Icon(icon),
    title: Text(title),
    trailing: Text(
      '$count',
      style: const TextStyle(fontWeight: FontWeight.w800),
    ),
  );
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle({required this.title});
  final String title;
  @override
  Widget build(BuildContext context) => Text(
    title,
    style: Theme.of(
      context,
    ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800),
  );
}

class _TimeCell extends StatelessWidget {
  const _TimeCell({required this.label, required this.value});
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

class _StatusChip extends StatelessWidget {
  const _StatusChip({required this.label});
  final String label;
  @override
  Widget build(BuildContext context) => DecoratedBox(
    decoration: BoxDecoration(
      color: Theme.of(context).colorScheme.primaryContainer,
      borderRadius: BorderRadius.circular(20),
    ),
    child: Padding(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      child: Text(
        label,
        style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w700),
      ),
    ),
  );
}

class _EmptyCard extends StatelessWidget {
  const _EmptyCard({required this.message});
  final String message;
  @override
  Widget build(BuildContext context) => Card(
    margin: EdgeInsets.zero,
    elevation: 0,
    child: Padding(padding: const EdgeInsets.all(18), child: Text(message)),
  );
}

class _ErrorBox extends StatelessWidget {
  const _ErrorBox({required this.message});
  final String message;
  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.all(14),
    decoration: BoxDecoration(
      color: Theme.of(context).colorScheme.errorContainer,
      borderRadius: BorderRadius.circular(12),
    ),
    child: Text(
      message,
      style: TextStyle(color: Theme.of(context).colorScheme.onErrorContainer),
    ),
  );
}

class _LocationStatus extends StatelessWidget {
  const _LocationStatus({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.all(14),
    decoration: BoxDecoration(
      color: Theme.of(context).colorScheme.tertiaryContainer,
      borderRadius: BorderRadius.circular(12),
    ),
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(
          Icons.location_on_outlined,
          size: 20,
          color: Theme.of(context).colorScheme.onTertiaryContainer,
        ),
        const SizedBox(width: 10),
        Expanded(
          child: Text(
            message,
            style: TextStyle(
              color: Theme.of(context).colorScheme.onTertiaryContainer,
            ),
          ),
        ),
      ],
    ),
  );
}

Map<String, dynamic>? _nextSchedule(
  Map<String, dynamic>? range, {
  required DateTime now,
}) {
  final days = range?['days'];
  if (days is! List) return null;
  for (final day in days.whereType<Map<String, dynamic>>()) {
    final intervals = day['intervals'];
    if (intervals is! List || intervals.isEmpty) continue;
    final interval = intervals.first;
    final end = interval is Map
        ? DateTime.tryParse(interval['end']?.toString() ?? '')?.toLocal()
        : null;
    if (end != null && end.isBefore(now)) continue;
    return day;
  }
  return null;
}

String _date(DateTime value) =>
    '${value.year.toString().padLeft(4, '0')}-${value.month.toString().padLeft(2, '0')}-${value.day.toString().padLeft(2, '0')}';
String _time(String? value) {
  if (value == null || value.isEmpty) return '—';
  final parsed = DateTime.tryParse(value);
  return parsed == null
      ? (value.length >= 5 ? value.substring(0, 5) : value)
      : DateFormat.Hm().format(parsed.toLocal());
}

String _dateTime(Object? value) {
  final parsed = DateTime.tryParse(value?.toString() ?? '');
  return parsed == null
      ? '—'
      : DateFormat('dd/MM HH:mm').format(parsed.toLocal());
}

String _dateLabel(Object? value) {
  final parsed = DateTime.tryParse(value?.toString() ?? '');
  return parsed == null
      ? value?.toString() ?? 'Fecha desconocida'
      : DateFormat('dd/MM/yyyy').format(parsed);
}

String _label(Object? value) {
  final text = value?.toString() ?? '';
  return switch (text.toUpperCase()) {
    'PRESENT' => 'Presente',
    'LATE' => 'Atraso',
    'ABSENT' => 'Ausente',
    'INCOMPLETE' => 'Incompleta',
    'JUSTIFIED' => 'Justificada',
    'DAY_OFF' => 'Día libre',
    'PENDING' => 'Pendiente',
    'APPROVED' => 'Aprobada',
    'REJECTED' => 'Rechazada',
    _ => text.isEmpty ? '' : text,
  };
}
