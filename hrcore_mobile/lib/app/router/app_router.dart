import 'package:go_router/go_router.dart';

import '../../features/auth/presentation/pages/login_page.dart';
import '../../features/auth/presentation/pages/biometric_lock_page.dart';
import '../../features/home/presentation/pages/app_shell_page.dart';
import '../../features/notifications/presentation/pages/notifications_page.dart';
import 'route_names.dart';

GoRouter createAppRouter({String initialLocation = RouteNames.loginPath}) => GoRouter(
  initialLocation: initialLocation,
  routes: [
    GoRoute(
      name: RouteNames.login,
      path: RouteNames.loginPath,
      builder: (context, state) => const LoginPage(),
    ),
    GoRoute(
      name: RouteNames.home,
      path: RouteNames.homePath,
      builder: (context, state) => const AppShellPage(),
    ),
    GoRoute(
      name: RouteNames.biometricLock,
      path: RouteNames.biometricLockPath,
      builder: (context, state) => const BiometricLockPage(),
    ),
    GoRoute(
      name: RouteNames.notifications,
      path: RouteNames.notificationsPath,
      builder: (context, state) => const NotificationsPage(),
    ),
  ],
);
