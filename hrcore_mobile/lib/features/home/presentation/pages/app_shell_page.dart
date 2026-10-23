import 'package:flutter/material.dart';
import 'package:get_it/get_it.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/route_names.dart';
import '../../../profile/presentation/pages/profile_page.dart';
import '../../../attendance/presentation/pages/attendance_page.dart';
import '../../../notifications/data/notification_repository.dart';
import '../../../requests/presentation/pages/requests_page.dart';
import '../../../files/presentation/pages/files_page.dart';
import '../../../auth/data/auth_repository.dart';
import '../../../profile/data/profile_repository.dart';
import 'home_page.dart';

class AppShellPage extends StatefulWidget {
  const AppShellPage({super.key});

  @override
  State<AppShellPage> createState() => _AppShellPageState();
}

class _AppShellPageState extends State<AppShellPage> {
  int _selectedIndex = 0;
  int _unreadNotifications = 0;
  ProfileData? _profile;

  static const _sections = [
    _AppSection('Inicio', Icons.home_outlined),
    _AppSection('Jornada', Icons.access_time_outlined),
    _AppSection('Permisos', Icons.assignment_outlined),
    _AppSection('Archivos', Icons.folder_open_outlined),
    _AppSection('Perfil', Icons.person_outline),
  ];

  @override
  void initState() {
    super.initState();
    _loadUnreadNotifications();
    _loadProfile();
  }

  Future<void> _loadProfile() async {
    try {
      final profile = await GetIt.I<ProfileRepository>().load();
      if (mounted) setState(() => _profile = profile);
    } catch (_) {
      // The account menu remains usable even if profile details are unavailable.
    }
  }

  Future<void> _loadUnreadNotifications() async {
    try {
      final count = await GetIt.I<NotificationRepository>().unreadCount();
      if (mounted) setState(() => _unreadNotifications = count);
    } catch (_) {
      // Notifications must not block the rest of the application.
    }
  }

  @override
  Widget build(BuildContext context) {
    final section = _sections[_selectedIndex];
    return Scaffold(
      appBar: AppBar(
        automaticallyImplyLeading: false,
        leadingWidth: 48,
        leading: Padding(
          padding: const EdgeInsets.only(left: 16),
          child: Image.asset(
            'assets/logo.png',
            fit: BoxFit.contain,
            semanticLabel: 'Logo de HRCore',
          ),
        ),
        titleSpacing: 8,
        title: Text(section.label),
        actions: [
          Stack(
            children: [
              IconButton(
                tooltip: 'Notificaciones',
                onPressed: () => context.push(RouteNames.notificationsPath),
                icon: const Icon(Icons.notifications_none_outlined),
              ),
              if (_unreadNotifications > 0)
                Positioned(
                  top: 10,
                  right: 10,
                  child: Container(
                    width: 7,
                    height: 7,
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.secondary,
                      shape: BoxShape.circle,
                    ),
                  ),
                ),
            ],
          ),
          Padding(
            padding: const EdgeInsets.only(left: 4, right: 12),
            child: PopupMenuButton<String>(
              tooltip: 'Cuenta de usuario',
              onSelected: _onAccountAction,
              itemBuilder: (context) => [
                PopupMenuItem<String>(
                  enabled: false,
                  child: _AccountHeader(profile: _profile),
                ),
                const PopupMenuDivider(),
                const PopupMenuItem<String>(
                  value: 'profile',
                  child: ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: Icon(Icons.person_outline),
                    title: Text('Perfil'),
                  ),
                ),
                const PopupMenuItem<String>(
                  value: 'logout',
                  child: ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: Icon(Icons.logout),
                    title: Text('Cerrar sesión'),
                  ),
                ),
              ],
              child: CircleAvatar(
                radius: 16,
                backgroundColor: Theme.of(context).colorScheme.primaryContainer,
                child: Icon(
                  Icons.person_outline,
                  size: 19,
                  color: Theme.of(context).colorScheme.primary,
                ),
              ),
            ),
          ),
        ],
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(2),
          child: Container(
            height: 2,
            color: Theme.of(context).colorScheme.secondary,
          ),
        ),
      ),
      body: IndexedStack(
        index: _selectedIndex,
        children: [
          HomePage(
            onOpenSection: (index) => setState(() => _selectedIndex = index),
            onOpenTeam: _openTeam,
          ),
          AttendancePage(),
          const RequestsPage(),
          const FilesPage(),
          ProfilePage(),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selectedIndex,
        onDestinationSelected: (index) =>
            setState(() => _selectedIndex = index),
        destinations: [
          for (final item in _sections)
            NavigationDestination(icon: Icon(item.icon), label: item.label),
        ],
      ),
    );
  }

  void _onAccountAction(String action) {
    if (action == 'profile') {
      setState(() => _selectedIndex = 4);
      return;
    }
    if (action == 'logout') _logout();
  }

  Future<void> _logout() async {
    await GetIt.I<AuthRepository>().logout();
    if (mounted) context.go(RouteNames.loginPath);
  }

  void _openTeam(List<Map<String, dynamic>> team) {
    showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Mi equipo'),
        content: SizedBox(
          width: double.maxFinite,
          child: team.isEmpty
              ? const Text('No hay personas asignadas a tu equipo.')
              : ListView.separated(
                  shrinkWrap: true,
                  itemCount: team.length,
                  separatorBuilder: (_, _) => const Divider(height: 1),
                  itemBuilder: (_, index) {
                    final person = team[index];
                    final name =
                        person['fullName']?.toString() ??
                        person['name']?.toString() ??
                        'Colaborador';
                    final status =
                        person['employmentStatus']?.toString() ??
                        person['status']?.toString() ??
                        '';
                    return ListTile(
                      contentPadding: EdgeInsets.zero,
                      title: Text(name),
                      subtitle: status.isEmpty ? null : Text(status),
                    );
                  },
                ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cerrar'),
          ),
        ],
      ),
    );
  }
}

class _AppSection {
  const _AppSection(this.label, this.icon);

  final String label;
  final IconData icon;
}

class _AccountHeader extends StatelessWidget {
  const _AccountHeader({required this.profile});

  final ProfileData? profile;

  @override
  Widget build(BuildContext context) {
    final name = profile?.account.fullName.isNotEmpty == true
        ? profile!.account.fullName
        : 'Usuario';
    final email = profile?.account.email ?? '';
    final tenant = profile?.account.tenantSlug ?? '';
    final initials = name
        .trim()
        .split(RegExp(r'\s+'))
        .where((part) => part.isNotEmpty)
        .take(2)
        .map((part) => part[0].toUpperCase())
        .join();
    return SizedBox(
      width: 245,
      child: Row(
        children: [
          CircleAvatar(
            radius: 20,
            child: Text(initials.isEmpty ? 'U' : initials),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  name,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(fontWeight: FontWeight.w700),
                ),
                if (email.isNotEmpty)
                  Text(
                    email,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                if (tenant.isNotEmpty)
                  Text(
                    tenant,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.labelSmall,
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
