// Fase 9G-4: service worker minimal para receber push notifications.
// Payload esperado (JSON):
//   { title, body, frontendUrl, entityType?, entityId? }
// Rota mapeada pra manter em sync com src/app/services/notification-target.ts

/* eslint-disable no-restricted-globals */

function routeFor(entityType, entityId) {
  if (!entityType) return '/';
  switch (entityType) {
    case 'MenuPlan': return '/menus';
    case 'CompanyNutritionist': return '/nutritionist/company-invitations';
    case 'NutritionistClientLink': return '/invitations';
    case 'Chat': return entityId ? `/chat/${entityId}` : '/';
    default: return '/';
  }
}

self.addEventListener('push', (event) => {
  let data = {};
  try {
    data = event.data ? event.data.json() : {};
  } catch (_) {
    // payload nao-JSON → so exibe titulo generico
    data = { title: 'Prandium Nutri', body: event.data ? event.data.text() : '' };
  }
  const title = data.title || 'Prandium Nutri';
  const path = routeFor(data.entityType, data.entityId);
  const url = (data.frontendUrl || self.location.origin) + path;
  const options = {
    body: data.body || '',
    icon: '/favicon.ico',
    badge: '/favicon.ico',
    data: { url },
    tag: `prandium-${data.entityType || 'generic'}`,
  };
  event.waitUntil(self.registration.showNotification(title, options));
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const target = (event.notification.data && event.notification.data.url) || '/';
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((list) => {
      // Se ja tem tab aberta, foca e navega
      for (const client of list) {
        if ('focus' in client) {
          client.navigate(target).catch(() => {});
          return client.focus();
        }
      }
      // Senao, abre nova
      return self.clients.openWindow(target);
    })
  );
});

self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', (e) => e.waitUntil(self.clients.claim()));
