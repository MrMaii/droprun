import { execFileSync } from 'node:child_process';

// Review the Git index, not .local or a user's working files. Run after staging.
const files = execFileSync('git', ['ls-files', '-z'], { encoding: 'utf8' }).split('\0').filter(Boolean);
if (!files.length) throw new Error('No staged/tracked files to review. Stage the intended public source first.');
const failures = [];
const forbidden = /(^|\/)(\.local|node_modules|\.wrangler)(\/|$)|\.(keystore|jks|pfx|p12|secret)(\.|$)|(^|\/)\.env$|ORIGIN_(TRANSCRIPT|CONVERSATION)\.md$|handoffs\/archive\//;
const privateInstance = /droprun-mvp\.dengmaizi0802\.workers\.dev|C:[\\/]+Users[\\/]+User[\\/]|cloudfront\.net\/user_[A-Za-z\d]+\//i;
const credential = /-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|\bgh[pousr]_[A-Za-z0-9]{30,}|\bgithub_pat_[A-Za-z0-9_]{30,}|\bsk-(?:proj-)?[A-Za-z0-9_-]{35,}/;
for (const file of files) {
  if (forbidden.test(file)) { failures.push(`${file}: private/generated path`); continue; }
  if (/\.(png|jpg|jpeg|gif|mp4|webm|jar|ico|woff2|zip|apk|exe)$/i.test(file)) continue;
  const text = execFileSync('git', ['show', ':' + file], { encoding: 'utf8', maxBuffer: 16000000 });
  if (file !== 'scripts/check-public.mjs' && privateInstance.test(text)) failures.push(`${file}: private machine or instance reference`);
  if (credential.test(text)) failures.push(`${file}: possible credential`);
}
if (failures.length) { console.error(failures.join('\n')); process.exitCode = 1; }
else console.log(`Public source check passed (${files.length} files). Manual material/privacy review still required.`);
