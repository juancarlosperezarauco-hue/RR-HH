import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter/services.dart';
import 'package:path_provider/path_provider.dart';

import '../../../core/errors/app_exception.dart';
import '../../../core/network/api_exceptions.dart';
import '../../../core/network/dio_client.dart';

class FilesRepository {
  const FilesRepository(this._client);
  final DioClient _client;
  static const _fileChannel = MethodChannel('hrcore_mobile/files');

  Future<FilesData> load() async {
    final results = await Future.wait<_PageResult>([
      _capturePage('/api/me/documents?size=50', 'documentos'),
      _capturePage(
        '/api/me/contracts?size=50&sort=startDate,desc',
        'contratos',
      ),
    ]);
    return FilesData(
      documents: results[0].items,
      contracts: results[1].items,
      issues: [
        for (final result in results)
          if (result.issue != null) result.issue!,
      ],
    );
  }

  Future<String> downloadContract(String id, String filename) async {
    try {
      final response = await _client.dio.get<List<int>>(
        '/api/me/contracts/$id/document',
        options: Options(
          responseType: ResponseType.bytes,
          headers: const {'Accept': 'application/pdf'},
        ),
      );
      final bytes = response.data;
      if (bytes == null || bytes.isEmpty) {
        throw const AppException(
          message: 'El servidor no devolvió el PDF del contrato.',
        );
      }
      final downloads = await getExternalStorageDirectories(
        type: StorageDirectory.downloads,
      );
      final directory = downloads != null && downloads.isNotEmpty
          ? downloads.first
          : await getExternalStorageDirectory() ??
                await getApplicationDocumentsDirectory();
      final requestedName = filename.trim().isEmpty
          ? 'contrato-$id.pdf'
          : filename;
      final safeName = requestedName.replaceAll(
        RegExp(r'[^A-Za-z0-9._-]'),
        '_',
      );
      final file = File('${directory.path}/$safeName');
      try {
        await file.writeAsBytes(bytes, flush: true);
      } on FileSystemException catch (error) {
        throw AppException(
          message: 'No se pudo guardar el archivo: ${error.message}',
        );
      }
      final publicLocation = await _saveToPublicDownloads(file.path, safeName);
      return publicLocation ?? file.path;
    } catch (error) {
      if (error is AppException) rethrow;
      throw mapDioException(error);
    }
  }

  Future<bool> openDownloadedFile(String path) async {
    if (path.trim().isEmpty || !File(path).existsSync()) return false;
    try {
      return await _fileChannel.invokeMethod<bool>(
            'openFile',
            <String, dynamic>{'path': path},
          ) ??
          false;
    } on PlatformException {
      return false;
    } on MissingPluginException {
      // Native channels are registered when the Android activity is rebuilt;
      // a hot reload can temporarily run an older binary without this method.
      return false;
    }
  }

  Future<bool> openDownloadsFolder() async {
    try {
      return await _fileChannel.invokeMethod<bool>('openDownloads') ?? false;
    } on PlatformException {
      return false;
    } on MissingPluginException {
      return false;
    }
  }

  Future<String?> _saveToPublicDownloads(String path, String filename) async {
    try {
      return await _fileChannel.invokeMethod<String>(
        'saveToDownloads',
        <String, dynamic>{'path': path, 'filename': filename},
      );
    } on PlatformException {
      return null;
    } on MissingPluginException {
      return null;
    }
  }

  Future<_PageResult> _capturePage(String path, String label) async {
    try {
      return _PageResult(await _page(path), null);
    } on AppException catch (error) {
      return _PageResult(
        const [],
        'No se pudieron cargar $label. ${error.message}',
      );
    }
  }

  Future<List<Map<String, dynamic>>> _page(String path) async {
    final response = await _client.dio.get<Map<String, dynamic>>(path);
    final body = response.data;
    if (body == null)
      throw const AppException(
        message: 'La respuesta del servidor está vacía.',
      );
    if (body['success'] != true)
      throw AppException(
        message:
            body['message']?.toString() ??
            'No se pudieron cargar los archivos.',
      );
    final data = body['data'];
    final content = data is Map<String, dynamic> ? data['content'] : null;
    return content is List
        ? content.whereType<Map<String, dynamic>>().toList()
        : const [];
  }
}

class FilesData {
  const FilesData({
    required this.documents,
    required this.contracts,
    this.issues = const [],
  });
  final List<Map<String, dynamic>> documents;
  final List<Map<String, dynamic>> contracts;
  final List<String> issues;
}

class _PageResult {
  const _PageResult(this.items, this.issue);
  final List<Map<String, dynamic>> items;
  final String? issue;
}
