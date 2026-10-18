import 'package:go_router/go_router.dart';

import '../../features/auth/presentation/pages/login_page.dart';
import '../../features/home/presentation/pages/app_shell_page.dart';
import 'route_names.dart';

final GoRouter appRouter = GoRouter(
  initialLocation: RouteNames.loginPath,
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
  ],
);
