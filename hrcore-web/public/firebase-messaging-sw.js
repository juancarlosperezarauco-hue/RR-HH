importScripts('https://www.gstatic.com/firebasejs/11.0.2/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/11.0.2/firebase-messaging-compat.js');

// Firebase Web config is intentionally public; private service-account keys never belong here.
let config = {};
try {
  const encoded = new URL(self.location.href).searchParams.get('config');
  if (encoded) config = JSON.parse(decodeURIComponent(escape(atob(encoded))));
} catch (_) {}
if (config.projectId) {
  firebase.initializeApp(config);
  firebase.messaging();
}
