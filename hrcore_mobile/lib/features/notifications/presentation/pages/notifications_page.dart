import 'package:flutter/material.dart';
import 'package:get_it/get_it.dart';

import '../../data/notification_repository.dart';

class NotificationsPage extends StatefulWidget {
  const NotificationsPage({super.key});

  @override
  State<NotificationsPage> createState() => _NotificationsPageState();
}

class _NotificationsPageState extends State<NotificationsPage> {
  late Future<List<MobileNotification>> _notifications;

  @override
  void initState() {
    super.initState();
    _notifications = GetIt.I<NotificationRepository>().list();
  }

  Future<void> _reload() async {
    setState(() => _notifications = GetIt.I<NotificationRepository>().list());
    await _notifications;
  }

  Future<void> _markAsRead(MobileNotification notification) async {
    if (notification.status == 'UNREAD') {
      await GetIt.I<NotificationRepository>().markAsRead(notification.id);
      await _reload();
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('Notificaciones')),
        body: RefreshIndicator(
          onRefresh: _reload,
          child: FutureBuilder<List<MobileNotification>>(
            future: _notifications,
            builder: (context, snapshot) {
              if (snapshot.connectionState == ConnectionState.waiting) {
                return const Center(child: CircularProgressIndicator());
              }
              if (snapshot.hasError) {
                return ListView(children: [
                  Padding(
                    padding: const EdgeInsets.all(32),
                    child: Text('No se pudieron cargar las notificaciones: ${snapshot.error}', textAlign: TextAlign.center),
                  ),
                ]);
              }
              final notifications = snapshot.data ?? const <MobileNotification>[];
              if (notifications.isEmpty) {
                return ListView(children: const [
                  Padding(
                    padding: EdgeInsets.all(48),
                    child: Center(child: Text('No tienes notificaciones.')),
                  ),
                ]);
              }
              return ListView.separated(
                padding: const EdgeInsets.all(16),
                itemCount: notifications.length,
                separatorBuilder: (_, _) => const SizedBox(height: 8),
                itemBuilder: (context, index) {
                  final notification = notifications[index];
                  return Card(
                    child: ListTile(
                      leading: Icon(
                        notification.status == 'UNREAD'
                            ? Icons.notifications_active_outlined
                            : Icons.notifications_none_outlined,
                      ),
                      title: Text(notification.title, style: const TextStyle(fontWeight: FontWeight.w700)),
                      subtitle: Text('${notification.body}\n${notification.createdAt.toLocal()}'),
                      isThreeLine: true,
                      onTap: () => _markAsRead(notification),
                    ),
                  );
                },
              );
            },
          ),
        ),
      );
}
