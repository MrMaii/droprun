import { mkdir, writeFile, readdir, readFile, stat, unlink } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { join, extname } from 'node:path';
import { spawn } from 'node:child_process';
import { lookup } from 'node:dns/promises';
import { isIP } from 'node:net';
import { createHash } from 'node:crypto';

export function run(executable, args, cwd, timeout = 180000) {
  return new Promise((resolve, reject) => {
    const child = spawn(executable, args, { cwd, windowsHide: true });
    let output = '';
    const capture = b => { output = (output + String(b)).slice(-12000); };
    child.stdout.on('data', capture); child.stderr.on('data', capture);
    const timer = setTimeout(() => { child.kill(); reject(new Error('Media processing timed out')); }, timeout);
    child.on('error', e => { clearTimeout(timer); reject(e); });
    child.on('exit', code => { clearTimeout(timer); code === 0 ? resolve(output) : reject(new Error(output.slice(-1200) || `${executable}: ${code}`)); });
  });
}
export function privateAddress(a) {
  return /^(127\.|10\.|192\.168\.|169\.254\.|172\.(1[6-9]|2\d|3[01])\.|0\.|::1$|fc|fd|fe80|::ffff:)/i.test(a);
}
export async function publicUrl(value) {
  const url = new URL(value);
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || (url.port && !['80', '443'].includes(url.port))) throw new Error('Unsupported source URL');
  const addresses = isIP(url.hostname) ? [{ address: url.hostname }] : await lookup(url.hostname, { all: true });
  if (!addresses.length || addresses.some(x => privateAddress(x.address))) throw new Error('Private network URLs are not accepted as shared media');
  return url;
}

const social = /(^|\.)(instagram\.com|x\.com|twitter\.com|youtube\.com|youtu\.be|tiktok\.com|bilibili\.com|xiaohongshu\.com|xhslink\.com)$/;
const UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0 Safari/537.36';
const decode = text => text.replace(/&(#x[0-9a-f]+|#\d+|amp|lt|gt|quot|apos|nbsp);/gi, (m, code) => {
  if (code[0] === '#') return String.fromCodePoint(code[1].toLowerCase() === 'x' ? parseInt(code.slice(2), 16) : parseInt(code.slice(1), 10));
  return { amp: '&', lt: '<', gt: '>', quot: '"', apos: "'", nbsp: ' ' }[code.toLowerCase()];
});
const clip = (text, max) => { const value = String(text ?? '').replace(/\r/g, '').trim(); return value.length > max ? value.slice(0, max) + '…' : value; };
const stamp = seconds => `${Math.floor(seconds / 60)}:${String(Math.round(seconds % 60)).padStart(2, '0')}`;

/** Reads Open Graph metadata (and the cover image) for links the media extractor could not fully fetch. */
export async function fetchPage(url, dir, index, fetchImpl = fetch, checkUrl = publicUrl) {
  const response = await fetchImpl(url, { headers: { 'User-Agent': UA, Accept: 'text/html,*/*' }, redirect: 'follow', signal: AbortSignal.timeout(15000) });
  if (!response.ok) throw new Error('HTTP ' + response.status);
  const html = (await response.text()).slice(0, 2 * 1024 * 1024);
  const meta = name => { const m = html.match(new RegExp(`<meta[^>]+(?:property|name)=["']${name}["'][^>]*content=["']([^"']*)["']`, 'i')) || html.match(new RegExp(`<meta[^>]+content=["']([^"']*)["'][^>]*(?:property|name)=["']${name}["']`, 'i')); return m ? decode(m[1]).trim() : ''; };
  const page = { url: String(url), title: meta('og:title') || decode((html.match(/<title[^>]*>([^<]*)<\/title>/i) || [, ''])[1]).trim(), description: meta('og:description') || meta('description'), site: meta('og:site_name'), image: null };
  const image = meta('og:image');
  if (image && dir) {
    try {
      const imageUrl = await checkUrl(new URL(image, url).toString());
      const picture = await fetchImpl(imageUrl, { headers: { 'User-Agent': UA }, signal: AbortSignal.timeout(15000) });
      const type = picture.headers.get('content-type') || '';
      if (picture.ok && /^image\/(jpeg|png|webp)/.test(type)) {
        const bytes = Buffer.from(await picture.arrayBuffer());
        if (bytes.length && bytes.length <= 8 * 1024 * 1024) { const path = join(dir, `page-${index}-cover.${type.includes('png') ? 'png' : type.includes('webp') ? 'webp' : 'jpg'}`); await writeFile(path, bytes); page.image = path; }
      }
    } catch {}
  }
  return page;
}

function subtitleText(raw) {
  const lines = raw.replace(/\r/g, '').split('\n').map(line => line.replace(/<[^>]+>/g, '').trim())
    .filter(line => line && !/^(WEBVTT|NOTE|Kind:|Language:|\d+)$/.test(line) && !/-->/.test(line));
  const out = [];
  for (const line of lines) if (out.at(-1) !== line) out.push(line);
  return out.join('\n');
}

/**
 * Short UI demos get two samples per second; longer sources keep a bounded overview.
 * Motion windows supplement this overview; sampled frames never imply continuous video understanding.
 */
export function samplingPlan(duration, { maxFrames = 60, maxSeconds = 1800 } = {}) {
  const seconds = Math.max(0, Math.min(Number(duration) || 0, maxSeconds));
  if (!seconds) return { interval: 10, frames: 36, covered: 360, truncated: true };
  const interval = Math.max(0.5, Math.ceil(seconds / maxFrames * 2) / 2);
  return { interval, frames: Math.min(maxFrames, Math.ceil(seconds / interval)), covered: seconds, truncated: (Number(duration) || 0) > maxSeconds };
}

async function contactSheets(frames, dir, name, config) {
  const sheets = [];
  const perSheet = 20;
  for (let start = 0, index = 1; start < frames.length && sheets.length < 3; start += perSheet, index++) {
    const listPath = join(dir, `sheet-${name}-${index}.txt`);
    await writeFile(listPath, frames.slice(start, start + perSheet).map(path => `file '${path.replaceAll('\\', '/').replaceAll("'", "'\\''")}'`).join('\n') + '\n');
    const out = join(dir, `sheet-${name}-${index}.jpg`);
    const count = Math.min(perSheet, frames.length - start);
    const cols = count <= 4 ? count : 4, rows = Math.ceil(count / cols);
    await run(config.ffmpeg || 'ffmpeg', ['-nostdin', '-y', '-hide_banner', '-loglevel', 'error', '-f', 'concat', '-safe', '0', '-i', listPath, '-vf', `scale=480:-2,tile=${cols}x${rows}:padding=4:margin=4:color=black`, '-frames:v', '1', '-q:v', '4', out], dir, 120000);
    sheets.push(out);
  }
  return sheets;
}

async function sampleFrames(video, dir, name, start, seconds, interval, count, config) {
  const frameDir = join(dir, 'frames-' + name);
  await mkdir(frameDir, { recursive: true });
  for (const file of (await readdir(frameDir)).filter(n => /^frame-\d+\.jpg$/.test(n))) await unlink(join(frameDir, file));
  await run(config.ffmpeg || 'ffmpeg', ['-nostdin', '-y', '-hide_banner', '-loglevel', 'error', '-ss', String(start), '-t', String(seconds), '-i', video, '-vf', `fps=1/${interval}:start_time=0:round=up,scale=960:-2`, '-frames:v', String(count), '-q:v', '3', join(frameDir, 'frame-%03d.jpg')], dir, 300000);
  const names = (await readdir(frameDir)).filter(n => /^frame-\d+\.jpg$/.test(n)).sort().slice(0, count);
  const frames = [];
  for (const [index, name] of names.entries()) {
    const path = join(frameDir, name), bytes = await readFile(path);
    if (bytes.length > 3 && bytes[0] === 0xff && bytes[1] === 0xd8) frames.push({ path, timeSeconds: Number((start + index * interval).toFixed(3)), sha256: createHash('sha256').update(bytes).digest('hex') });
  }
  if (!frames.length) throw new Error('没有取得可解码的视频帧');
  return { intervalSeconds: interval, frames, sheets: [] };
}

async function motionWindows(video, dir, name, seconds, config) {
  // Analyze small frames, then retain at most two separate two-second passages at 8 fps.
  // The scene score locates pixel changes, not their semantic trigger or easing curve.
  const scoreName = `motion-${name}.txt`;
  await run(config.ffmpeg || 'ffmpeg', ['-nostdin', '-y', '-hide_banner', '-loglevel', 'error', '-t', String(seconds), '-i', video, '-vf', `fps=4,scale=160:-2,select='gte(scene,0)',metadata=mode=print:file=${scoreName}`, '-an', '-f', 'null', '-'], dir, 180000);
  const raw = await readFile(join(dir, scoreName), 'utf8');
  const scores = [...raw.matchAll(/pts_time:([\d.]+)[^\r\n]*\r?\n(?:[^\r\n]*\r?\n)*?lavfi\.scene_score=([\d.e+-]+)/g)]
    .map(match => ({ time: Number(match[1]), score: Number(match[2]) })).filter(item => item.score > 0.00001 && item.time < seconds);
  const selected = [];
  for (const item of scores.sort((a, b) => b.score - a.score || a.time - b.time)) {
    const startSeconds = Number(Math.max(0, Math.min(item.time - 0.5, seconds - 2)).toFixed(3));
    const endSeconds = Number(Math.min(seconds, startSeconds + 2).toFixed(3));
    if (selected.every(window => startSeconds >= window.endSeconds || endSeconds <= window.startSeconds)) selected.push({ startSeconds, endSeconds });
    if (selected.length === 2) break;
  }
  return selected.sort((a, b) => a.startSeconds - b.startSeconds);
}

/** A mechanical intake check only. The agent must still identify the intended page, trigger and visual states. */
export function assessVisualEvidence(material, { motion = false } = {}) {
  const evidence = material?.visualEvidence, images = new Set(material?.images || []);
  const videos = evidence?.videos || [], reasons = [];
  const represented = sequence => sequence?.frames?.length > 0 && (sequence.sheets?.length ? sequence.sheets.every(path => images.has(path)) : sequence.frames.every(frame => images.has(frame.path)));
  const hasStatic = [...(evidence?.attachedImages || []), ...(evidence?.coverImages || [])].some(path => images.has(path)) || videos.some(video => represented(video.overview));
  if (!motion) return { sufficient: hasStatic, kind: hasStatic ? 'static' : 'none', reasons: hasStatic ? [] : ['未取得已附给 Codex 的参考图片或视频画面。'] };
  if (!videos.length) reasons.push('没有取得可读取的原视频；链接、标题、字幕和封面不足以复现动效。');
  for (const video of videos) {
    if (!video.durationSeconds || !video.decoded) reasons.push('原视频时长或解码未验证，无法确认动态画面覆盖。');
    if (video.truncated || video.processedSeconds < video.durationSeconds) reasons.push('视频未完整采样；请提供包含目标效果的较短片段。');
    if (!represented(video.overview) || video.overview.frames.length < 2) reasons.push('视频的时序总览未完整附给 Codex。');
    if (video.overview.intervalSeconds > 1) reasons.push('视频总览间隔超过一秒；请提供目标效果的短片段以免漏掉交互。');
    const details = video.motionWindows.filter(window => represented(window) && window.frames.length >= 2);
    if (!details.length || video.uniqueFrameCount < 2) reasons.push('未取得至少两个不同状态及密集时序画面，不能据此确认动效。');
  }
  return { sufficient: reasons.length === 0, kind: reasons.length === 0 ? 'motion' : hasStatic ? 'static' : 'none', reasons: [...new Set(reasons)] };
}

async function transcribe(video, name, dir, seconds, api, config) {
  const chunk = 300, limit = Math.min(seconds || 360, 1800);
  const parts = [];
  for (let start = 0; start < limit; start += chunk) {
    const audio = join(dir, `${name}.${start}.wav`);
    await run(config.ffmpeg || 'ffmpeg', ['-nostdin', '-y', '-hide_banner', '-loglevel', 'error', '-ss', String(start), '-t', String(chunk), '-i', video, '-vn', '-ac', '1', '-ar', '16000', audio], dir);
    const size = (await stat(audio)).size;
    if (size < 2000) break;
    const result = await api('/connector/transcribe', { audio: (await readFile(audio)).toString('base64') });
    const text = (result.text || '').trim();
    if (text) parts.push({ start, text });
    if (!seconds && !text) break;
  }
  if (!parts.length) throw new Error('转写没有返回文字');
  const textPath = join(dir, name + '.transcript.json');
  await writeFile(textPath, JSON.stringify(parts, null, 2));
  return { path: textPath, text: parts.length === 1 ? parts[0].text : parts.map(part => `[${stamp(part.start)}] ${part.text}`).join('\n'), covered: Math.min(limit, parts.at(-1).start + chunk) };
}

export function describeMaterial(material) {
  const d = material.digest, parts = [];
  const seconds = Number(d.duration) || 0;
  if (material.coverage.video === 'downloaded') parts.push('视频' + (seconds ? ' ' + stamp(seconds) : ''));
  else if (material.pages.length) parts.push('网页信息');
  else if (material.attachments.some(name => /\.(mp4|webm|mov|mkv)$/i.test(name))) parts.push('视频文件');
  else if (material.attachments.length) parts.push(`${material.attachments.length} 个附件`);
  else if (material.source && !material.urls.length) parts.push('文字');
  if (material.coverage.audio === 'transcribed') parts.push('已转写');
  else if (d.subtitles) parts.push('有字幕');
  if (material.frames) parts.push(`${material.frames} 张关键帧`);
  if (material.attachments.some(name => /\.(jpe?g|png|webp)$/i.test(name))) parts.push(`${material.attachments.filter(name => /\.(jpe?g|png|webp)$/i.test(name)).length} 张图片`);
  if (!parts.length) parts.push(material.urls.length ? '仅链接' : '空材料');
  if (material.needsLogin) parts.push('视频需登录才能读取');
  const title = d.title ? ` · 「${clip(d.title, 40)}」` : '';
  return parts.join(' · ') + title;
}

/** Markdown block placed in the Codex message so the model reads the content itself instead of being told to go fetch it. */
export function renderBrief(material) {
  const d = material.digest, lines = [];
  for (const url of material.urls) lines.push('链接：' + url);
  if (d.title) lines.push('标题：' + d.title);
  if (d.author) lines.push('作者：' + d.author);
  if (d.duration) lines.push('时长：' + stamp(d.duration));
  if (d.description) lines.push('简介：' + clip(d.description, 1200));
  for (const page of material.pages) if (!d.title || page.title !== d.title) lines.push(`网页「${clip(page.title || page.url, 80)}」：${clip(page.description, 600) || '（无摘要）'}`);
  if (material.source && !material.urls.length) lines.push('分享的文字：\n' + clip(material.source, 4000));
  else if (material.source && material.source.replace(/https?:\/\/\S+/g, '').trim()) lines.push('分享时附带的文字：' + clip(material.source.replace(/https?:\/\/\S+/g, '').trim(), 1000));
  if (d.transcript) lines.push('语音文字稿（Whisper）：\n"""\n' + clip(d.transcript, 12000) + '\n"""');
  else if (d.subtitles) lines.push('字幕：\n"""\n' + clip(d.subtitles, 12000) + '\n"""');
  if (material.frames) {
    if (material.visualEvidence?.videos.length) {
      lines.push(`画面：已提取 ${material.frames} 张时序帧，联络表从左到右、从上到下排列。时间为原视频采样位置（非逐帧完整观看）；单帧及 SHA-256 记录在 ${join(material.dir, 'material.json')}。`);
      for (const [index, video] of material.visualEvidence.videos.entries()) {
        lines.push(`视频 ${index + 1}：${video.path}；原时长 ${video.durationSeconds ?? '未知'} 秒；总览实际覆盖 ${video.processedSeconds} 秒${video.truncated ? '（有截断）' : ''}；${video.uniqueFrameCount} 个不同采样画面。`);
        for (const [label, sequence] of [['总览', video.overview], ...video.motionWindows.map((window, i) => [`动效片段 ${i + 1}`, window])]) {
          if (!sequence.frames.length) continue;
          lines.push(`${label}：${sequence.frames[0].timeSeconds}–${sequence.frames.at(-1).timeSeconds} 秒，每 ${sequence.intervalSeconds} 秒一帧。`);
          for (const [i, sheet] of sequence.sheets.entries()) lines.push(`联络表 ${sheet}${material.images.includes(sheet) ? '（已附）' : '（未附，需本地读取）'}：${sequence.frames.slice(i * 20, (i + 1) * 20).map(frame => frame.timeSeconds + 's').join('、')}。`);
          if (!sequence.sheets.length) lines.push('单帧位置：' + sequence.frames.map(frame => `${frame.timeSeconds}s=${frame.path}${material.images.includes(frame.path) ? '（已附）' : ''}`).join('；'));
        }
      }
      lines.push('必须先用这些时序状态确认触发动作与视觉变化；采样间隙、触发方式和缓动曲线仍需结合内容判断，不得编造效果名称。');
    } else {
      const plan = material.sampling || {};
      lines.push(`画面：视频每 ${plan.interval || 10} 秒抽一帧，共 ${material.frames} 张，按时间顺序拼成 ${material.sheets?.length || 0} 张联络表图。单帧文件在 ${material.dir}。`);
    }
  }
  const images = material.attachments.filter(name => /\.(jpe?g|png|webp)$/i.test(name));
  if (images.length) lines.push(`手机附带的图片：已附 ${Math.min(images.length, 12)} 张`);
  const other = material.attachments.filter(name => !/\.(jpe?g|png|webp)$/i.test(name));
  if (other.length) lines.push('手机附带的文件：' + other.join('、') + `（位于 ${material.dir}）`);
  if (material.limitations.length) lines.push('读取范围说明：\n' + material.limitations.map(item => '- ' + item).join('\n'));
  return lines.join('\n');
}

export async function prepare(task, dir, api, config, { fetchImpl = fetch, checkUrl = publicUrl } = {}) {
  await mkdir(dir, { recursive: true });
  const material = { source: task.content, dir, files: [], images: [], attachments: [], urls: [], pages: [], frames: 0, sceneFrames: 0, sheets: [], sampling: null, needsLogin: false, limitations: [], visualEvidence: { version: 1, videos: [], attachedImages: [], coverImages: [], limitations: [] }, digest: { title: '', description: '', author: '', duration: 0, transcript: '', subtitles: '' }, coverage: { text: task.content ? 'shared_text_only' : 'missing', video: 'missing', visual: 'missing', audio: 'missing' } };
  await writeFile(join(dir, 'share.txt'), task.content, 'utf8');
  for (const a of JSON.parse(task.assets || '[]')) {
    const ext = extname(a.name).replace(/[^.a-zA-Z0-9]/g, '').slice(0, 10) || (a.mime.startsWith('image/') ? '.jpg' : '.mp4');
    const path = join(dir, a.id + ext);
    const r = await api('/uploads/' + a.id, null, 'GET', true);
    await writeFile(path, Buffer.from(await r.arrayBuffer()));
    material.files.push(path); material.attachments.push(a.name || (a.id + ext));
    if (/\.(jpe?g|png|webp)$/i.test(path)) { material.images.push(path); material.visualEvidence.attachedImages.push(path); }
  }
  material.urls = [...new Set(task.content.match(/https?:\/\/[^\s<>"）)]+/g) || [])].slice(0, 3);
  let index = 0;
  for (const value of material.urls) {
    index++;
    let url;
    try { url = await checkUrl(value); } catch (e) { material.limitations.push(`链接不可读取：${value}。${e.message.slice(-300)}`); continue; }
    let fetched = false;
    if (social.test(url.hostname)) {
      try {
        const args = [...(config.ytDlp ? [] : ['-m', 'yt_dlp']), '--no-playlist', '--no-progress', '--socket-timeout', '20', '--retries', '1', '--max-filesize', '80M', '--write-info-json', '--write-subs', '--write-auto-subs', '--sub-langs', 'en,zh.*', '-f', 'b[height<=720]/bv[height<=720]+ba/b', '--merge-output-format', 'mp4', '-o', 'source-%(id)s.%(ext)s'];
        if (config.ffmpeg) args.push('--ffmpeg-location', config.ffmpeg);
        if (config.cookiesFile && existsSync(config.cookiesFile)) args.push('--cookies', config.cookiesFile);
        else if (config.cookiesFromBrowser) args.push('--cookies-from-browser', config.cookiesFromBrowser);
        await run(config.ytDlp || config.python || 'python', [...args, '--', value], dir, 300000);
        fetched = true;
      } catch (e) {
        const message = e.message.slice(-500);
        material.limitations.push(`视频未完整取得：${value}。${clip(message.replace(/\s+/g, ' '), 300)}`);
        if (/login|log in|cookies|rate-limit|not available|private|18 years|sign in/i.test(message)) {
          material.needsLogin = true;
          material.limitations.push('该平台要求登录后才能读取。在电脑上打开 http://127.0.0.1:47493/login 登录一次即可；或者在手机上先把视频保存到相册，再从相册分享。');
        }
      }
    }
    if (!fetched) {
      try { const page = await fetchPage(url, dir, index, fetchImpl, checkUrl); material.pages.push(page); if (page.image) { material.files.push(page.image); material.images.push(page.image); material.visualEvidence.coverImages.push(page.image); } }
      catch (e) { material.limitations.push(`网页信息未取得：${value}。${e.message.slice(-200)}`); }
    }
  }
  const names = await readdir(dir);
  material.files = [...new Set([...material.files, ...names.filter(n => !n.endsWith('.part') && !/^(share\.txt|material\.json|material\.md)$/.test(n)).map(n => join(dir, n))])];
  for (const name of names.filter(n => /\.info\.json$/.test(n))) {
    try {
      const info = JSON.parse(await readFile(join(dir, name), 'utf8'));
      material.digest.title ||= info.title || info.fulltitle || '';
      material.digest.description ||= info.description || '';
      material.digest.author ||= info.uploader || info.channel || info.creator || '';
      material.digest.duration ||= Number(info.duration) || 0;
    } catch {}
  }
  for (const name of names.filter(n => /\.(vtt|srt)$/.test(n))) {
    try { const text = subtitleText(await readFile(join(dir, name), 'utf8')); if (text.length > material.digest.subtitles.length) material.digest.subtitles = text; } catch {}
  }
  if (material.digest.subtitles) material.coverage.text = 'partial';
  if (names.some(n => /\.(jpg|jpeg|png|webp)$/i.test(n))) material.coverage.visual = 'partial';
  for (const page of material.pages) { material.digest.title ||= page.title; material.digest.description ||= page.description; }
  const maxSeconds = Number(config.maxVideoMinutes || 30) * 60;
  for (const name of names.filter(n => /\.(mp4|webm|mov|mkv)$/i.test(n))) {
    material.coverage.video = 'downloaded';
    const video = join(dir, name), tag = name.replace(/\W/g, '_');
    let duration = 0;
    try {
      const probe = JSON.parse(await run(config.ffprobe || 'ffprobe', ['-v', 'error', '-select_streams', 'v:0', '-show_entries', 'format=duration:stream=codec_type,width,height,duration', '-of', 'json', video], dir, 30000));
      if (probe.streams?.[0]?.codec_type === 'video' && probe.streams[0].width > 0 && probe.streams[0].height > 0) duration = Number(probe.format?.duration || probe.streams[0].duration) || 0;
    } catch {}
    material.digest.duration ||= duration;
    const plan = samplingPlan(duration, { maxSeconds });
    material.sampling = plan;
    const evidence = { path: video, durationSeconds: duration || null, processedSeconds: 0, decoded: false, truncated: plan.truncated, overview: { intervalSeconds: plan.interval, frames: [], sheets: [] }, motionWindows: [], uniqueFrameCount: 0, limitations: [] };
    material.visualEvidence.videos.push(evidence);
    const addSequence = async (sequence, label) => {
      const frames = sequence.frames.map(frame => frame.path);
      material.files.push(...frames); material.frames += frames.length;
      material.coverage.visual = 'partial';
      try {
        sequence.sheets = await contactSheets(frames, dir, label, config);
        material.sheets.push(...sequence.sheets); material.files.push(...sequence.sheets); material.images.push(...sequence.sheets);
      } catch (e) {
        evidence.limitations.push('联络表生成失败，改为附单帧：' + clip(e.message.replace(/\s+/g, ' '), 160));
        material.images.push(...frames.slice(0, 8));
      }
    };
    try {
      evidence.overview = await sampleFrames(video, dir, tag, 0, plan.covered, plan.interval, plan.frames, config);
      evidence.decoded = true;
      evidence.processedSeconds = Number(Math.min(plan.covered, evidence.overview.frames.at(-1).timeSeconds + plan.interval).toFixed(3));
      await addSequence(evidence.overview, tag);
      evidence.limitations.push(`总览每 ${plan.interval} 秒采样；未取得采样时刻之间的连续画面。`);
      if (plan.truncated) evidence.limitations.push(duration ? `视频超过 ${Math.round(maxSeconds / 60)} 分钟，只采样前 ${Math.round(plan.covered / 60)} 分钟。` : '视频时长未知，最多采样前六分钟，无法确认全部时间覆盖。');
    } catch (e) { evidence.limitations.push('关键帧处理失败：' + clip(e.message.replace(/\s+/g, ' '), 200)); }
    try {
      if (evidence.overview.frames.length > 1) for (const [index, window] of (await motionWindows(video, dir, tag, evidence.processedSeconds, config)).entries()) {
        const label = tag + '-motion-' + (index + 1);
        const sequence = { ...window, ...await sampleFrames(video, dir, label, window.startSeconds, window.endSeconds - window.startSeconds, 0.125, Math.ceil((window.endSeconds - window.startSeconds) * 8), config) };
        evidence.motionWindows.push(sequence);
        await addSequence(sequence, label);
      }
    } catch (e) { evidence.limitations.push('动效密集采样失败：' + clip(e.message.replace(/\s+/g, ' '), 200)); }
    if (evidence.motionWindows.length) evidence.limitations.push('变化定位每秒检测四帧，仅对最多两个两秒片段按每秒八帧采样；更快的过渡、其他片段的细节及交互触发关系仍可能缺失。');
    evidence.uniqueFrameCount = new Set([evidence.overview, ...evidence.motionWindows].flatMap(sequence => sequence.frames.map(frame => frame.sha256))).size;
    material.limitations.push(...evidence.limitations);
    try {
      const transcript = await transcribe(video, name, dir, Math.min(duration, plan.covered || 360), api, config);
      material.files.push(transcript.path); material.coverage.audio = 'transcribed';
      if (transcript.text.length > material.digest.transcript.length) material.digest.transcript = transcript.text;
      if (duration > transcript.covered + 5) material.limitations.push(`语音转写覆盖到 ${stamp(transcript.covered)}。`);
    } catch (e) { material.limitations.push(/does not contain any stream|Output file is empty|matches no streams|转写没有返回文字/i.test(e.message) ? '视频没有音轨或没有可辨认的语音。' : '音频转写失败：' + clip(e.message.replace(/\s+/g, ' '), 200)); }
  }
  if (material.coverage.audio === 'missing' && material.coverage.video === 'downloaded' && !material.digest.subtitles) material.limitations.push('没有讲解文字；不能声称理解了视频里说的话，画面信息以联络表为准。');
  if (material.urls.length && material.coverage.video === 'missing' && !material.pages.length) material.limitations.push('链接内容没有取得，只有链接本身。');
  material.images = [...new Set(material.images)].slice(0, 12);
  material.visualEvidence.limitations = [...material.limitations];
  material.summary = describeMaterial(material);
  material.brief = renderBrief(material);
  await writeFile(join(dir, 'material.json'), JSON.stringify(material, null, 2));
  await writeFile(join(dir, 'material.md'), material.brief, 'utf8');
  return material;
}
