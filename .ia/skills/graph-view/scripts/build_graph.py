#!/usr/bin/env python3
"""
build_graph.py — extrai o grafo de referências reais do framework e gera
.cache/graph.html: uma visualização interativa (D3, arquivo único
auto-contido) de como AGENTS.md, agents/, skills/, corporate/, rules/,
gates/, config/ (motor, dentro de .ia/) se conectam com os nomes simbólicos
`specs`, `state`, `discovery`, `reviews` e `requirements` (documentos-fonte)
usados em todo o resto do framework.

Sem dependências externas (só stdlib — inclusive o parser de YAML abaixo é
mínimo de propósito, não é um parser YAML completo). Sem LLM. A extração
roda 100% local; o HTML gerado carrega D3 e a fonte via CDN/Google Fonts
quando aberto num navegador com internet (não é necessário para a extração
em si).

Uso:
    python3 build_graph.py              # gera/atualiza .cache/graph.html
    python3 build_graph.py --check      # só verifica se está desatualizado
                                          (exit 0 = em dia; exit 1 = ausente/desatualizado)

## Por que este script lê config/paths.yaml em vez de assumir nomes de pasta

`specs`, `state`, `discovery`, `reviews` e `requirements` (documentos-fonte
de requisitos) são NOMES SIMBÓLICOS em todo o resto do framework — nenhum
`AGENT.md`/`RULES.md`/`gate` escreve o nome físico da pasta. O mapeamento
simbólico -> físico vive só em `config/paths.yaml -> workspace.*` e
`-> requirements.root`. Isso é o mesmo princípio que já vale pra `backend`/
`frontend` desde o início. Copiar `.ia/` pra outro projeto e renomear a
pasta (`requisitos/` -> `estrutura/`, por exemplo) só exige editar
`config/paths.yaml` — nada mais no framework muda, e este script precisa
acompanhar isso ou vai quebrar silenciosamente no dia da renomeação.

Topologia assumida (ver AGENTS.md "Estrutura do repositório"), com os
nomes físicos de hoje como EXEMPLO — o que manda é sempre paths.yaml:

    <raiz-do-projeto>/
    ├── .ia/                 <- motor: AGENTS.md, agents/ skills/ corporate/
    │                           rules/ gates/ workflows/ docs/ config/
    ├── requisitos/           <- (nome físico de hoje) docs-fonte + specs/
    │                           state/ discovery/ reviews/ gerados
    └── backend/  frontend/  <- código do produto, FORA do escopo deste grafo

    Não existe AGENTS.md na raiz do projeto de propósito — sessão MUST ser
    aberta escopada em .ia/.

Resolução de referência (uma linha `` `algo.md` `` pode significar coisas
diferentes dependendo de quem a escreve):
    1. Path literal -> tenta, nesta ordem: relativo à raiz de .ia/ (a maioria
       das referências internas do motor não tem prefixo `.ia/`), tradução
       de nome simbólico (`specs/`, `state/`, `discovery/`, `reviews/`) pro
       path físico lido de paths.yaml, relativo à raiz do projeto sem
       tradução (fallback), e relativo à pasta de quem referencia (assets
       locais de uma Skill).
    2. Pacote inteiro (`corporate/x/**`) -> resolvido relativo a .ia/.
    3. Menção solta a script `.py`.
    4. Referência templated de Spec (`<ID>-<slug>/business.md`) -> fica
       pendente até existir uma Spec real cujo nome bata, procurada dentro
       do path físico de `specs` (lido de paths.yaml).
    5. Roteamento semântico do orquestrador: AGENTS.md -> os 12
       .ia/agents/<intenção>/AGENT.md, lidos da lista de intenções do
       próprio AGENTS.md.
"""
import json
import os
import re
import sys
from datetime import datetime, timezone

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
# .../.ia/skills/graph-view/scripts/build_graph.py
IA_ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, "..", "..", ".."))
PROJECT_ROOT = os.path.abspath(os.path.join(IA_ROOT, ".."))
TEMPLATE_PATH = os.path.join(SCRIPT_DIR, "..", "assets", "template.html")
PATHS_YAML = os.path.join(IA_ROOT, "config", "paths.yaml")

EXCLUDE_DIR_NAMES = {"cache", "node_modules", ".git", "__pycache__"}

DEFAULT_INTENTS = [
    "discovery", "requirements", "specification", "architecture", "backend",
    "frontend", "database", "security", "testing", "migration", "jira", "review",
]

# Fallback se config/paths.yaml não existir ou não tiver essas chaves ainda
# (ex.: motor recém-copiado pra outro projeto antes de configurar). Reflete
# a convenção de hoje, mas é só ponto de partida — paths.yaml sempre manda.
FALLBACK_WORKSPACE = {
    "specs": "requisitos/specs",
    "state": "requisitos/state",
    "discovery": "requisitos/discovery",
    "reviews": "requisitos/reviews",
}
FALLBACK_REQUIREMENTS_ROOT = "requisitos"

GROUP_META = {
    "root":               {"label": "Orquestrador (AGENTS.md)"},
    "agents":              {"label": "Agents (contrato de contexto)"},
    "skills":              {"label": "Skills do projeto (procedimento)"},
    "skill-asset":         {"label": "Assets / referências de Skill"},
    "corporate":           {"label": "Corporate (AGENT/RULES/SKILL)"},
    "corporate-template":  {"label": "Templates corporativos"},
    "rules":               {"label": "Rules (invariantes)"},
    "gates":               {"label": "Gates (critério de passagem)"},
    "config":              {"label": "Config (*.yaml)"},
    "workflows":           {"label": "Workflows (orquestração)"},
    "docs":                {"label": "Docs (manutenção)"},
    "spec-business":         {"label": "Spec · Business"},
    "spec-api":              {"label": "Spec · API"},
    "spec-database":         {"label": "Spec · Database"},
    "spec-tests":            {"label": "Spec · Tests"},
    "spec-other":            {"label": "Spec · Outros"},
    "state":                {"label": "state (workspace, símbolo de paths.yaml)"},
    "discovery-out":        {"label": "discovery (workspace, símbolo de paths.yaml)"},
    "reviews":              {"label": "reviews (workspace, símbolo de paths.yaml)"},
    "requirements-src":     {"label": "requirements (símbolo de paths.yaml)"},
    "meta":                {"label": "Meta (.gitignore etc.)"},
}

FILE_PATT = re.compile(r'`([a-zA-Z0-9_\-./]+\.(?:md|yaml|yml|py))(?:\s*->\s*[a-zA-Z.]+)?`')
PKG_PATT = re.compile(r'`([a-zA-Z0-9_\-./]+)/\*\*`')
PY_ANY_PATT = re.compile(r'([a-zA-Z0-9_\-./]+\.py)')
TEMPLATED_PATT = re.compile(r'`([a-zA-Z0-9_\-./<>]*<[^`]*\.(?:md|yaml|yml))`')


def parse_simple_yaml(path):
    """Parser mínimo pro formato usado em config/paths.yaml: chaves de topo
    sem indentação, sub-chaves indentadas 'chave: valor'. Não é YAML
    completo (sem listas, sem string multilinha) — de propósito, pra não
    precisar de dependência externa (PyYAML) nesses scripts."""
    data = {}
    current_top = None
    if not os.path.exists(path):
        return data
    with open(path, encoding="utf-8", errors="ignore") as fh:
        for raw_line in fh:
            line = raw_line.rstrip("\n")
            stripped = line.strip()
            if not stripped or stripped.startswith("#"):
                continue
            if not line[0].isspace():
                key = stripped.split(":", 1)[0].strip()
                current_top = key
                data[current_top] = {}
                continue
            if current_top is not None and ":" in stripped:
                k, v = stripped.split(":", 1)
                v = v.split("#", 1)[0].strip()  # remove comentário inline
                v = v.strip('"').strip("'")
                data[current_top][k.strip()] = v
    return data


def norm_rel(value):
    """Normaliza um valor de paths.yaml pra path relativo à raiz do
    projeto, aceitando tanto relativo ('./requisitos/specs') quanto
    absoluto ('/Users/x/projeto/requisitos/specs') — path absoluto sempre
    quebrava aqui antes: '/Users/...'.split('/')[0] é string vazia, e o
    filtro de WALK_TARGETS descartava tudo silenciosamente."""
    if not value:
        return value
    v = value.strip()
    if os.path.isabs(v):
        v = os.path.relpath(v, PROJECT_ROOT)
    elif v.startswith("./"):
        v = v[2:]
    elif v.startswith("../"):
        v = os.path.relpath(os.path.normpath(os.path.join(PROJECT_ROOT, v)), PROJECT_ROOT)
    return v.replace("\\", "/").rstrip("/")


_paths_config = parse_simple_yaml(PATHS_YAML)
_workspace_cfg = _paths_config.get("workspace", {})
_requirements_cfg = _paths_config.get("requirements", {})
_cache_cfg = _paths_config.get("cache", {})

WORKSPACE = {
    name: norm_rel(_workspace_cfg.get(name) or FALLBACK_WORKSPACE[name])
    for name in ("specs", "state", "discovery", "reviews")
}
REQUIREMENTS_ROOT = norm_rel(_requirements_cfg.get("root") or FALLBACK_REQUIREMENTS_ROOT)
CACHE_DIR = os.path.join(PROJECT_ROOT, norm_rel(_cache_cfg.get("root") or ".cache"))
OUTPUT_PATH = os.path.join(CACHE_DIR, "graph.html")

_GROUP_BY_SYMBOL = {"state": "state", "discovery": "discovery-out", "reviews": "reviews"}


def _top_segment(p):
    return p.split("/")[0] if p else None


_walk_roots = {REQUIREMENTS_ROOT} | {v for v in WORKSPACE.values() if v}
WALK_TARGETS = [".ia"] + sorted({_top_segment(p) for p in _walk_roots if _top_segment(p)})


def classify(rel):
    """rel é sempre relativo à raiz do PROJETO."""
    parts = rel.split("/")
    if parts[0] == ".ia":
        sub = parts[1:]
        if not sub:
            return "meta"
        top = sub[0]
        if top == "AGENTS.md":
            return "root"
        if top == "agents":
            return "agents"
        if top == "skills":
            return "skills" if sub[-1] == "SKILL.md" else "skill-asset"
        if top == "corporate":
            if sub[-1] in ("AGENT.md", "RULES.md", "SKILL.md"):
                return "corporate"
            if "templates" in sub:
                return "corporate-template"
            return "corporate"
        if top == "rules":
            return "rules"
        if top == "gates":
            return "gates"
        if top == "config":
            return "config"
        if top == "workflows":
            return "workflows"
        if top == "docs":
            return "docs"
        return "meta"

    # Specs: cada artefato Markdown recebe um grupo visual independente.
    # Isso permite ligar/desligar Business, API, Database e Tests separadamente.
    specs_base = WORKSPACE.get("specs")
    if specs_base and (rel == specs_base or rel.startswith(specs_base + "/")):
        filename = os.path.basename(rel).lower()
        if filename == "business.md":
            return "spec-business"
        if filename == "api.md":
            return "spec-api"
        if filename == "database.md":
            return "spec-database"
        if filename in ("tests.md", "test.md"):
            return "spec-tests"
        return "spec-other"

    # Demais áreas do workspace.
    for symbol, group in _GROUP_BY_SYMBOL.items():
        base = WORKSPACE[symbol]
        if base and (rel == base or rel.startswith(base + "/")):
            return group
    if REQUIREMENTS_ROOT and (rel == REQUIREMENTS_ROOT or rel.startswith(REQUIREMENTS_ROOT + "/")):
        return "requirements-src"
    return "meta"


def collect_real_files():
    """Coleta arquivos que devem aparecer como nós no grafo.

    YAML/YML continuam podendo ser lidos diretamente pelo script (por exemplo
    config/paths.yaml), mas não são exibidos como nós da visualização.
    """
    files = set()
    for target in WALK_TARGETS:
        full_target = os.path.join(PROJECT_ROOT, target)
        if not os.path.exists(full_target):
            continue
        if os.path.isfile(full_target):
            if not full_target.lower().endswith((".yaml", ".yml")):
                files.add(target)
            continue
        for r, dirs, fnames in os.walk(full_target):
            dirs[:] = [d for d in dirs if d not in EXCLUDE_DIR_NAMES]
            for f in fnames:
                if f.lower().endswith((".yaml", ".yml")):
                    continue
                full = os.path.join(r, f)
                rel = os.path.relpath(full, PROJECT_ROOT).replace("\\", "/")
                if rel.endswith(".gitkeep"):
                    continue
                files.add(rel)
    return files


def extract_intents():
    agents_md = os.path.join(IA_ROOT, "AGENTS.md")
    if not os.path.exists(agents_md):
        return DEFAULT_INTENTS
    with open(agents_md, encoding="utf-8", errors="ignore") as fh:
        content = fh.read()
    m = re.search(r"Classificar a solicitação.*?\n\n(.*?agents/<intenção>/AGENT\.md.*?)\n\n", content, re.S)
    block = m.group(1) if m else content
    found = re.findall(r"`([a-z]+)`", block)
    cleaned = [w for w in found if w in DEFAULT_INTENTS]
    return cleaned if len(cleaned) >= 6 else DEFAULT_INTENTS


DEP_TABLE_HEADING = "## Dependências entre casos de uso"
DEP_ROW_PATT = re.compile(r"^\|(.+)\|\s*$")


def parse_dependency_table(content):
    """Extrai pares (origem, depende_de) da tabela markdown sob o heading
    '## Dependências entre casos de uso' em discovery/dependencies.md.
    Ignora a linha de cabeçalho e a linha separadora (|---|---|...)."""
    idx = content.find(DEP_TABLE_HEADING)
    if idx == -1:
        return []
    tail = content[idx + len(DEP_TABLE_HEADING):]
    rows = []
    seen_header = False
    for line in tail.split("\n"):
        m = DEP_ROW_PATT.match(line)
        if not m:
            if seen_header:
                break  # tabela acabou
            continue
        cells = [c.strip() for c in m.group(1).split("|")]
        if not seen_header:
            seen_header = True
            continue  # linha de cabeçalho (Origem | Depende de | ...)
        if set(c.replace("-", "") for c in cells) <= {""}:
            continue  # linha separadora ---|---|---
        if len(cells) >= 2 and cells[0] and cells[1]:
            rows.append((cells[0], cells[1]))
    return rows


def find_spec_anchor(uc_id, real_files, specs_base):
    """Acha o arquivo-âncora da Spec.

    business.md é sempre preferido. Como YAML/YML não entra no grafo visual,
    o fallback considera somente arquivos Markdown da pasta da Spec.
    """
    if not specs_base:
        return None
    prefix = specs_base + "/"
    folders = set()
    for f in real_files:
        if f.startswith(prefix):
            folder = f[len(prefix):].split("/")[0]
            if folder == uc_id or folder.startswith(uc_id + "-"):
                folders.add(folder)
    for folder in sorted(folders):
        cand = f"{specs_base}/{folder}/business.md"
        if cand in real_files:
            return cand
    for folder in sorted(folders):
        spec_prefix = f"{specs_base}/{folder}/"
        for f in sorted(real_files):
            if f.startswith(spec_prefix) and f.lower().endswith(".md"):
                return f
    return None


def resolve(ref, referencing_rel, real_files):
    """Tenta resolver uma referência literal contra os arquivos reais,
    nesta ordem: relativa a .ia/, tradução de nome simbólico de workspace
    (specs/state/discovery/reviews) pro path físico de paths.yaml, relativa
    à raiz do projeto sem tradução, relativa à pasta de quem referencia."""
    own_dir = os.path.dirname(referencing_rel)
    candidates = []
    if referencing_rel.startswith(".ia/"):
        candidates.append(".ia/" + ref)

    first_seg = ref.split("/", 1)[0]
    if first_seg in WORKSPACE and WORKSPACE[first_seg]:
        rest = ref[len(first_seg):]  # inclui a barra inicial
        candidates.append(WORKSPACE[first_seg] + rest)

    candidates.append(ref)
    if own_dir:
        candidates.append(os.path.normpath(os.path.join(own_dir, ref)).replace("\\", "/"))
    for cand in candidates:
        if cand in real_files:
            return cand
    return None


def build_graph():
    real_files = collect_real_files()

    basename_counts = {}
    for rel in real_files:
        b = rel.split("/")[-1]
        basename_counts[b] = basename_counts.get(b, 0) + 1

        def label_for(rel):
            parts = rel.split("/")
            basename = parts[-1]

            specs_base = WORKSPACE.get("specs")

            # Arquivos dentro de uma Spec:
            # requisitos/specs/UC-010-manter-cargo/business.md
            # vira:
            # UC-010 · Manter Cargo
            if specs_base and rel.startswith(specs_base + "/"):
                spec_parts = rel[len(specs_base) + 1:].split("/")

                if len(spec_parts) >= 2:
                    spec_folder = spec_parts[0]

                    match = re.match(
                        r"^(UC-\d+)(?:-(.*))?$",
                        spec_folder,
                        re.IGNORECASE
                    )

                    if match:
                        uc_id = match.group(1).upper()
                        slug = match.group(2)

                        if slug:
                            spec_name = slug.replace("-", " ").replace("_", " ")
                            spec_name = spec_name.title()

                            return f"{uc_id} · {spec_name}"

                        return uc_id

                    return spec_folder

            # comportamento normal para arquivos fora de specs
            if basename_counts[basename] > 1 and len(parts) > 1:
                parent = parts[-2]
                return f"{parent.upper()}_{basename}"

            return basename

    nodes = {rel: {"id": rel, "label": label_for(rel), "group": classify(rel), "path": rel}
             for rel in real_files}

    edges = []
    seen = set()

    def add_edge(src, dst, semantic=False, kind="reference"):
        if src == dst:
            return
        key = (src, dst)
        if key in seen:
            return
        seen.add(key)
        edges.append({"from": src, "to": dst, "semantic": semantic, "kind": kind})

    pending_templates = []  # (src, basename) aguardando Spec real correspondente

    for rel in sorted(real_files):
        if not rel.endswith(".md"):
            continue
        full = os.path.join(PROJECT_ROOT, rel)
        with open(full, encoding="utf-8", errors="ignore") as fh:
            content = fh.read()

        for m in FILE_PATT.finditer(content):
            ref = m.group(1)
            if "<" in ref:
                continue
            target = resolve(ref, rel, real_files)
            if target:
                add_edge(rel, target)

        for m in PKG_PATT.finditer(content):
            prefix = m.group(1)
            first_seg = prefix.split("/")[0]
            if prefix.startswith(".ia/"):
                base = prefix
            elif first_seg in WALK_TARGETS:
                base = prefix
            else:
                base = ".ia/" + prefix
            for target in real_files:
                if target.startswith(base + "/"):
                    add_edge(rel, target)

        for m in PY_ANY_PATT.finditer(content):
            ref = m.group(1)
            target = resolve(ref, rel, real_files)
            if target:
                add_edge(rel, target)

        for m in TEMPLATED_PATT.finditer(content):
            ref = m.group(1)
            basename = ref.split("/")[-1]
            pending_templates.append((rel, basename))

    # resolver templates pendentes (specs/<algo>/<basename>) contra Specs
    # reais, procurando dentro do path físico de "specs" lido de paths.yaml
    specs_base = WORKSPACE["specs"]
    for src, basename in pending_templates:
        for target in real_files:
            if specs_base and target.startswith(specs_base + "/") and target.split("/")[-1] == basename:
                add_edge(src, target, semantic=True)

    # dependências entre casos de uso, lidas de discovery/dependencies.md
    # (tabela "Origem | Depende de | ..." preenchida pelo Discovery)
    discovery_base = WORKSPACE["discovery"]
    if discovery_base:
        dep_file = f"{discovery_base}/dependencies.md"
        if dep_file in real_files:
            full = os.path.join(PROJECT_ROOT, dep_file)
            with open(full, encoding="utf-8", errors="ignore") as fh:
                dep_content = fh.read()
            for origem, depende_de in parse_dependency_table(dep_content):
                src_anchor = find_spec_anchor(origem, real_files, specs_base)
                dst_anchor = find_spec_anchor(depende_de, real_files, specs_base)
                if src_anchor and dst_anchor:
                    add_edge(src_anchor, dst_anchor, kind="dependency")

    intents = extract_intents()
    for it in intents:
        target = f".ia/agents/{it}/AGENT.md"
        if target in real_files:
            add_edge(".ia/AGENTS.md", target, semantic=True)

    deg = {}
    for e in edges:
        deg[e["from"]] = deg.get(e["from"], 0) + 1
        deg[e["to"]] = deg.get(e["to"], 0) + 1
    for n in nodes.values():
        n["degree"] = deg.get(n["id"], 0)

    return {"nodes": list(nodes.values()), "edges": edges, "groupMeta": GROUP_META}


def is_stale():
    if not os.path.exists(OUTPUT_PATH):
        return True
    out_mtime = os.path.getmtime(OUTPUT_PATH)
    if os.path.exists(PATHS_YAML) and os.path.getmtime(PATHS_YAML) > out_mtime:
        return True
    for target in WALK_TARGETS:
        full_target = os.path.join(PROJECT_ROOT, target)
        if not os.path.exists(full_target):
            continue
        if os.path.isfile(full_target):
            if os.path.getmtime(full_target) > out_mtime:
                return True
            continue
        for r, dirs, fnames in os.walk(full_target):
            dirs[:] = [d for d in dirs if d not in EXCLUDE_DIR_NAMES]
            for f in fnames:
                if os.path.getmtime(os.path.join(r, f)) > out_mtime:
                    return True
    return False


def main():
    check_only = "--check" in sys.argv

    if check_only:
        sys.exit(1 if is_stale() else 0)

    graph = build_graph()
    data_json = json.dumps(graph, ensure_ascii=False)

    with open(TEMPLATE_PATH, encoding="utf-8") as fh:
        template = fh.read()

    generated_at = datetime.now(timezone.utc).astimezone().strftime("%Y-%m-%d %H:%M")
    html = template.replace("/*__GRAPH_DATA_JSON__*/", data_json)
    html = html.replace("/*__GENERATED_AT__*/", generated_at)

    os.makedirs(CACHE_DIR, exist_ok=True)
    with open(OUTPUT_PATH, "w", encoding="utf-8") as fh:
        fh.write(html)

    print(f"OK: {len(graph['nodes'])} arquivos, {len(graph['edges'])} referências")
    print(f"workspace lido de paths.yaml: {WORKSPACE} | requirements: {REQUIREMENTS_ROOT}")
    print(f"gerado em: {OUTPUT_PATH}")


if __name__ == "__main__":
    main()
