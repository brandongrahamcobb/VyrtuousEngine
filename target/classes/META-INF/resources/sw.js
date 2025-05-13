// Minimal Service Worker stub to satisfy PWA registration
// This service worker does not cache resources or provide offline functionality.

// Immediately take control of the page
self.addEventListener('install', event => {
  self.skipWaiting();
});
self.addEventListener('activate', event => {
  event.waitUntil(self.clients.claim());
});