declare global { interface Window { __HRCORE_RUNTIME_CONFIG__?: { firebaseWeb?: Record<string, string> } } }
const runtime = typeof window !== 'undefined' ? (window.__HRCORE_RUNTIME_CONFIG__?.firebaseWeb ?? {}) : {};

export const environment = {
  production: false,
  // En desarrollo, las rutas relativas pasan por proxy.conf.json.
  apiUrl: '',
  firebaseWeb: {
    apiKey: runtime['apiKey'] ?? '', authDomain: runtime['authDomain'] ?? '', projectId: runtime['projectId'] ?? '', storageBucket: runtime['storageBucket'] ?? '',
    messagingSenderId: runtime['messagingSenderId'] ?? '', appId: runtime['appId'] ?? '', measurementId: runtime['measurementId'] ?? '', vapidKey: runtime['vapidKey'] ?? ''
  },
  // Toggle para desarrollo desconectado sin backend
  useMocks: false
};
