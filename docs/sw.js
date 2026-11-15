/* قرارگاه مردمی مدافعان وطن — محله کرمان
   Service Worker: پیش‌بارگذاری پوستهٔ برنامه + شبکه‌اول با بازگشت به کش */
const VERSION = 'v1.0.1';
const SHELL = 'qm-shell-' + VERSION;
const RUNTIME = 'qm-runtime-' + VERSION;
const SHELL_FILES = [
  './',
  'index.html',
  'offline.html',
  'manifest.webmanifest',
  'icons/icon-192.png',
  'icons/icon-512.png',
  'icons/maskable-512.png'
];

self.addEventListener('install', e => {
  e.waitUntil(
    caches.open(SHELL).then(c => c.addAll(SHELL_FILES).catch(() => c.add('index.html')))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys()
      .then(ks => Promise.all(ks.filter(k => k.startsWith('qm-') && k !== SHELL && k !== RUNTIME).map(k => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', e => {
  const req = e.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);

  // API گیت‌هاب و سرویس‌های بیرونی: همیشه از شبکه
  if (url.hostname === 'api.github.com' || url.hostname === 'api.open-meteo.com') return;

  // محتوای منتشرشده روی گیت‌هاب: شبکه‌اول، در نبود اینترنت آخرین نسخهٔ کش‌شده
  if (url.hostname === 'raw.githubusercontent.com') {
    e.respondWith(networkFirst(req, RUNTIME));
    return;
  }

  // فونت و استایل گوگل: کش‌اول
  if (url.hostname === 'fonts.googleapis.com' || url.hostname === 'fonts.gstatic.com') {
    e.respondWith(cacheFirst(req, RUNTIME));
    return;
  }

  // خود برنامه: شبکه‌اول با بازگشت به کش، و در نهایت صفحهٔ آفلاین
  if (url.origin === location.origin) {
    e.respondWith(
      networkFirst(req, SHELL).catch(() =>
        caches.match('offline.html', { ignoreSearch: true }).then(r => r || caches.match('index.html', { ignoreSearch: true }))
      )
    );
  }
});

async function networkFirst(req, cacheName) {
  const cache = await caches.open(cacheName);
  try {
    const res = await fetch(req, { cache: 'no-store' });
    if (res && (res.ok || res.type === 'opaque')) cache.put(req, res.clone());
    return res;
  } catch (err) {
    const hit = await caches.match(req, { ignoreSearch: true });
    if (hit) return hit;
    throw err;
  }
}

async function cacheFirst(req, cacheName) {
  const hit = await caches.match(req);
  if (hit) return hit;
  const cache = await caches.open(cacheName);
  const res = await fetch(req);
  if (res && (res.ok || res.type === 'opaque')) cache.put(req, res.clone());
  return res;
}

// امکان به‌روزرسانی فوری از داخل برنامه
self.addEventListener('message', e => {
  if (e.data === 'skipWaiting') self.skipWaiting();
});
