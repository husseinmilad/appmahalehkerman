#!/usr/bin/env node
/**
 * تست دودی برنامهٔ وب (بدون نیاز به مرورگر)
 *
 *   cd /tmp && npm i jsdom        # یک‌بار
 *   node scripts/smoke-test.mjs /tmp/node_modules
 *
 * صفحهٔ docs/index.html را در jsdom اجرا می‌کند، همهٔ تب‌ها و پنل مدیریت را
 * می‌چرخاند و بررسی می‌کند خطای جاوااسکریپت رخ ندهد.
 */
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const modulesPath = process.argv[2] || '';
const jsdomEntry = modulesPath
  ? await import(resolve(modulesPath, 'jsdom/lib/api.js'))
  : await import('jsdom');
const { JSDOM, VirtualConsole } = jsdomEntry;

const html = readFileSync(resolve(root, 'docs/index.html'), 'utf8');
const errors = [];
const vc = new VirtualConsole();
vc.on('jsdomError', e => errors.push('jsdomError: ' + (e.stack || e.message)));
vc.on('error', (...a) => errors.push('console.error: ' + a.join(' ')));

const dom = new JSDOM(html, {
  runScripts: 'dangerously',
  pretendToBeVisual: true,
  url: 'https://husseinmilad.github.io/appmahalehkerman/',
  virtualConsole: vc,
  beforeParse(w) {
    w.fetch = () => Promise.reject(new Error('offline in test'));
    w.scrollTo = () => {};
    w.matchMedia = w.matchMedia || (() => ({ matches: false, addEventListener() {}, removeEventListener() {} }));
  }
});

const fail = msg => { console.error('❌ ' + msg); process.exitCode = 1; };
const pass = msg => console.log('✓ ' + msg);

setTimeout(() => {
  const w = dom.window;
  const d = w.document;
  const app = d.querySelector('#v');
  const click = el => el.dispatchEvent(new w.MouseEvent('click', { bubbles: true }));

  if (!d.querySelector('h1')) fail('صفحهٔ اول رندر نشد');
  else pass('صفحهٔ اول: ' + d.querySelector('h1').textContent.trim());

  const navCount = d.querySelectorAll('#nav button').length;
  navCount === 5 ? pass('نوار پایین: ۵ بخش') : fail('نوار پایین ناقص: ' + navCount);

  for (const tab of ['events', 'pay', 'reports', 'more', 'collab', 'contact', 'admin']) {
    w.go(tab);
    const len = app.innerHTML.length;
    len > 100 ? pass(`تب ${tab} (${len} کاراکتر)`) : fail(`تب ${tab} خالی است`);
  }

  // پنل مدیریت
  w.go('admin');
  d.querySelector('#pin').value = '1234';
  w.login();
  if (!/پنل مدیریت/.test(app.innerHTML)) fail('ورود مدیر انجام نشد');
  else pass('ورود مدیر با رمز پیش‌فرض');

  for (const section of ['اخبار', 'مراسمات', 'گزارش‌ها', 'پرداخت', 'تنظیمات', '📣 اعلان', 'پیام‌ها']) {
    const chip = [...d.querySelectorAll('.chips .chip')]
      .find(c => c.textContent.trim() === section && (c.getAttribute('onclick') || '').includes('adm.s'));
    if (!chip) { fail('بخش مدیریت پیدا نشد: ' + section); continue; }
    click(chip);
    pass(`بخش مدیریت ${section} (${app.innerHTML.length} کاراکتر)`);
  }

  // افزودن خبر
  click([...d.querySelectorAll('.chips .chip')].find(c => c.textContent.trim() === 'اخبار'));
  d.querySelector('#f_title').value = 'خبر آزمایشی';
  d.querySelector('#f_body').value = 'متن آزمایشی';
  w.sv('news');
  /خبر آزمایشی/.test(app.innerHTML) ? pass('ثبت خبر جدید') : fail('ثبت خبر ناموفق');

  // افزودن مراسم با تاریخ شمسی
  click([...d.querySelectorAll('.chips .chip')].find(c => c.textContent.trim() === 'مراسمات'));
  d.querySelector('#f_title').value = 'مراسم آزمایشی';
  d.querySelector('#f_time').value = '19:30';
  d.querySelector('#f_place').value = 'حسینیه';
  w.sv('events');
  w.go('home');
  /مراسم آزمایشی/.test(app.innerHTML) ? pass('ثبت مراسم و نمایش در صفحهٔ اول') : fail('مراسم ثبت نشد');
  /شمارش معکوس/.test(app.innerHTML) ? pass('کارت شمارش معکوس') : fail('شمارش معکوس نمایش داده نشد');

  if (errors.length) fail('خطاهای جاوااسکریپت:\n' + errors.slice(0, 5).join('\n'));
  else pass('بدون خطای جاوااسکریپت');

  console.log(process.exitCode ? '\nتست‌ها ناموفق بود.' : '\nهمهٔ تست‌ها موفق بود.');
  process.exit(process.exitCode || 0);
}, 1500);
