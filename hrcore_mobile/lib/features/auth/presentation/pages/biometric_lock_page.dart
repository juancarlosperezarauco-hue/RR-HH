import 'package:flutter/material.dart';
import 'package:get_it/get_it.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/route_names.dart';
import '../../../../core/auth/biometric_auth_service.dart';
import '../../../../core/storage/secure_storage_service.dart';

class BiometricLockPage extends StatefulWidget {
  const BiometricLockPage({super.key});

  @override
  State<BiometricLockPage> createState() => _BiometricLockPageState();
}

class _BiometricLockPageState extends State<BiometricLockPage> {
  bool _isAuthenticating = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _unlock());
  }

  Future<void> _unlock() async {
    if (_isAuthenticating) return;
    setState(() {
      _isAuthenticating = true;
      _errorMessage = null;
    });
    final authenticated = await GetIt.I<BiometricAuthService>().authenticate();
    if (!mounted) return;
    if (authenticated) {
      context.go(RouteNames.homePath);
    } else {
      final detail = GetIt.I<BiometricAuthService>().lastError;
      setState(() {
        _isAuthenticating = false;
        _errorMessage = detail == null
            ? 'No se pudo validar la biometría. Verifica que tengas una huella registrada en Ajustes e inténtalo nuevamente.'
            : 'No se pudo validar la biometría: $detail';
      });
    }
  }

  Future<void> _useAnotherAccount() async {
    await GetIt.I<SecureStorageService>().deleteToken();
    await GetIt.I<BiometricAuthService>().setEnabled(false);
    if (mounted) context.go(RouteNames.loginPath);
  }

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Scaffold(
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(28),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.fingerprint, size: 72, color: colors.primary),
              const SizedBox(height: 20),
              Text('Desbloquea HRCore', style: Theme.of(context).textTheme.headlineSmall),
              const SizedBox(height: 8),
              const Text('Usa la huella o Face ID configurado en este dispositivo.', textAlign: TextAlign.center),
              if (_errorMessage != null) ...[
                const SizedBox(height: 16),
                Text(_errorMessage!, textAlign: TextAlign.center, style: TextStyle(color: colors.error)),
              ],
              const SizedBox(height: 24),
              FilledButton.icon(
                onPressed: _isAuthenticating ? null : _unlock,
                icon: const Icon(Icons.lock_open_outlined),
                label: Text(_isAuthenticating ? 'Validando...' : 'Validar identidad'),
              ),
              TextButton(onPressed: _useAnotherAccount, child: const Text('Usar otra cuenta')),
            ],
          ),
        ),
      ),
    );
  }
}
