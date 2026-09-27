import { execFileSync } from 'node:child_process';
import { mkdir, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';

const serial = process.env.ANDROID_SERIAL;
if (!serial?.startsWith('emulator-')) throw new Error('Select a dedicated emulator with ANDROID_SERIAL; this fixture runner never seeds a personal phone.');
const adb = process.env.ADB || resolve('.local/android-sdk/platform-tools/adb.exe');
const output = resolve('.local/ui-public');
await mkdir(output, { recursive: true });
const run = args => execFileSync(adb, ['-s', serial, ...args], { encoding: 'utf8', windowsHide: true });
const sleep = ms => new Promise(r => setTimeout(r, ms));
const captures = [];
for (const [name, activity, extra] of [
  ['home', 'DemoHomeActivity', []], ['settings', 'DemoSettingsActivity', []],
  ['task', 'DemoTaskActivity', []], ['share', 'DemoShareActivity', []],
  ['home-dark', 'DemoHomeActivity', ['--es', 'appearance', 'dark']],
  ['home-zh', 'DemoHomeActivity', ['--es', 'language', 'zh']]
]) {
  run(['shell', 'am', 'force-stop', 'app.droprun.mobile.debug']);
  run(['shell', 'am', 'start', '-W', '-n', `app.droprun.mobile.debug/app.droprun.${activity}`, ...extra]);
  await sleep(1200);
  run(['shell', 'uiautomator', 'dump', '/sdcard/droprun-public-demo.xml']);
  const xml = run(['shell', 'cat', '/sdcard/droprun-public-demo.xml']);
  if (!xml.includes('app.droprun.mobile.debug')) throw new Error(`Wrong foreground application for ${name}`);
  const expected = { home: 'Recent handoffs', settings: 'Settings', task: 'Handoff', share: 'Where should this idea go?', 'home-dark': 'Recent handoffs', 'home-zh': '最近交办' }[name];
  if (!xml.includes(expected)) throw new Error(`Expected ${expected} for ${name}, but another screen is visible`);
  await writeFile(resolve(output, name + '.xml'), xml);
  await writeFile(resolve(output, name + '.png'), execFileSync(adb, ['-s', serial, 'exec-out', 'screencap', '-p'], { windowsHide: true, maxBuffer: 16000000 }));
  captures.push({ name, activity, syntheticData: true });
}
await writeFile(resolve(output, 'captures.json'), JSON.stringify({ serial, platform: run(['shell', 'getprop', 'ro.build.version.sdk']).trim(), package: 'app.droprun.mobile.debug', captures }, null, 2));
console.log(JSON.stringify({ output, count: captures.length, note: 'Actual app screens, synthetic fixture data; not end-to-end execution evidence.' }));
