import { readFile, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { Resvg } from '@resvg/resvg-js';

// Editorial artwork only. Native screenshots remain separate, unedited files.
const directory = new URL('../assets/brand/', import.meta.url);
const mark = await readFile(new URL('mark.png', directory));
const logo = `data:image/png;base64,${mark.toString('base64')}`;
const compact = process.argv.includes('--compact');

for (const language of ['en', 'zh']) {
  for (const theme of ['light', 'dark']) {
    const dark = theme === 'dark';
    const ink = dark ? '#f5f7f2' : '#191d1a';
    const muted = dark ? '#acb6aa' : '#59645b';
    const surface = dark ? '#151b16' : '#f6f7f2';
    const line = dark ? '#303b30' : '#dde4d6';
    const title = language === 'en'
      ? ['Share from your phone.', 'Put local Codex to work.']
      : ['手机分享参考，', '本地 Codex 接手。'];
    const subtitle = language === 'en'
      ? 'YOUR REFERENCES. YOUR PROJECTS. YOUR SETUP.'
      : '你的参考 · 你的项目 · 你的环境';
    const svg = compact ? `<svg xmlns="http://www.w3.org/2000/svg" width="640" height="480" viewBox="0 0 640 480">
      <defs>
        <radialGradient id="light" cx="100%" cy="0%" r="100%">
          <stop offset="0" stop-color="#b8ef73" stop-opacity="${dark ? '.16' : '.22'}"/>
          <stop offset="1" stop-color="#b8ef73" stop-opacity="0"/>
        </radialGradient>
        <clipPath id="mark"><rect x="48" y="44" width="112" height="112" rx="25"/></clipPath>
      </defs>
      <rect width="640" height="480" rx="32" fill="${surface}"/>
      <rect width="640" height="480" rx="32" fill="url(#light)"/>
      <g font-family="Segoe UI, Microsoft YaHei, sans-serif">
        <text x="192" y="117" font-size="64" font-weight="600" letter-spacing="-2.5" fill="${ink}">DropRun</text>
        <text x="48" y="238" font-size="${language === 'en' ? '39' : '44'}" font-weight="600" letter-spacing="-.8" fill="${ink}">${title[0]}</text>
        <text x="48" y="292" font-size="${language === 'en' ? '39' : '44'}" font-weight="600" letter-spacing="-.8" fill="${ink}">${title[1]}</text>
        <path d="M48 348H592" stroke="${line}"/>
        <circle cx="54" cy="389" r="6" fill="#b8ef73"/>
        <text x="73" y="398" font-size="27" fill="${ink}">Android + Windows</text>
        <text x="48" y="440" font-size="25" fill="${muted}">${language === 'en' ? 'Self-hosted · Open source' : '自部署 · 开源'}</text>
      </g>
      <image href="${logo}" x="48" y="44" width="112" height="112" clip-path="url(#mark)"/>
    </svg>` : `<svg xmlns="http://www.w3.org/2000/svg" width="1280" height="448" viewBox="0 0 1280 448">
      <defs>
        <radialGradient id="light" cx="100%" cy="0%" r="100%">
          <stop offset="0" stop-color="#b8ef73" stop-opacity="${dark ? '.16' : '.22'}"/>
          <stop offset="1" stop-color="#b8ef73" stop-opacity="0"/>
        </radialGradient>
        <clipPath id="mark"><rect x="958" y="92" width="244" height="244" rx="54"/></clipPath>
      </defs>
      <rect width="1280" height="448" rx="32" fill="${surface}"/>
      <rect width="1280" height="448" rx="32" fill="url(#light)"/>
      <g font-family="Segoe UI, Microsoft YaHei, sans-serif">
        <text x="64" y="122" font-size="76" font-weight="600" letter-spacing="-3" fill="${ink}">DropRun</text>
        <text x="66" y="191" font-size="${language === 'en' ? '39' : '44'}" font-weight="600" letter-spacing="-.8" fill="${ink}">${title[0]}</text>
        <text x="66" y="245" font-size="${language === 'en' ? '39' : '44'}" font-weight="600" letter-spacing="-.8" fill="${ink}">${title[1]}</text>
        <text x="67" y="298" font-size="17" letter-spacing="${language === 'en' ? '2' : '1'}" fill="${muted}">${subtitle}</text>
        <path d="M66 347H1214" stroke="${line}"/>
        <circle cx="74" cy="391" r="6" fill="#b8ef73"/>
        <text x="93" y="398" font-size="22" fill="${ink}">Android + Windows</text>
        <text x="1214" y="398" text-anchor="end" font-size="22" fill="${muted}">${language === 'en' ? 'Self-hosted · Open source' : '自部署 · 开源'}</text>
      </g>
      <image href="${logo}" x="958" y="92" width="244" height="244" clip-path="url(#mark)"/>
    </svg>`;
    const image = new Resvg(svg, { font: { loadSystemFonts: true } }).render();
    const name = `readme-cover-${language}-${theme}${compact ? '-compact' : ''}.png`;
    await writeFile(new URL(name, directory), image.asPng());
    console.log(`${name}: ${image.width}×${image.height}`);
  }
}
console.log(`Source mark: ${fileURLToPath(new URL('mark.png', directory))}`);
