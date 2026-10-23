import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:get_it/get_it.dart';

import '../../../../core/errors/app_exception.dart';
import '../../data/files_repository.dart';

class FilesPage extends StatefulWidget {
  const FilesPage({super.key});
  @override
  State<FilesPage> createState() => _FilesPageState();
}

class _FilesPageState extends State<FilesPage> {
  FilesData? _data;
  String? _error;
  bool _loading = true;
  String? _downloading;
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
      final data = await GetIt.I<FilesRepository>().load();
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
          _error = 'No se pudieron cargar tus archivos.';
          _loading = false;
        });
    }
  }

  Future<void> _download(Map<String, dynamic> contract) async {
    final id = contract['id']?.toString();
    if (id == null || id.isEmpty) return;
    setState(() => _downloading = id);
    try {
      final number = contract['contractNumber']?.toString() ?? id;
      final path = await GetIt.I<FilesRepository>().downloadContract(
        id,
        'contrato-$number.pdf',
      );
      if (mounted) {
        await _showSavedPath(path);
      }
    } on AppException catch (error) {
      if (mounted)
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(error.message)));
    } finally {
      if (mounted) setState(() => _downloading = null);
    }
  }

  Future<void> _showSavedPath(String path) async {
    await showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Contrato descargado'),
        content: SelectableText(path),
        actions: [
          TextButton.icon(
            onPressed: () async {
              await Clipboard.setData(ClipboardData(text: path));
              if (dialogContext.mounted) Navigator.pop(dialogContext);
              if (mounted) {
                ScaffoldMessenger.of(
                  context,
                ).showSnackBar(const SnackBar(content: Text('Ruta copiada.')));
              }
            },
            icon: const Icon(Icons.copy_outlined),
            label: const Text('Copiar ruta'),
          ),
          OutlinedButton.icon(
            onPressed: () async {
              final opened = await GetIt.I<FilesRepository>()
                  .openDownloadsFolder();
              if (!mounted) return;
              if (!dialogContext.mounted) return;
              if (opened) {
                Navigator.pop(dialogContext);
                return;
              }
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: Text(
                    'No se pudo abrir el explorador. Usa la ruta copiada.',
                  ),
                ),
              );
            },
            icon: const Icon(Icons.folder_open_outlined),
            label: const Text('Abrir Descargas'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext),
            child: const Text('Cerrar'),
          ),
        ],
      ),
    );
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
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(20, 20, 20, 32),
        children: [
          Text(
            'Archivos',
            style: Theme.of(
              context,
            ).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 6),
          Text(
            'Consulta tus documentos laborales y contratos.',
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
              color: Theme.of(context).colorScheme.onSurfaceVariant,
            ),
          ),
          if (data.issues.isNotEmpty) ...[
            const SizedBox(height: 14),
            _LoadIssues(items: data.issues),
          ],
          const SizedBox(height: 22),
          Text(
            'Contratos',
            style: Theme.of(
              context,
            ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 10),
          _ContractList(
            items: data.contracts,
            downloading: _downloading,
            onDownload: _download,
          ),
          const SizedBox(height: 24),
          Text(
            'Documentos laborales',
            style: Theme.of(
              context,
            ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 10),
          _DocumentList(items: data.documents),
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

class _ContractList extends StatelessWidget {
  const _ContractList({
    required this.items,
    required this.downloading,
    required this.onDownload,
  });
  final List<Map<String, dynamic>> items;
  final String? downloading;
  final ValueChanged<Map<String, dynamic>> onDownload;
  @override
  Widget build(BuildContext context) {
    if (items.isEmpty)
      return const _Empty(message: 'No hay contratos disponibles.');
    return Card(
      margin: EdgeInsets.zero,
      elevation: 0,
      child: Column(
        children: [
          for (final item in items)
            ListTile(
              leading: const Icon(Icons.description_outlined),
              title: Text(
                item['contractNumber']?.toString() ?? 'Contrato laboral',
              ),
              subtitle: Text(
                '${_label(item['contractType']?.toString() ?? '')} · Inicio: ${item['startDate'] ?? '—'} · ${_label(item['status']?.toString() ?? '')}',
              ),
              trailing: downloading == item['id']?.toString()
                  ? const SizedBox(
                      width: 22,
                      height: 22,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : IconButton(
                      tooltip: 'Descargar PDF',
                      onPressed: () => onDownload(item),
                      icon: const Icon(Icons.download_outlined),
                    ),
            ),
        ],
      ),
    );
  }
}

class _DocumentList extends StatelessWidget {
  const _DocumentList({required this.items});
  final List<Map<String, dynamic>> items;
  @override
  Widget build(BuildContext context) {
    if (items.isEmpty)
      return const _Empty(message: 'No hay documentos laborales disponibles.');
    return Card(
      margin: EdgeInsets.zero,
      elevation: 0,
      child: Column(
        children: [
          for (final item in items)
            ListTile(
              leading: const Icon(Icons.insert_drive_file_outlined),
              title: Text(item['title']?.toString() ?? 'Documento laboral'),
              subtitle: Text(
                '${_label(item['documentType']?.toString() ?? '')} · ${item['issuedAt'] ?? 'Fecha no indicada'}${item['expiresAt'] == null ? '' : ' · Vence: ${item['expiresAt']}'}',
              ),
              trailing: _Verification(
                label: item['verificationStatus']?.toString() ?? '',
              ),
            ),
        ],
      ),
    );
  }
}

class _Verification extends StatelessWidget {
  const _Verification({required this.label});
  final String label;
  @override
  Widget build(BuildContext context) => Text(
    _label(label),
    style: TextStyle(
      fontSize: 11,
      color: Theme.of(context).colorScheme.primary,
      fontWeight: FontWeight.w700,
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

String _label(String value) => switch (value.toUpperCase()) {
  'ACTIVE' => 'Activo',
  'SIGNED' => 'Firmado',
  'DRAFT' => 'Borrador',
  'TERMINATED' => 'Terminado',
  'EXPIRED' => 'Vencido',
  'INDEFINITE' => 'Indefinido',
  'FIXED_TERM' => 'Plazo fijo',
  'ID_CARD' => 'Documento de identidad',
  'CERTIFICATE' => 'Certificado',
  'VERIFIED' => 'Verificado',
  'PENDING' => 'Pendiente',
  'REJECTED' => 'Rechazado',
  _ => value.isEmpty ? 'Sin verificar' : value,
};
