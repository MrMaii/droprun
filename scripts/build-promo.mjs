import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { Resvg } from '@resvg/resvg-js';

// Typeset campaign layouts. Screenshots are embedded unchanged, with fixture labels intact.
const image = async name => 'data:image/png;base64,' + (await readFile('landing/media/' + name + '.png')).toString('base64');
const home = await image('home'), share = await image('share'), task = await image('task');
const phone = (src,x,y,w) => `<rect x="${x-8}" y="${y-8}" width="${w+16}" height="${w*2+16}" rx="30" fill="white"/><image href="${src}" x="${x}" y="${y}" width="${w}" height="${w*2}"/>`;
const text = (x,y,size,value,fill='#18291f',weight=500) => `<text x="${x}" y="${y}" fill="${fill}" font-family="Segoe UI,Arial,sans-serif" font-size="${size}" font-weight="${weight}">${value}</text>`;
const layouts = [
  ['social-wide',1200,630, text(64,80,26,'droprun.',undefined,700)+text(64,164,14,'OPEN SOURCE / PUBLIC PREVIEW','#4d6c42',600)+text(64,256,64,'A little inspiration.')+text(64,334,64,'A real next step.')+text(64,413,23,'Share from Android into local Codex projects.')+text(64,527,18,'Your Windows PC. Your Cloudflare. Your control.')+text(64,578,15,'github.com/MrMaii/droprun · Actual app, demonstration data.')+phone(home,869,44,268)],
  ['social-portrait',1080,1350,text(64,88,36,'droprun.',undefined,700)+text(64,174,17,'ANDROID + WINDOWS / SELF-HOSTED','#4d6c42',600)+text(64,280,82,'Give inspiration')+text(64,378,82,'a next step.')+text(64,462,26,'Share. Pick a project. Inspect the result.')+phone(home,594,546,362)+text(64,682,38,'Your project.')+text(64,741,38,'Your Codex.')+text(64,800,38,'Your control.')+text(64,1066,21,'Open source · Public preview')+text(64,1120,18,'Actual app / demonstration data')+text(64,1235,21,'github.com/MrMaii/droprun')],
  ['social-square',1200,1200,text(64,85,34,'droprun.',undefined,700)+text(64,175,18,'OPEN SOURCE / ANDROID + WINDOWS','#4d6c42',600)+text(64,280,78,'A real next step.')+text(64,352,26,'Share to an existing project. Let local Codex work.')+phone(home,102,440,280)+phone(share,460,440,280)+phone(task,818,440,280)+text(64,1080,20,'Public preview · Your own Cloudflare · Actual app with demonstration data')+text(64,1138,22,'github.com/MrMaii/droprun')]
];
await mkdir('docs/launch/media',{recursive:true});
for(const [name,w,h,body] of layouts){
  const svg=`<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}"><rect width="100%" height="100%" fill="#f7f8f2"/><circle cx="${w}" cy="${h*.5}" r="${w*.36}" fill="#e9efde"/>${body}</svg>`;
  await writeFile(`docs/launch/media/${name}.svg`,svg);
  await writeFile(`docs/launch/media/${name}.png`,new Resvg(svg).render().asPng());
}
await writeFile('landing/media/social.png',await readFile('docs/launch/media/social-wide.png'));
console.log('Three campaign layouts exported, with unchanged actual app screenshots.');
