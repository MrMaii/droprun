import { Codex } from '../connector/codex.mjs';
const codex = await new Codex().start();
try {
  const projects = await codex.projects();
  console.log(JSON.stringify({ executable: codex.executable, projects }, null, 2));
} finally { codex.close(); }
