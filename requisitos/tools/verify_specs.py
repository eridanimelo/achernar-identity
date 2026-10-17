#!/usr/bin/env python3
"""Ferramenta de documentação (não é código de produto): verifica IDs, cobertura e gera traceability.md.
Uso: python3 requisitos/tools/verify_specs.py   (a partir da raiz do repositório ou de qualquer pasta)."""
import re, os, sys, glob, json
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
SPECS = os.path.join(ROOT, 'specs'); SH = os.path.join(SPECS, '_shared'); DISC = os.path.join(ROOT, 'discovery')
rd = lambda p: open(p, encoding='utf-8').read()

FAM = r'RN-[A-Z]{3}|SEC-FT\d\d|CA-FT\d\d|CT-FT\d\d|API-FT\d\d|DATA-FT\d\d|INT-FT\d\d|RSK-FT\d\d|RNF|RF|SEC|SCR|TPL|DM|UC|DEV|RISK|P|D|A'
TOK = re.compile(r'(?<![A-Za-z0-9_-])(' + FAM + r')-(\d{1,3})(?![A-Za-z0-9])((?:\s*(?:,|…|/)\s*(?:(?:\1)-)?\d{1,3}(?![A-Za-z0-9]))*)')
SEQ = re.compile(r'(,|…|/)\s*(?:(?:' + FAM + r')-)?(\d{1,3})')

def expand(text):
    out = []
    for m in TOK.finditer(text):
        fam, first, rest = m.group(1), m.group(2), m.group(3)
        w = len(first)
        nums = [int(first)]; ops = []
        prev = int(first)
        for s in SEQ.finditer(rest):
            op, n = s.group(1), int(s.group(2))
            if op == '…':
                for k in range(prev + 1, n + 1): nums.append(k)
            else: nums.append(n)
            prev = n
        for n in nums: out.append(f'{fam}-{str(n).zfill(w)}')
    return out

# ---- definições ----
defs = set()
def add_rows(path, pat):
    for m in re.finditer(pat, rd(path), re.M): defs.add(m.group(1))
add_rows(os.path.join(SH, 'requirements.md'), r'^\|\s*(RF-\d+|RNF-\d+)\s*\|')
add_rows(os.path.join(SH, 'business-rules.md'), r'^\|\s*(RN-[A-Z]{3}-\d+)\s*\|')
add_rows(os.path.join(SH, 'parameters.md'), r'^\|\s*(P-\d+)\s*\|')
add_rows(os.path.join(SH, 'security-policy.md'), r'^\|\s*(SEC-\d+)\s*\|')
add_rows(os.path.join(SH, 'ui-map.md'), r'^\|\s*(SCR-\d+)\b')
add_rows(os.path.join(SH, 'emails.md'), r'^\|\s*(TPL-\d+)\s*\|')
add_rows(os.path.join(SH, 'data-model.md'), r'^\|\s*(DM-\d+)\s*\|')
add_rows(os.path.join(SH, 'decisions.md'), r'^##\s+(D-\d+)\b')
for f in glob.glob(os.path.join(DISC, '*.md')) + glob.glob(os.path.join(SH, '*.md')):
    t = rd(f)
    for m in re.finditer(r'(?<![A-Za-z0-9-])(UC-\d{3}|DEV-\d+|RISK-\d+|A-\d+|FND-\d+)(?![A-Za-z0-9])', t): defs.add(m.group(1))
defs_uc = set(i for i in defs if i.startswith('UC-'))

fts = sorted(d for d in os.listdir(SPECS) if d.startswith('FT-'))
local = {}  # por FT: família -> set definidos
CA, CT, caBlock, ctBlock = {}, {}, {}, {}
problems = []
DEFPAT = r'^(?:#{1,6}\s+|[-*]\s+\*\*)(%s-FT\d\d-\d\d)\b'
for d in fts:
    ft = d[:5]
    files = {n: os.path.join(SPECS, d, n) for n in ['business.md','api.md','database.md','frontend.md','security.md','tests.md','status.yaml','context-manifest.yaml']}
    for n, p in files.items():
        if not os.path.exists(p): problems.append(f'{ft}: arquivo ausente {n}')
    b = rd(files['business.md']); t = rd(files['tests.md'])
    # CAs
    pos = [(m.start(), m.group(1)) for m in re.finditer(DEFPAT % 'CA', b, re.M)]
    for i, (s, cid) in enumerate(pos):
        e = pos[i+1][0] if i+1 < len(pos) else len(b)
        blk = b[s:e]
        cut = re.search(r'^## ', blk[3:], re.M)
        if cut: blk = blk[:cut.start()+3]
        CA[cid] = ft; caBlock[cid] = blk
    pos = [(m.start(), m.group(1)) for m in re.finditer(DEFPAT % 'CT', t, re.M)]
    for i, (s, cid) in enumerate(pos):
        e = pos[i+1][0] if i+1 < len(pos) else len(t)
        blk = t[s:e]
        cut = re.search(r'^#{1,2} ', blk[3:], re.M)
        if cut: blk = blk[:cut.start()+3]
        CT[cid] = ft; ctBlock[cid] = blk
    # definições locais
    for fam, fn, pat in [('API','api.md',r'(API-FT\d\d-\d\d)'),('DATA','database.md',r'(DATA-FT\d\d-\d\d)'),('INT','database.md',r'(INT-FT\d\d-\d\d)'),('SEC','security.md',r'(SEC-FT\d\d-\d\d)'),('RSK','security.md',r'(RSK-FT\d\d-\d\d)')]:
        txt = rd(files[fn])
        for m in re.finditer(pat, txt): defs.add(m.group(1))
        for ln in txt.split('\n'):
            if re.match(r'^(###|[-*] \*\*|\|)\s*' + pat, ln): defs.update(i for i in expand(ln) if i.startswith(fam))
defs |= set(CA) | set(CT)

# ---- referências indefinidas ----
undefined = {}
for f in sorted(glob.glob(os.path.join(SPECS, 'FT-*', '*.md'))):
    t = rd(f)
    t_nocode = t
    for i in expand(t_nocode):
        if i not in defs: undefined.setdefault(i, set()).add(os.path.relpath(f, SPECS))
# faixas de intervalo externas (ex.: SEC-24 vs. D-xx) já cobertas; FT-nn e FND não são exigidos
for i, fs in sorted(undefined.items()):
    problems.append(f'ID não definido: {i} (em {", ".join(sorted(fs))})')

# ---- mapas ----
ca2ct = {c: set() for c in CA}; ct2ca = {}
for cid, blk in ctBlock.items():
    refs = [r for r in expand(blk) if r.startswith('CA-FT') and r in CA]
    ct2ca[cid] = sorted(set(refs))
    for r in refs: ca2ct[r].add(cid)
for c, s in ca2ct.items():
    if not s: problems.append(f'CA sem CT: {c}')
for c, r in ct2ca.items():
    if not r and not c.startswith('CT-X'): problems.append(f'CT sem CA: {c}')
ca2rf = {c: sorted(set(r for r in expand(blk) if r.startswith(('RF-','RNF-')))) for c, blk in caBlock.items()}
rf_defs = sorted(i for i in defs if re.fullmatch(r'RF-\d+', i)); rnf_defs = sorted(i for i in defs if re.fullmatch(r'RNF-\d+', i))
rf2ca = {r: [] for r in rf_defs}
for c, rs in ca2rf.items():
    for r in rs:
        if r in rf2ca: rf2ca[r].append(c)
for r, cs in rf2ca.items():
    if not cs: problems.append(f'RF sem CA: {r}')
alltests = ''.join(rd(f) for f in glob.glob(os.path.join(SPECS, 'FT-*', 'tests.md'))) + rd(os.path.join(SH, 'test-plan.md'))
allca = ''.join(caBlock.values())
rnf_cov = {}
for r in rnf_defs:
    rnf_cov[r] = (r in expand(alltests)) or (r in expand(allca))
    if not rnf_cov[r]: problems.append(f'RNF sem referência em CA/CT/test-plan: {r}')
# UC -> CA
uc2ca = {}
for d in fts:
    ft = d[:5]; b = rd(os.path.join(SPECS, d, 'business.md'))
    m = re.search(r'^## .*Rastreabilidade.*$', b, re.M)
    if not m: problems.append(f'{ft}: seção Rastreabilidade ausente em business.md'); continue
    sect = b[m.start():]
    nxt = re.search(r'\n## ', sect[3:])
    sect = sect[:nxt.start()+3] if nxt else sect
    prev = []
    for seg in re.split(r'·|\n', sect):
        ucs = [u for u in expand(seg.split('→')[0]) if u.startswith('UC-')]
        cas = [c for c in expand(seg) if c.startswith('CA-FT') and c in CA]
        if ucs: prev = ucs
        for u in prev: uc2ca.setdefault(u, set()).update(cas)
use_ucs = sorted(set(re.findall(r'^\|\s*(UC-\d{3})\s*\|', rd(os.path.join(DISC,'use-cases.md')), re.M)))
for u in use_ucs:
    if not uc2ca.get(u): problems.append(f'UC sem CA: {u}')
for d in fts:
    ft = d[:5]
    n = sum(1 for c in CA if CA[c] == ft)
    if n == 0: problems.append(f'{ft}: nenhum CA')

# ---- relatório ----
stats = {'fts': len(fts), 'ca': len(CA), 'ct': len(CT), 'uc': len(use_ucs), 'rf': len(rf_defs), 'rnf': len(rnf_defs), 'problems': len(problems)}
out = ['# Achernar Identity — Matriz de Rastreabilidade', '',
 'Gerada por `requisitos/tools/verify_specs.py` (ferramenta de documentação). Não editar manualmente; regenerar após alterar Specs.', '',
 f'Resumo: {stats["fts"]} Specs · {stats["uc"]} UCs · {stats["rf"]} RFs · {stats["rnf"]} RNFs · {stats["ca"]} critérios de aceite (CA) · {stats["ct"]} cenários de teste (CT) · {stats["problems"]} problemas.', '',
 '## 1. UC → Spec → CA → CT', '', '| UC | Spec | CAs | CTs |', '|---|---|---|---|']
def short(ids):
    ids = sorted(ids)
    return ', '.join(i.replace('CA-','').replace('CT-','') for i in ids) if ids else '—'
for u in use_ucs:
    cas = sorted(uc2ca.get(u, []))
    cts = sorted(set(t for c in cas for t in ca2ct.get(c, [])))
    ft = next((CA[c] for c in cas), '—')
    out.append(f'| {u} | {ft} | {short(cas)} | {short(cts)} |')
out += ['', '## 2. RF → CA → CT', '', '| RF | CAs | CTs |', '|---|---|---|']
for r in rf_defs:
    cas = sorted(rf2ca[r]); cts = sorted(set(t for c in cas for t in ca2ct.get(c, [])))
    note = '' if cas else ' (coberto por guia: ver completeness-check)'
    out.append(f'| {r} | {short(cas)}{note} | {short(cts)} |')
out += ['', '## 3. RNF → verificação', '', '| RNF | Referenciado em CA/CT/test-plan |', '|---|---|']
for r in rnf_defs: out.append(f'| {r} | {"sim" if rnf_cov[r] else "NÃO"} |')
out += ['', '## 4. CA → CT', '', '| CA | RFs/RNFs | CTs |', '|---|---|---|']
for c in sorted(CA): out.append(f'| {c} | {", ".join(ca2rf[c]) or "—"} | {short(ca2ct[c])} |')
out += ['', '## 5. Problemas encontrados', '']
out += [f'- {p}' for p in problems] or ['Nenhum.']
open(os.path.join(SH, 'traceability.md'), 'w', encoding='utf-8').write('\n'.join(out) + '\n')
print(json.dumps(stats)); print('\n'.join(problems[:80]))
sys.exit(1 if problems else 0)
