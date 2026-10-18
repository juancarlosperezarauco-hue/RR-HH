import 'package:flutter/material.dart';

import '../../../profile/presentation/pages/profile_page.dart';
import '../../../attendance/presentation/pages/attendance_page.dart';
import 'home_page.dart';

class AppShellPage extends StatefulWidget {
  const AppShellPage({super.key});

  @override
  State<AppShellPage> createState() => _AppShellPageState();
}

class _AppShellPageState extends State<AppShellPage> {
  int _selectedIndex = 0;

  static const _sections = [
    _AppSection('Inicio', Icons.home_outlined),
    _AppSection('Jornada', Icons.access_time_outlined),
    _AppSection('Permisos', Icons.assignment_outlined),
    _AppSection('Archivos', Icons.folder_open_outlined),
    _AppSection('Perfil', Icons.person_outline),
  ];

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
                onPressed: () => ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                    content: Text('No tienes notificaciones nuevas'),
                  ),
                ),
                icon: const Icon(Icons.notifications_none_outlined),
              ),
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
            padding: const EdgeInsets.only(left: 4, right: 16),
            child: CircleAvatar(
              radius: 16,
              backgroundColor: Theme.of(context).colorScheme.primaryContainer,
              child: Text(
                'CA',
                style: TextStyle(
                  color: Theme.of(context).colorScheme.primary,
                  fontSize: 12,
                  fontWeight: FontWeight.w700,
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
        children: const [
          HomePage(),
          AttendancePage(),
          _SectionPlaceholder(
            title: 'Permisos',
            message: 'Gestiona vacaciones, permisos y otras solicitudes.',
            icon: Icons.assignment_outlined,
          ),
          _SectionPlaceholder(
            title: 'Archivos',
            message: 'Encuentra tus documentos laborales.',
            icon: Icons.folder_open_outlined,
          ),
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
}

class _AppSection {
  const _AppSection(this.label, this.icon);

  final String label;
  final IconData icon;
}

class _SectionPlaceholder extends StatelessWidget {
  const _SectionPlaceholder({
    required this.title,
    required this.message,
    required this.icon,
  });

  final String title;
  final String message;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 48, color: Theme.of(context).colorScheme.primary),
            const SizedBox(height: 16),
            Text(
              title,
              style: Theme.of(
                context,
              ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 8),
            Text(message, textAlign: TextAlign.center),
          ],
        ),
      ),
    );
  }
}
