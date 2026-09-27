import { execFileSync, spawn } from 'node:child_process';
import { mkdir } from 'node:fs/promises';
import { resolve } from 'node:path';
const serial = process.env.ANDROID_SERIAL;
if (!serial?.startsWith('emulator-')) throw new Error('Set ANDROID_SERIAL to a dedicated emulator.');
const adb = process.env.ADB || resolve('.local/android-sdk/platform-tools/adb.exe');
const output = resolve('.local/ui-public'); await mkdir(output,{recursive:true});
const run = args => execFileSync(adb,['-s',serial,...args],{encoding:'utf8',windowsHide:true});
const wait = ms=>new Promise(r=>setTimeout(r,ms));
run(['shell','am','start','-W','-f','0x10008000','-n','app.droprun.mobile.debug/app.droprun.DemoHomeActivity','--es','appearance','light','--es','language','en']);
await wait(1000);
const long = process.argv.includes('--long');
const seconds = long ? 72 : 24;
const filename = long ? 'walkthrough.mp4' : 'demo.mp4';
const child=spawn(adb,['-s',serial,'shell','screenrecord','--time-limit',String(seconds),'--bit-rate','3000000',`/sdcard/droprun-${filename}`],{windowsHide:true,stdio:'ignore'});
const done=new Promise((accept,reject)=>{child.on('error',reject);child.on('exit',code=>code===0?accept():reject(new Error('screenrecord failed')));});
for(const [i,activity] of ['DemoHomeActivity','DemoShareActivity','DemoTaskActivity','DemoSettingsActivity'].entries()){
 if(i)run(['shell','am','start','-W','-f','0x10008000','-n',`app.droprun.mobile.debug/app.droprun.${activity}`,'--es','appearance','light','--es','language','en']);
 await wait(long?16000:4500);
}
await done;
run(['pull',`/sdcard/droprun-${filename}`,resolve(output,filename)]);
console.log(JSON.stringify({file:resolve(output,filename),seconds,syntheticData:true,kind:'Actual app UI tour; not a real source-to-Codex task'}));
