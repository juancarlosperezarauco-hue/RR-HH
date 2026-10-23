import 'package:flutter/material.dart';

import '../core/theme/app_theme.dart';
import 'router/app_router.dart';
import 'router/route_names.dart';

class App extends StatelessWidget {
  const App({super.key, this.initialLocation = RouteNames.loginPath});

  final String initialLocation;

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'HRCore',
      theme: AppTheme.light,
      routerConfig: createAppRouter(initialLocation: initialLocation),
    );
  }
}
