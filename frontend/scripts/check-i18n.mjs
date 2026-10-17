import fs from 'node:fs'; import path from 'node:path';
const pt = JSON.parse(fs.readFileSync('public/i18n/pt-BR.json','utf8'));
const roots = new Set(Object.keys(pt).map(k=>k.split('.')[0]));
const files=[]; (function w(d){for(const f of fs.readdirSync(d,{withFileTypes:true})){const p=path.join(d,f.name); if(f.isDirectory()) w(p); else if(/\.(ts|html)$/.test(f.name)&&!/\.spec\.ts$/.test(f.name)) files.push(p);}})('src/app');
const missing=new Map(); const dyn=new Set();
for(const f of files){const s=fs.readFileSync(f,'utf8');
 for(const m of s.matchAll(/['"`]([a-zA-Z]+(?:\.[a-zA-Z0-9_-]+)+)(['"`]|\$\{)/g)){
  const k=m[1]; if(!roots.has(k.split('.')[0])) continue; if(/^(common|app)\.component/.test(k)) continue;
  if(m[2]==='${'){dyn.add(f+': '+k);continue;}
  if(!(k in pt)) {(missing.get(k)??missing.set(k,[]).get(k)).push(f);}
 }
 for(const m of s.matchAll(/['"]([a-zA-Z]+(?:\.[a-zA-Z0-9_]+)*\.)['"] \+/g)) dyn.add(f+': '+m[1]+'*');
}
if (missing.size) { console.error('Chaves i18n ausentes em pt-BR.json:'); for (const [k, v] of missing) console.error('  ', k, '<-', v[0]); process.exit(1); }
console.log('i18n OK (' + Object.keys(pt).length + ' chaves)');
