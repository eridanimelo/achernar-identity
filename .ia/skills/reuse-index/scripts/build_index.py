#!/usr/bin/env python3
"""
build_index.py — indexador local e determinístico de símbolos de código-fonte.

Sem dependências externas (só stdlib). Sem LLM, sem rede, sem custo.
Gera .cache/reuse-index.md: um índice raso (regex, não AST) de classes,
interfaces, services, components, DTOs e fragmentos encontrados no código,
para o Reuse Scan (rules/reuse/reuse.md) consultar antes de abrir arquivos.

A stack de cada raiz (backend/frontend/...) é lida de config/technology.yaml
e mapeada para extensões via STACK_KEYWORDS — não é fixa em Java/TS.

Uso:
    python3 build_index.py              # gera/atualiza o índice
    python3 build_index.py --check      # só verifica se está desatualizado (exit 1 se sim)
    python3 build_index.py --dir X --dir Y   # raiz extra, fora de config/paths.yaml

Como adicionar uma linguagem nova (ex.: Go, Ruby, Kotlin):
    1. adicionar um mapeamento em STACK_KEYWORDS: substring da stack -> extensão(ões);
    2. adicionar um bloco em LANG_RULES para a extensão: regex de "type" (classe/
       interface/equivalente) e uma lista "members" de (regex, índice do grupo do nome);
    3. se o formato não for "classe com métodos" (ex.: um template engine), tratar como
       Thymeleaf abaixo: um extrator dedicado, plugado em SPECIAL_INDEXERS.

Limitações (documentadas de propósito, não escondidas):
    - extração por regex, não parser real: pode perder assinaturas incomuns
      ou casar coisa errada em casos ambíguos;
    - não resolve relação entre símbolos (quem chama quem) — isso ainda
      exige grep/leitura manual;
    - cobre só as linguagens com regra abaixo; uma stack sem mapeamento é
      avisada no stderr e pulada, nunca falha silenciosamente;
    - é um atalho de localização, nunca a fonte de verdade sobre
      comportamento: sempre abrir o arquivo real antes de decidir reutilizar.
"""
from __future__ import annotations

import argparse
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

# .ia/skills/reuse-index/scripts/build_index.py -> parents[3] == .ia/
IA_ROOT = Path(__file__).resolve().parents[3]
PROJECT_ROOT = IA_ROOT.parent
PATHS_YAML = IA_ROOT / "config" / "paths.yaml"
TECHNOLOGY_YAML = IA_ROOT / "config" / "technology.yaml"

IGNORED_DIRS = {
    "node_modules", "dist", "build", "target", ".git", ".angular",
    "coverage", "out", "bin", "obj", "__pycache__", ".idea", ".vscode",
    ".next", ".venv", "venv",
}

# palavras que nunca são nome de método, mesmo casando a forma "palavra(...) {" ou "palavra(...);"
_STATEMENT_KEYWORDS = (
    "if", "for", "while", "switch", "catch", "return", "throw", "new",
    "else", "typeof", "instanceof", "function", "super", "using", "lock",
)
_KEYWORD_GUARD = r"(?!(?:" + "|".join(_STATEMENT_KEYWORDS) + r")\b)"

# --------------------------------------------------------------------------
# stack (config/technology.yaml) -> extensões a varrer
# chave = substring (lowercase) procurada no valor da stack; valores somam-se
# --------------------------------------------------------------------------
STACK_KEYWORDS = {
    "java": {".java"},
    "spring": {".java"},
    "kotlin": {".kt"},
    "python": {".py"},
    "django": {".py"},
    "fastapi": {".py"},
    "flask": {".py"},
    "dotnet": {".cs"},
    ".net": {".cs"},
    "csharp": {".cs"},
    "c#": {".cs"},
    "angular": {".ts"},
    "next": {".ts", ".tsx"},
    "react": {".ts", ".tsx"},
    "thymeleaf": {".html"},
}


def extensions_for_stack(label: str, stack_value: str) -> set[str]:
    lowered = stack_value.lower()
    found: set[str] = set()
    for keyword, exts in STACK_KEYWORDS.items():
        if keyword in lowered:
            found |= exts
    if not found:
        print(
            f"[aviso] stack '{stack_value}' ({label}) sem regra de indexação "
            f"conhecida — pulando. Adicione um mapeamento em STACK_KEYWORDS "
            f"e, se precisar, um bloco em LANG_RULES.",
            file=sys.stderr,
        )
    return found


# --------------------------------------------------------------------------
# extensão -> regras de extração: "type" (classe/interface/equivalente) e
# "members" (lista de (regex, índice do grupo com o nome do membro))
# --------------------------------------------------------------------------
LANG_RULES: dict[str, dict] = {
    ".java": {
        "type": re.compile(
            r"^\s*(?:public\s+|private\s+|protected\s+)?"
            r"(?:abstract\s+|final\s+|static\s+)*"
            r"(class|interface|enum|record)\s+(\w+)", re.MULTILINE),
        "members": [
            (re.compile(
                r"^\s*(?:public|default)\s+"
                r"[\w<>\[\],\s.?]+?\s+" + _KEYWORD_GUARD + r"(\w+)\s*\([^;{]*\)\s*\{",
                re.MULTILINE), 1),
            # assinatura de interface, sem modificador, termina em ';'
            (re.compile(
                r"^\s*" + _KEYWORD_GUARD +
                r"[\w<>\[\],\s]+?\s+" + _KEYWORD_GUARD + r"(\w+)\s*\([^;{)]*\)\s*;\s*$",
                re.MULTILINE), 1),
        ],
        "annotation": re.compile(r"^\s*@(\w+)", re.MULTILINE),
        "skip_suffix": ("Test.java", "Tests.java"),
    },
    ".cs": {
        "type": re.compile(
            r"^\s*(?:public\s+|internal\s+|private\s+|protected\s+)?"
            r"(?:abstract\s+|sealed\s+|static\s+|partial\s+)*"
            r"(class|interface|record|struct|enum)\s+(\w+)", re.MULTILINE),
        "members": [
            (re.compile(
                r"^\s*public\s+(?:static\s+|virtual\s+|override\s+|async\s+)*"
                r"[\w<>\[\],\s.?]+?\s+" + _KEYWORD_GUARD + r"(\w+)\s*\([^;{]*\)\s*\{",
                re.MULTILINE), 1),
            # assinatura de interface (C# tb não usa modificador em member de interface)
            (re.compile(
                r"^\s*" + _KEYWORD_GUARD +
                r"[\w<>\[\],\s]+?\s+" + _KEYWORD_GUARD + r"(\w+)\s*\([^;{)]*\)\s*;\s*$",
                re.MULTILINE), 1),
        ],
        # atributos C# usam colchetes: [ApiController], [HttpGet]
        "annotation": re.compile(r"^\s*\[(\w+)", re.MULTILINE),
        "skip_suffix": ("Tests.cs", "Test.cs"),
    },
    ".py": {
        "type": re.compile(r"^class\s+(\w+)\s*(?:\([^)]*\))?\s*:", re.MULTILINE),
        "members": [
            (re.compile(
                r"^\s*def\s+" + _KEYWORD_GUARD + r"(\w+)\s*\(", re.MULTILINE), 1),
        ],
        # decorators Python: @dataclass, @app.route(...), @staticmethod
        "annotation": re.compile(r"^\s*@([\w.]+)", re.MULTILINE),
        "skip_suffix": ("_test.py", "test_", ".test.py"),
        # em Python "type" captura só o grupo 1 (nome); não há kind explícito
        "type_kind": "class",
        # convenção pública: sem underscore inicial
        "public_prefix_filter": True,
    },
}
# Angular/TS e Next/React (TSX) compartilham a mesma forma "classe/interface +
# método com corpo/decl", mais componentes funcionais (function/const export)
_TS_TYPE = re.compile(
    r"^\s*export\s+(?:default\s+)?(?:abstract\s+)?"
    r"(class|interface|function)\s+(\w+)"
    r"|^\s*export\s+(?:default\s+)?const\s+(\w+)\s*(?::\s*[\w.<>]+)?\s*=\s*(?:\([^)]*\)|[\w,\s{}]+)\s*=>",
    re.MULTILINE)
_TS_MEMBERS = [
    (re.compile(
        r"^\s*(?:public\s+)?" + _KEYWORD_GUARD + r"(\w+)\s*\([^;{]*\)\s*"
        r"(?::\s*[\w<>\[\],\s|]+)?\s*\{",
        re.MULTILINE), 1),
    (re.compile(
        r"^\s*" + _KEYWORD_GUARD + r"(\w+)\s*\([^;{)]*\)\s*"
        r"(?::\s*[\w<>\[\],\s|]+)?\s*;\s*$",
        re.MULTILINE), 1),
]
_TS_RULES = {
    "type": _TS_TYPE,
    "members": _TS_MEMBERS,
    "annotation": re.compile(r"^\s*@(\w+)\(", re.MULTILINE),
    "skip_suffix": (".spec.ts", ".test.ts", ".spec.tsx", ".test.tsx"),
    "type_kind": None,  # resolvido em index_file por causa da alternância acima
}
LANG_RULES[".ts"] = _TS_RULES
LANG_RULES[".tsx"] = _TS_RULES


def parse_simple_yaml_keys(path: Path, keys: set[str]) -> dict[str, str]:
    """Parser mínimo para 'chave: valor' em qualquer nível de indentação —
    cobre o formato fixo de config/paths.yaml e config/technology.yaml."""
    found: dict[str, str] = {}
    if not path.exists():
        return found
    for line in path.read_text(encoding="utf-8").splitlines():
        m = re.match(r"^\s*(\w+):\s*(\S+)\s*$", line)
        if m and m.group(1) in keys and m.group(1) not in found:
            found[m.group(1)] = m.group(2)
    return found


def parse_paths_yaml() -> dict[str, Path]:
    roots: dict[str, Path] = {}
    if not PATHS_YAML.exists():
        return roots
    text = PATHS_YAML.read_text(encoding="utf-8")
    current_key = None
    for line in text.splitlines():
        top = re.match(r"^(\w+):\s*$", line)
        if top:
            current_key = top.group(1)
            continue
        sub = re.match(r"^\s+root:\s*(\S+)\s*$", line)
        if sub and current_key and current_key not in roots:
            # Transforma relativo ao PROJECT_ROOT em vez do PATH_YAML.parent
            raw_path = sub.group(1)
            if raw_path.startswith("./"):
                candidate = (PROJECT_ROOT / raw_path[2:]).resolve()
            elif raw_path.startswith("../"):
                candidate = (PROJECT_ROOT / raw_path).resolve()
            elif Path(raw_path).is_absolute():
                candidate = Path(raw_path).resolve()
            else:
                candidate = (PROJECT_ROOT / raw_path).resolve()
            roots[current_key] = candidate
    return roots


def resolve_extensions(labels: set[str]) -> dict[str, set[str]]:
    """Para cada rótulo de raiz (backend/frontend/...), lê a stack correspondente
    em config/technology.yaml e resolve para extensões via STACK_KEYWORDS."""
    tech = parse_simple_yaml_keys(TECHNOLOGY_YAML, labels)
    result: dict[str, set[str]] = {}
    for label in labels:
        stack_value = tech.get(label)
        if not stack_value:
            print(f"[aviso] sem stack definida para '{label}' em config/technology.yaml — pulando.",
                  file=sys.stderr)
            result[label] = set()
            continue
        result[label] = extensions_for_stack(label, stack_value)
    return result


def iter_source_files(root: Path, extensions: set[str]):
    if not extensions:
        return
    if not root.exists():
        print(f"[aviso] raiz não encontrada, pulando: {root}", file=sys.stderr)
        return
    for path in root.rglob("*"):
        if not path.is_file() or path.suffix not in extensions:
            continue
        if any(part in IGNORED_DIRS for part in path.parts):
            continue
        rules = LANG_RULES.get(path.suffix)
        if rules and str(path).endswith(rules.get("skip_suffix", ())):
            continue
        yield path


# --------------------------------------------------------------------------
# Thymeleaf (.html): não é "classe com método" — o que se reutiliza é um
# fragmento nomeado (th:fragment). Extrator dedicado, fora do fluxo genérico.
# --------------------------------------------------------------------------
_THYMELEAF_FRAGMENT = re.compile(
    r"""\bth:fragment\s*=\s*["']([\w-]+)""", re.MULTILINE)
_THYMELEAF_LAYOUT_FRAGMENT = re.compile(
    r"""\blayout:fragment\s*=\s*["']([\w-]+)""", re.MULTILINE)


def index_thymeleaf(text: str) -> list[str]:
    entries = []
    for m in _THYMELEAF_FRAGMENT.finditer(text):
        entries.append(f"- `th:fragment=\"{m.group(1)}\"`")
    for m in _THYMELEAF_LAYOUT_FRAGMENT.finditer(text):
        entries.append(f"- `layout:fragment=\"{m.group(1)}\"`")
    return entries


SPECIAL_INDEXERS = {
    ".html": index_thymeleaf,
}


def index_file(path: Path) -> list[str]:
    special = SPECIAL_INDEXERS.get(path.suffix)
    if special:
        try:
            text = path.read_text(encoding="utf-8", errors="ignore")
        except OSError:
            return []
        return special(text)

    rules = LANG_RULES.get(path.suffix)
    if not rules:
        return []
    try:
        text = path.read_text(encoding="utf-8", errors="ignore")
    except OSError:
        return []

    entries = []
    for match in rules["type"].finditer(text):
        groups = [g for g in match.groups() if g]
        if rules.get("type_kind"):
            kind, name = rules["type_kind"], groups[0]
        elif len(groups) >= 2:
            kind, name = groups[0], groups[1]
        else:
            # alternância TS: função/const componente casou só o nome (3º grupo)
            kind, name = "component", groups[-1]
        prefix = text[max(0, match.start() - 200):match.start()]
        annotations = rules["annotation"].findall(prefix.splitlines()[-1]) if prefix else []
        ann_str = " ".join(f"@{a}" for a in annotations[-2:])
        entries.append(f"- `{ann_str} {kind} {name}`".replace("  ", " ").strip())

    members = []
    for pattern, group_idx in rules.get("members", []):
        for m in pattern.finditer(text):
            name = m.group(group_idx)
            if not name or name in members:
                continue
            if rules.get("public_prefix_filter"):
                if not name.startswith("_"):
                    members.append(name)
            elif name[0].islower():
                members.append(name)
    members.sort()
    for member in members:
        entries.append(f"  - `{member}(...)`")

    return entries


def build_index(roots: dict[str, Path]) -> str:
    now = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    lines = [
        "<!-- gerado por .ia/skills/reuse-index/scripts/build_index.py — não editar à mão -->",
        f"# Índice de reuso (gerado em {now})",
        "",
        "Índice heurístico (regex, não AST). Use para localizar candidatos rapidamente;",
        "sempre abra o arquivo real antes de decidir reutilizar.",
        "",
    ]
    ext_by_label = resolve_extensions(set(roots.keys()))
    total_files = 0
    for label, root in roots.items():
        extensions = ext_by_label.get(label, set())
        files = sorted(iter_source_files(root, extensions))
        if not files:
            continue
        rel_root = root.relative_to(PROJECT_ROOT) if root.is_relative_to(PROJECT_ROOT) else root
        lines.append(f"## {label} (`{rel_root}`)")
        lines.append("")
        for path in files:
            entries = index_file(path)
            if not entries:
                continue
            total_files += 1
            rel = path.relative_to(root)
            lines.append(f"### {rel}")
            lines.extend(entries)
            lines.append("")
    lines.append(f"---\n\n{total_files} arquivo(s) indexado(s).")
    return "\n".join(lines)


def get_out_file() -> Path:
    config = parse_paths_yaml()
    cache_root = config.get("cache")
    if cache_root:
        return cache_root / "reuse-index.md"
    return PROJECT_ROOT / ".cache" / "reuse-index.md"

def is_stale(roots: dict[str, Path]) -> bool:
    out_file = get_out_file()
    if not out_file.exists():
        return True
    index_mtime = out_file.stat().st_mtime
    ext_by_label = resolve_extensions(set(roots.keys()))
    for label, root in roots.items():
        if label == "cache":
            continue
        for path in iter_source_files(root, ext_by_label.get(label, set())):
            if path.stat().st_mtime > index_mtime:
                return True
    return False


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--dir", action="append", metavar="PATH",
                         help="raiz extra a indexar (rótulo 'extra-N'); repetível")
    parser.add_argument("--check", action="store_true",
                         help="só verifica se o índice está desatualizado (exit 1 se sim, sem gerar nada)")
    args = parser.parse_args()

    roots = parse_paths_yaml()
    for i, extra in enumerate(args.dir or []):
        roots[f"extra-{i}"] = Path(extra).resolve()

    if not roots:
        print("Nenhuma raiz encontrada em config/paths.yaml e nenhum --dir informado.", file=sys.stderr)
        sys.exit(2)

    if args.check:
        stale = is_stale(roots)
        print("desatualizado" if stale else "em dia")
        sys.exit(1 if stale else 0)

    out_file = get_out_file()
    out_file.parent.mkdir(parents=True, exist_ok=True)
    
    # Remove 'cache' dos roots antes de indexar, já que não é código fonte
    if "cache" in roots:
        del roots["cache"]
        
    content = build_index(roots)
    out_file.write_text(content, encoding="utf-8")
    print(f"Índice gerado: {out_file.relative_to(PROJECT_ROOT)}")


if __name__ == "__main__":
    main()
