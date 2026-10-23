import 'package:flutter/material.dart';
import 'package:get_it/get_it.dart';

import '../../../../core/errors/app_exception.dart';
import '../../data/requests_repository.dart';

class RequestsPage extends StatefulWidget {
  const RequestsPage({super.key});
  @override
  State<RequestsPage> createState() => _RequestsPageState();
}

class _RequestsPageState extends State<RequestsPage> {
  RequestsData? _data;
  String? _error;
  bool _loading = true;
  bool _creatingRequest = false;
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
      final data = await GetIt.I<RequestsRepository>().load();
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
          _error = 'No se pudieron cargar tus solicitudes.';
          _loading = false;
        });
    }
  }

  Future<void> _newRequest() async {
    if (_creatingRequest) return;
    setState(() => _creatingRequest = true);
    try {
      final types = _data?.leaveTypes ?? const <Map<String, dynamic>>[];
      if (types.isEmpty) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('No hay tipos de permiso activos disponibles.'),
            ),
          );
        }
        return;
      }
      if (!mounted) return;
      final result = await showDialog<_RequestDraft>(
        context: context,
        builder: (_) => _RequestForm(types: types),
      );
      if (result == null) return;
      await GetIt.I<RequestsRepository>().createAndSubmit(
        leaveTypeId: result.leaveTypeId,
        startDate: result.startDate,
        endDate: result.endDate,
        reason: result.reason,
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Solicitud enviada para revisión.')),
        );
        await _load();
      }
    } on AppException catch (error) {
      if (mounted)
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(error.message)));
    } finally {
      if (mounted) setState(() => _creatingRequest = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_data == null)
      return RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          children: [
            const SizedBox(height: 180),
            Center(child: Text(_error ?? 'No hay información disponible.')),
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
    final typeNames = <String, String>{
      for (final type in data.leaveTypes)
        if (type['id'] != null)
          type['id'].toString():
              type['name']?.toString() ?? type['code']?.toString() ?? 'Permiso',
    };
    final balancesWithDays = data.balances
        .where((item) => _number(item['availableDays']) > 0)
        .length;
    final pending = data.requests
        .where((item) => _pending(item['status']))
        .length;
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(20, 20, 20, 32),
        children: [
          Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Solicitudes',
                      style: Theme.of(context).textTheme.headlineSmall
                          ?.copyWith(fontWeight: FontWeight.w800),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      'Consulta tus saldos y el estado de cada solicitud.',
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        color: Theme.of(context).colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              IconButton.filled(
                tooltip: 'Nueva solicitud',
                onPressed: _creatingRequest ? null : _newRequest,
                icon: const Icon(Icons.add),
              ),
            ],
          ),
          const SizedBox(height: 20),
          Row(
            children: [
              Expanded(
                child: _Metric(
                  icon: Icons.beach_access_outlined,
                  label: 'Tipos con saldo',
                  value: '$balancesWithDays',
                  color: Colors.teal.shade700,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _Metric(
                  icon: Icons.pending_actions_outlined,
                  label: 'Pendientes',
                  value: '$pending',
                  color: Theme.of(context).colorScheme.primary,
                ),
              ),
            ],
          ),
          const SizedBox(height: 24),
          if (data.issues.isNotEmpty) ...[
            _LoadIssues(items: data.issues),
            const SizedBox(height: 20),
          ],
          Text(
            'Saldos de vacaciones',
            style: Theme.of(
              context,
            ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 10),
          _BalanceList(items: data.balances, typeNames: typeNames),
          const SizedBox(height: 24),
          Text(
            'Historial de solicitudes',
            style: Theme.of(
              context,
            ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 10),
          _RequestList(items: data.requests, typeNames: typeNames),
          const SizedBox(height: 24),
          Text(
            'Incidencias laborales',
            style: Theme.of(
              context,
            ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 10),
          _IncidentList(items: data.incidents),
          if (_error != null) ...[
            const SizedBox(height: 14),
            Text(
              _error!,
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
          ],
        ],
      ),
    );
  }
}

class _Metric extends StatelessWidget {
  const _Metric({
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
            style: Theme.of(
              context,
            ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800),
          ),
        ],
      ),
    ),
  );
}

class _LoadIssues extends StatelessWidget {
  const _LoadIssues({required this.items});
  final List<String> items;

  @override
  Widget build(BuildContext context) => Card(
    margin: EdgeInsets.zero,
    elevation: 0,
    color: Theme.of(context).colorScheme.errorContainer,
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

class _BalanceList extends StatelessWidget {
  const _BalanceList({required this.items, required this.typeNames});
  final List<Map<String, dynamic>> items;
  final Map<String, String> typeNames;
  @override
  Widget build(BuildContext context) {
    if (items.isEmpty)
      return const _Empty(message: 'No hay saldos de vacaciones disponibles.');
    return Card(
      margin: EdgeInsets.zero,
      elevation: 0,
      child: Column(
        children: [
          for (final item in items.take(8))
            ListTile(
              leading: const Icon(Icons.event_available_outlined),
              title: Text(
                _leaveTypeName(item, typeNames, fallback: 'Vacaciones'),
              ),
              subtitle: Text(
                'Año ${item['balanceYear'] ?? ''} · Usados: ${item['usedDays'] ?? 0} días',
              ),
              trailing: Text(
                '${item['availableDays'] ?? 0} días',
                style: const TextStyle(fontWeight: FontWeight.w800),
              ),
            ),
        ],
      ),
    );
  }
}

class _RequestList extends StatelessWidget {
  const _RequestList({required this.items, required this.typeNames});
  final List<Map<String, dynamic>> items;
  final Map<String, String> typeNames;
  @override
  Widget build(BuildContext context) {
    if (items.isEmpty)
      return const _Empty(
        message: 'Todavía no tienes solicitudes registradas.',
      );
    return Card(
      margin: EdgeInsets.zero,
      elevation: 0,
      child: Column(
        children: [
          for (final item in items.take(12))
            ListTile(
              leading: Icon(_statusIcon(item['status'])),
              title: Text(_leaveTypeName(item, typeNames)),
              subtitle: Text(
                '${item['startDate'] ?? '—'} al ${item['endDate'] ?? '—'} · ${item['requestedDays'] ?? 0} días',
              ),
              trailing: _StatusChip(label: _label(item['status'])),
            ),
        ],
      ),
    );
  }
}

class _IncidentList extends StatelessWidget {
  const _IncidentList({required this.items});
  final List<Map<String, dynamic>> items;
  @override
  Widget build(BuildContext context) {
    if (items.isEmpty)
      return const _Empty(
        message: 'No hay incidencias personales disponibles.',
      );
    return Card(
      margin: EdgeInsets.zero,
      elevation: 0,
      child: Column(
        children: [
          for (final item in items.take(8))
            ListTile(
              leading: const Icon(Icons.report_problem_outlined),
              title: Text(item['title']?.toString() ?? 'Incidencia laboral'),
              subtitle: Text(
                '${item['incidentType'] ?? '—'} · ${item['occurredAt']?.toString().split('T').first ?? '—'}',
              ),
              trailing: _StatusChip(label: _label(item['status'])),
            ),
        ],
      ),
    );
  }
}

class _StatusChip extends StatelessWidget {
  const _StatusChip({required this.label});
  final String label;
  @override
  Widget build(BuildContext context) => DecoratedBox(
    decoration: BoxDecoration(
      color: Theme.of(context).colorScheme.primaryContainer,
      borderRadius: BorderRadius.circular(18),
    ),
    child: Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 5),
      child: Text(
        label,
        style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w700),
      ),
    ),
  );
}

class _Empty extends StatelessWidget {
  const _Empty({required this.message});
  final String message;
  @override
  Widget build(BuildContext context) => Card(
    margin: EdgeInsets.zero,
    elevation: 0,
    child: Padding(padding: const EdgeInsets.all(16), child: Text(message)),
  );
}

class _RequestDraft {
  const _RequestDraft({
    required this.leaveTypeId,
    required this.startDate,
    required this.endDate,
    required this.reason,
  });
  final String leaveTypeId;
  final DateTime startDate;
  final DateTime endDate;
  final String reason;
}

class _RequestForm extends StatefulWidget {
  const _RequestForm({required this.types});
  final List<Map<String, dynamic>> types;
  @override
  State<_RequestForm> createState() => _RequestFormState();
}

class _RequestFormState extends State<_RequestForm> {
  final _formKey = GlobalKey<FormState>();
  String? _type;
  DateTime? _start;
  DateTime? _end;
  final _reason = TextEditingController();
  @override
  void dispose() {
    _reason.dispose();
    super.dispose();
  }

  Future<void> _pick(bool start) async {
    final today = DateTime.now();
    final minimumDate = start ? today : (_start ?? today);
    final selected = await showDatePicker(
      context: context,
      firstDate: minimumDate,
      lastDate: today.add(const Duration(days: 730)),
      initialDate: start ? (_start ?? today) : (_end ?? _start ?? today),
    );
    if (selected != null)
      setState(() {
        if (start) {
          _start = selected;
          if (_end != null && _end!.isBefore(selected)) _end = selected;
        } else {
          _end = selected;
        }
      });
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
    title: const Text('Nueva solicitud'),
    content: Form(
      key: _formKey,
      autovalidateMode: AutovalidateMode.onUserInteraction,
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            DropdownButtonFormField<String>(
              initialValue: _type,
              decoration: const InputDecoration(labelText: 'Tipo de permiso'),
              items: [
                for (final type in widget.types)
                  DropdownMenuItem(
                    value: type['id']?.toString(),
                    child: Text(
                      type['name']?.toString() ??
                          type['code']?.toString() ??
                          'Permiso',
                    ),
                  ),
              ],
              onChanged: (value) => setState(() => _type = value),
              validator: (value) =>
                  value == null ? 'Selecciona un tipo.' : null,
            ),
            const SizedBox(height: 8),
            ListTile(
              contentPadding: EdgeInsets.zero,
              title: Text(
                _start == null
                    ? 'Fecha de inicio'
                    : 'Inicio: ${_dateLabel(_start)}',
              ),
              trailing: const Icon(Icons.calendar_today_outlined),
              onTap: () => _pick(true),
            ),
            ListTile(
              contentPadding: EdgeInsets.zero,
              title: Text(
                _end == null ? 'Fecha de fin' : 'Fin: ${_dateLabel(_end)}',
              ),
              trailing: const Icon(Icons.calendar_today_outlined),
              onTap: () => _pick(false),
            ),
            TextFormField(
              controller: _reason,
              maxLines: 3,
              decoration: const InputDecoration(
                labelText: 'Motivo',
                alignLabelWithHint: true,
              ),
              validator: (value) => value == null || value.trim().isEmpty
                  ? 'Indica el motivo.'
                  : null,
            ),
          ],
        ),
      ),
    ),
    actions: [
      TextButton(
        onPressed: () => Navigator.pop(context),
        child: const Text('Cancelar'),
      ),
      FilledButton(onPressed: _submit, child: const Text('Enviar')),
    ],
  );
  void _submit() {
    final valid = _formKey.currentState?.validate() ?? false;
    if (!valid || _start == null || _end == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Completa tipo, fechas y motivo.')),
      );
      return;
    }
    if (_end!.isBefore(_start!)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('La fecha de fin no puede ser anterior al inicio.'),
        ),
      );
      return;
    }
    Navigator.pop(
      context,
      _RequestDraft(
        leaveTypeId: _type!,
        startDate: _start!,
        endDate: _end!,
        reason: _reason.text,
      ),
    );
  }
}

String _leaveTypeName(
  Map<String, dynamic> item,
  Map<String, String> typeNames, {
  String fallback = 'Solicitud de permiso',
}) =>
    item['leaveTypeName']?.toString() ??
    item['leaveTypeCode']?.toString() ??
    typeNames[item['leaveTypeId']?.toString()] ??
    fallback;
bool _pending(Object? value) => value?.toString().toUpperCase() == 'PENDING';
String _label(Object? value) => switch (value?.toString().toUpperCase()) {
  'DRAFT' => 'Borrador',
  'SUBMITTED' => 'Enviada',
  'PENDING' => 'Pendiente',
  'APPROVED' => 'Aprobada',
  'REJECTED' => 'Rechazada',
  'CANCELLED' => 'Cancelada',
  'OPEN' => 'Abierta',
  'UNDER_REVIEW' => 'En revisión',
  'RESOLVED' => 'Resuelta',
  'CLOSED' => 'Cerrada',
  _ => value?.toString() ?? 'Sin estado',
};
IconData _statusIcon(Object? value) => _pending(value)
    ? Icons.pending_actions_outlined
    : value?.toString().toUpperCase() == 'APPROVED'
    ? Icons.check_circle_outline
    : Icons.assignment_outlined;
double _number(Object? value) => double.tryParse(value?.toString() ?? '') ?? 0;
String _dateLabel(DateTime? value) => value == null
    ? '—'
    : '${value.day.toString().padLeft(2, '0')}/${value.month.toString().padLeft(2, '0')}/${value.year}';
